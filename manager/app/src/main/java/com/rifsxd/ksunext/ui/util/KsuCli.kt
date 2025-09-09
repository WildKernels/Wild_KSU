package com.rifsxd.ksunext.ui.util

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.os.Parcelable
import android.os.SystemClock
import android.provider.OpenableColumns
import android.system.Os
import android.util.Log
import com.rifsxd.ksunext.Natives
import com.rifsxd.ksunext.ksuApp
import com.topjohnwu.superuser.CallbackList
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.ShellUtils
import com.topjohnwu.superuser.io.SuFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * @author weishu
 * @date 2023/1/1.
 */
private const val TAG = "KsuCli"
private const val BUSYBOX = "/data/adb/ksu/bin/busybox"

private val ksuDaemonMagicPath by lazy {
    "${ksuApp.applicationInfo.nativeLibraryDir}${File.separator}libksud_magic.so"
}

private val ksuDaemonOverlayfsPath by lazy {
    "${ksuApp.applicationInfo.nativeLibraryDir}${File.separator}libksud_overlayfs.so"
}

fun readMountSystemFile(): Boolean {
    val filePath = "/data/adb/ksu/mount_system"
    val result = ShellUtils.fastCmd("cat $filePath").trim()
    return result == "OVERLAYFS"
}

// Get the path based on the user's choice
fun getKsuDaemonPath(): String {
    val useOverlayFs = readMountSystemFile()

    return if (useOverlayFs) {
        ksuDaemonOverlayfsPath
    } else {
        ksuDaemonMagicPath
    }
}

fun updateMountSystemFile(useOverlayFs: Boolean) {
    val filePath = "/data/adb/ksu/mount_system"
    if (useOverlayFs) {
        ShellUtils.fastCmd("echo -n OVERLAYFS > $filePath")
    } else {
        ShellUtils.fastCmd("echo -n MAGIC_MOUNT > $filePath")
    }
}

data class FlashResult(val code: Int, val err: String, val showReboot: Boolean) {
    constructor(result: Shell.Result, showReboot: Boolean) : this(result.code, result.err.joinToString("\n"), showReboot)
    constructor(result: Shell.Result) : this(result, result.isSuccess)
}

inline fun <T> withNewRootShell(
    globalMnt: Boolean = false,
    block: Shell.() -> T
): T {
    return createRootShell(globalMnt).use(block)
}

fun Uri.getFileName(context: Context): String? {
    var fileName: String? = null
    val contentResolver: ContentResolver = context.contentResolver
    val cursor: Cursor? = contentResolver.query(this, null, null, null, null)
    cursor?.use {
        if (it.moveToFirst()) {
            fileName = it.getString(it.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
        }
    }
    return fileName
}

fun createRootShellBuilder(globalMnt: Boolean = false): Shell.Builder {
    return Shell.Builder.create().run {
        val cmd = buildString {
            append("$ksuDaemonMagicPath debug su")
            if (globalMnt) append(" -g")
            append(" || ")
            append("su")
            if (globalMnt) append(" --mount-master")
            append(" || ")
            append("sh")
        }
        setCommands("sh", "-c", cmd)
    }
}

fun createRootShell(globalMnt: Boolean = false): Shell {
    return runCatching {
        createRootShellBuilder(globalMnt).build()
    }.getOrElse { e ->
        Log.w(TAG, "su failed: ", e)
        Shell.Builder.create().apply {
            if (globalMnt) setFlags(Shell.FLAG_MOUNT_MASTER)
        }.build()
    }
}

fun execKsud(args: String, newShell: Boolean = false): Boolean {
    return if (newShell) {
        withNewRootShell {
            ShellUtils.fastCmdResult(this, "${getKsuDaemonPath()} $args")
        }
    } else {
        ShellUtils.fastCmdResult("${getKsuDaemonPath()} $args")
    }
}

fun install() {
    val start = SystemClock.elapsedRealtime()
    val magiskboot = File(ksuApp.applicationInfo.nativeLibraryDir, "libmagiskboot.so").absolutePath
    val result = execKsud("install --magiskboot $magiskboot", true)
    Log.w(TAG, "install result: $result, cost: ${SystemClock.elapsedRealtime() - start}ms")
}

fun listModules(): String {
    val out =
        Shell.cmd("${getKsuDaemonPath()} module list").to(ArrayList(), null).exec().out
    return out.joinToString("\n").ifBlank { "[]" }
}

fun getModuleCount(): Int {
    return runCatching {
        JSONArray(listModules()).length()
    }.getOrDefault(0)
}

fun getSuperuserCount(): Int {
    return Natives.allowList.size
}

fun toggleModule(id: String, enable: Boolean): Boolean {
    val cmd = if (enable) {
        "module enable $id"
    } else {
        "module disable $id"
    }
    val result = execKsud(cmd, true)
    Log.i(TAG, "$cmd result: $result")
    return result
}

fun uninstallModule(id: String): Boolean {
    val cmd = "module uninstall $id"
    val result = execKsud(cmd, true)
    Log.i(TAG, "uninstall module $id result: $result")
    return result
}

fun restoreModule(id: String): Boolean {
    val cmd = "module restore $id"
    val result = execKsud(cmd, true)
    Log.i(TAG, "restore module $id result: $result")
    return result
}

private fun flashWithIO(
    cmd: String,
    onStdout: (String) -> Unit,
    onStderr: (String) -> Unit
): Shell.Result {

    val stdoutCallback: CallbackList<String?> = object : CallbackList<String?>() {
        override fun onAddElement(s: String?) {
            onStdout(s ?: "")
        }
    }

    val stderrCallback: CallbackList<String?> = object : CallbackList<String?>() {
        override fun onAddElement(s: String?) {
            onStderr(s ?: "")
        }
    }

    return withNewRootShell {
        newJob().add(cmd).to(stdoutCallback, stderrCallback).exec()
    }
}

fun flashModule(
    uri: Uri,
    onStdout: (String) -> Unit,
    onStderr: (String) -> Unit
): FlashResult {
    val resolver = ksuApp.contentResolver
    with(resolver.openInputStream(uri)) {
        val file = File(ksuApp.cacheDir, "module.zip")
        file.outputStream().use { output ->
            this?.copyTo(output)
        }
        val cmd = "module install ${file.absolutePath}"
        val result = flashWithIO("${getKsuDaemonPath()} $cmd", onStdout, onStderr)
        Log.i("KernelSU", "install module $uri result: $result")

        file.delete()

        return FlashResult(result)
    }
}

fun runModuleAction(
    moduleId: String, onStdout: (String) -> Unit, onStderr: (String) -> Unit
): Boolean {
    val shell = createRootShell(true)

    val stdoutCallback: CallbackList<String?> = object : CallbackList<String?>() {
        override fun onAddElement(s: String?) {
            onStdout(s ?: "")
        }
    }

    val stderrCallback: CallbackList<String?> = object : CallbackList<String?>() {
        override fun onAddElement(s: String?) {
            onStderr(s ?: "")
        }
    }

    val result = shell.newJob().add("${getKsuDaemonPath()} module action $moduleId")
        .to(stdoutCallback, stderrCallback).exec()
    Log.i("KernelSU", "Module runAction result: $result")

    return result.isSuccess
}

fun restoreBoot(
    onStdout: (String) -> Unit, onStderr: (String) -> Unit
): FlashResult {
    val magiskboot = File(ksuApp.applicationInfo.nativeLibraryDir, "libmagiskboot.so")
    val result = flashWithIO("${getKsuDaemonPath()} boot-restore -f --magiskboot $magiskboot", onStdout, onStderr)
    return FlashResult(result)
}

fun uninstallPermanently(
    onStdout: (String) -> Unit, onStderr: (String) -> Unit
): FlashResult {
    val magiskboot = File(ksuApp.applicationInfo.nativeLibraryDir, "libmagiskboot.so")
    val result = flashWithIO("${getKsuDaemonPath()} uninstall --magiskboot $magiskboot", onStdout, onStderr)
    return FlashResult(result)
}

suspend fun shrinkModules(): Boolean = withContext(Dispatchers.IO) {
    execKsud("module shrink", true)
}

@Parcelize
sealed class LkmSelection : Parcelable {
    data class LkmUri(val uri: Uri) : LkmSelection()
    data class KmiString(val value: String) : LkmSelection()
    data object KmiNone : LkmSelection()
}

fun installBoot(
    bootUri: Uri?,
    lkm: LkmSelection,
    ota: Boolean,
    onStdout: (String) -> Unit,
    onStderr: (String) -> Unit,
): FlashResult {
    val resolver = ksuApp.contentResolver

    val bootFile = bootUri?.let { uri ->
        with(resolver.openInputStream(uri)) {
            val bootFile = File(ksuApp.cacheDir, "boot.img")
            bootFile.outputStream().use { output ->
                this?.copyTo(output)
            }

            bootFile
        }
    }

    val magiskboot = File(ksuApp.applicationInfo.nativeLibraryDir, "libmagiskboot.so")
    var cmd = "boot-patch --magiskboot ${magiskboot.absolutePath}"

    cmd += if (bootFile == null) {
        // no boot.img, use -f to force install
        " -f"
    } else {
        " -b ${bootFile.absolutePath}"
    }

    if (ota) {
        cmd += " -u"
    }

    var lkmFile: File? = null
    when (lkm) {
        is LkmSelection.LkmUri -> {
            lkmFile = with(resolver.openInputStream(lkm.uri)) {
                val file = File(ksuApp.cacheDir, "kernelsu-tmp-lkm.ko")
                file.outputStream().use { output ->
                    this?.copyTo(output)
                }

                file
            }
            cmd += " -m ${lkmFile.absolutePath}"
        }

        is LkmSelection.KmiString -> {
            cmd += " --kmi ${lkm.value}"
        }

        LkmSelection.KmiNone -> {
            // do nothing
        }
    }

    // output dir
    val downloadsDir =
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    cmd += " -o $downloadsDir"

    val result = flashWithIO("${getKsuDaemonPath()} $cmd", onStdout, onStderr)
    Log.i("KernelSU", "install boot result: ${result.isSuccess}")

    bootFile?.delete()
    lkmFile?.delete()

    // if boot uri is empty, it is direct install, when success, we should show reboot button
    return FlashResult(result, bootUri == null && result.isSuccess)
}

fun reboot(reason: String = "") {
    if (reason == "recovery") {
        // KEYCODE_POWER = 26, hide incorrect "Factory data reset" message
        ShellUtils.fastCmdResult("/system/bin/reboot $reason")
    }
    ShellUtils.fastCmdResult("/system/bin/svc power reboot $reason || /system/bin/reboot $reason")
}

fun rootAvailable() = Shell.isAppGrantedRoot() == true

fun isAbDevice(): Boolean {
    return ShellUtils.fastCmd("getprop ro.build.ab_update").trim().toBoolean()
}

fun isInitBoot(): Boolean {
    return !Os.uname().release.contains("android12-")
}

suspend fun getCurrentKmi(): String = withContext(Dispatchers.IO) {
    val cmd = "boot-info current-kmi"
    ShellUtils.fastCmd("${getKsuDaemonPath()} $cmd")
}

suspend fun getSupportedKmis(): List<String> = withContext(Dispatchers.IO) {
    val cmd = "boot-info supported-kmi"
    val out = Shell.cmd("${getKsuDaemonPath()} $cmd").to(ArrayList(), null).exec().out
    out.filter { it.isNotBlank() }.map { it.trim() }
}

fun overlayFsAvailable(): Boolean {
    // check /proc/filesystems
    return ShellUtils.fastCmdResult("cat /proc/filesystems | grep overlay")
}

fun hasMagisk(): Boolean {
    val result = ShellUtils.fastCmdResult("which magisk")
    Log.i(TAG, "has magisk: $result")
    return result
}

fun isGlobalNamespaceEnabled(): Boolean {
    val result = ShellUtils.fastCmd("cat ${Natives.GLOBAL_NAMESPACE_FILE}")
    Log.i(TAG, "is global namespace enabled: $result")
    return result == "1"
}

fun setGlobalNamespaceEnabled(value: String) {
    Shell.cmd("echo $value > ${Natives.GLOBAL_NAMESPACE_FILE}")
        .submit { result ->
            Log.i(TAG, "setGlobalNamespaceEnabled result: ${result.isSuccess} [${result.out}]")
        }
}

fun isSepolicyValid(rules: String?): Boolean {
    if (rules == null) {
        return true
    }
    val result =
        Shell.cmd("${getKsuDaemonPath()} sepolicy check '$rules'").to(ArrayList(), null)
            .exec()
    return result.isSuccess
}

fun getSepolicy(pkg: String): String {
    val result =
        Shell.cmd("${getKsuDaemonPath()} profile get-sepolicy $pkg").to(ArrayList(), null)
            .exec()
    Log.i(TAG, "code: ${result.code}, out: ${result.out}, err: ${result.err}")
    return result.out.joinToString("\n")
}

fun setSepolicy(pkg: String, rules: String): Boolean {
    val result = Shell.cmd("${getKsuDaemonPath()} profile set-sepolicy $pkg '$rules'")
        .to(ArrayList(), null).exec()
    Log.i(TAG, "set sepolicy result: ${result.code}")
    return result.isSuccess
}

fun listAppProfileTemplates(): List<String> {
    return Shell.cmd("${getKsuDaemonPath()} profile list-templates").to(ArrayList(), null)
        .exec().out
}

fun getAppProfileTemplate(id: String): String {
    return Shell.cmd("${getKsuDaemonPath()} profile get-template '${id}'")
        .to(ArrayList(), null).exec().out.joinToString("\n")
}

fun getFileName(context: Context, uri: Uri): String {
    var name = "Unknown Module"
    if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
        val cursor: Cursor? = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                name = it.getString(it.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
            }
        }
    } else if (uri.scheme == "file") {
        name = uri.lastPathSegment ?: "Unknown Module"
    }
    return name
}

fun moduleBackupDir(): String? {
    val baseBackupDir = "/data/adb/ksu/backup/modules"

    if (!SuFile(baseBackupDir).mkdirs()) return null

    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())

    val newBackupDir = "$baseBackupDir/$timestamp"

    if (SuFile(newBackupDir).mkdirs()) return newBackupDir
    return null
}

fun moduleBackup(): Boolean {
    if (SuFile("/data/adb/modules").listFiles()?.isEmpty() ?: true) return false

    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())

    val tarName = "modules_backup_$timestamp.tar"
    val tarPath = "/data/local/tmp/$tarName"
    val internalBackupDir = "/data/adb/ksu/backup/modules"
    val internalBackupPath = "$internalBackupDir/$tarName"

    val tarCmd = "$BUSYBOX tar -cpf $tarPath -C /data/adb/modules $(ls /data/adb/modules)"
    val tarResult = ShellUtils.fastCmdResult(tarCmd)
    if (!tarResult) return false

    if (!SuFile(internalBackupDir).mkdirs()) return false

    val cpResult = ShellUtils.fastCmdResult("cp $tarPath $internalBackupPath")
    if (!cpResult) return false

    SuFile(tarPath).delete()

    return true
}

fun moduleRestore(): Boolean {
    val findTarCmd = "ls -t /data/adb/ksu/backup/modules/modules_backup_*.tar 2>/dev/null | head -n 1"
    val tarPath = ShellUtils.fastCmd(findTarCmd).trim()
    if (tarPath.isEmpty()) return false

    val extractCmd = "$BUSYBOX tar -xpf $tarPath -C /data/adb/modules_update"
    return ShellUtils.fastCmdResult(extractCmd)
}

fun allowlistBackup(): Boolean {
    if (!SuFile("/data/adb/ksu/.allowlist").exists()) return false

    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())

    val tarName = "allowlist_backup_$timestamp.tar"
    val tarPath = "/data/local/tmp/$tarName"
    val internalBackupDir = "/data/adb/ksu/backup/allowlist"
    val internalBackupPath = "$internalBackupDir/$tarName"

    val tarCmd = "$BUSYBOX tar -cpf $tarPath -C /data/adb/ksu .allowlist"
    val tarResult = ShellUtils.fastCmdResult(tarCmd)
    if (!tarResult) return false

    if (!SuFile(internalBackupDir).mkdirs()) return false

    val cpResult = ShellUtils.fastCmdResult("cp $tarPath $internalBackupPath")
    if (!cpResult) return false

    SuFile(tarPath).delete()

    return true
}

fun allowlistRestore(): Boolean {
    // Find the latest allowlist tar backup in /data/adb/ksu/backup/allowlist
    val findTarCmd = "ls -t /data/adb/ksu/backup/allowlist/allowlist_backup_*.tar 2>/dev/null | head -n 1"
    val tarPath = ShellUtils.fastCmd(findTarCmd).trim()
    if (tarPath.isEmpty()) return false

    // Extract the tar to /data/adb/ksu (restores .allowlist folder with permissions)
    val extractCmd = "$BUSYBOX tar -xpf $tarPath -C /data/adb/ksu"
    return ShellUtils.fastCmdResult(extractCmd)
}

fun themeBackup(customPath: String? = null): Boolean {
    try {
        val context = ksuApp
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val zipName = "theme_backup_$timestamp.zip"
        val zipPath = "/data/local/tmp/$zipName"
        val internalBackupDir = customPath ?: "/data/adb/ksu/backup/theme"
        val internalBackupPath = "$internalBackupDir/$zipName"
        val tempDir = "/data/local/tmp/theme_backup_$timestamp"
        
        // Create temp directory
        if (!SuFile(tempDir).mkdirs()) return false
        
        // Create JSON with theme settings (each value on a line as requested)
        val themeSettings = JSONObject().apply {
            put("theme_mode", prefs.getString("theme_mode", "system_default"))
            put("background_image_uri", prefs.getString("background_image_uri", ""))
            put("hide_bottom_bar", prefs.getBoolean("hide_bottom_bar", false))
            put("background_transparency", prefs.getFloat("background_transparency", 0.0f).toDouble())
            put("background_blur", prefs.getFloat("background_blur", 0.0f).toDouble())
            put("ui_transparency", prefs.getFloat("ui_transparency", 0.0f).toDouble())
            put("app_dpi", prefs.getInt("app_dpi", 160))
            put("original_system_dpi", prefs.getInt("original_system_dpi", 160))
            put("backup_timestamp", timestamp)
        }
        
        // Write JSON to temp file with each value on a line
        val jsonFile = SuFile("$tempDir/theme_settings.json")
        jsonFile.writeText(themeSettings.toString(2))
        
        // Copy background image if it exists
        val backgroundUri = prefs.getString("background_image_uri", null)
        if (!backgroundUri.isNullOrEmpty()) {
            try {
                val sourceFile = if (backgroundUri.startsWith("file://")) {
                    File(Uri.parse(backgroundUri).path!!)
                } else {
                    File(backgroundUri)
                }
                
                if (sourceFile.exists()) {
                    val destFile = SuFile("$tempDir/background_image.${sourceFile.extension}")
                    ShellUtils.fastCmdResult("cp \"${sourceFile.absolutePath}\" \"${destFile.absolutePath}\"")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to copy background image: ${e.message}")
            }
        }
        
        // Create zip file
        val zipCmd = "cd $tempDir && $BUSYBOX zip -r $zipName ."
        val zipResult = ShellUtils.fastCmdResult(zipCmd)
        if (!zipResult) {
            ShellUtils.fastCmdResult("rm -rf $tempDir")
            return false
        }
        
        // Move zip file to final location
        val moveResult = ShellUtils.fastCmdResult("mv $tempDir/$zipName $zipPath")
        if (!moveResult) {
            ShellUtils.fastCmdResult("rm -rf $tempDir")
            return false
        }
        
        // Create backup directory and copy zip file
        if (!SuFile(internalBackupDir).mkdirs()) {
            ShellUtils.fastCmdResult("rm -rf $tempDir")
            SuFile(zipPath).delete()
            return false
        }
        
        val cpResult = ShellUtils.fastCmdResult("cp $zipPath $internalBackupPath")
        
        // Clean up temp files
        ShellUtils.fastCmdResult("rm -rf $tempDir")
        
        return cpResult
    } catch (e: Exception) {
        Log.e(TAG, "Theme backup failed: ${e.message}")
        return false
    }
}

// Overloaded function for Uri-based backup
fun themeBackup(context: Context, uri: Uri): Boolean {
    try {
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val tempDir = "/data/local/tmp/theme_backup_$timestamp"
        
        // Create temp directory
        if (!SuFile(tempDir).mkdirs()) return false
        
        // Create JSON with theme settings
        val themeSettings = JSONObject().apply {
            put("theme_mode", prefs.getString("theme_mode", "system_default"))
            put("background_image_uri", prefs.getString("background_image_uri", ""))
            put("hide_bottom_bar", prefs.getBoolean("hide_bottom_bar", false))
            put("background_transparency", prefs.getFloat("background_transparency", 0.0f).toDouble())
            put("background_blur", prefs.getFloat("background_blur", 0.0f).toDouble())
            put("ui_transparency", prefs.getFloat("ui_transparency", 0.0f).toDouble())
            put("app_dpi", prefs.getInt("app_dpi", 160))
            put("original_system_dpi", prefs.getInt("original_system_dpi", 160))
            put("backup_timestamp", timestamp)
        }
        
        // Write JSON to temp file
        val jsonFile = SuFile("$tempDir/theme_settings.json")
        jsonFile.writeText(themeSettings.toString(2))
        
        // Copy background image if it exists
        val backgroundUri = prefs.getString("background_image_uri", null)
        if (!backgroundUri.isNullOrEmpty()) {
            try {
                val sourceFile = if (backgroundUri.startsWith("file://")) {
                    File(Uri.parse(backgroundUri).path!!)
                } else {
                    File(backgroundUri)
                }
                
                if (sourceFile.exists()) {
                    val destFile = SuFile("$tempDir/background_image.${sourceFile.extension}")
                    ShellUtils.fastCmdResult("cp \"${sourceFile.absolutePath}\" \"${destFile.absolutePath}\"")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to copy background image: ${e.message}")
            }
        }
        
        // Create zip file
        val zipName = "theme_backup_$timestamp.zip"
        val zipCmd = "cd $tempDir && $BUSYBOX zip -r $zipName ."
        val zipResult = ShellUtils.fastCmdResult(zipCmd)
        if (!zipResult) {
            ShellUtils.fastCmdResult("rm -rf $tempDir")
            return false
        }
        
        // Write zip file to selected Uri using ContentResolver
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            SuFile("$tempDir/$zipName").inputStream().use { inputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        
        // Clean up temp files
        ShellUtils.fastCmdResult("rm -rf $tempDir")
        SuFile(zipPath).delete()
        
        return true
    } catch (e: Exception) {
        Log.e(TAG, "Theme backup failed: ${e.message}")
        return false
    }
}

// Overloaded function for Uri-based restore
fun themeRestore(context: Context, uri: Uri): Boolean {
    try {
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val tempDir = "/data/local/tmp/theme_restore_$timestamp"
        val zipPath = "/data/local/tmp/theme_restore_$timestamp.zip"
        
        // Copy zip file from Uri to temp location
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            SuFile(zipPath).outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        } ?: return false
        
        // Create temp directory and extract zip
        if (!SuFile(tempDir).mkdirs()) {
            SuFile(zipPath).delete()
            return false
        }
        
        val extractCmd = "$BUSYBOX unzip -o $zipPath -d $tempDir"
        if (!ShellUtils.fastCmdResult(extractCmd)) {
            ShellUtils.fastCmdResult("rm -rf $tempDir")
            SuFile(zipPath).delete()
            return false
        }
        
        // Read and parse JSON settings
        val jsonFile = SuFile("$tempDir/theme_settings.json")
        if (!jsonFile.exists()) {
            ShellUtils.fastCmdResult("rm -rf $tempDir")
            SuFile(zipPath).delete()
            return false
        }
        
        val jsonContent = jsonFile.readText()
        val themeSettings = JSONObject(jsonContent)
        
        // Restore theme settings to SharedPreferences
        val editor = prefs.edit()
        
        // Restore all theme settings
        themeSettings.optString("theme_mode", "system_default").let {
            editor.putString("theme_mode", it)
        }
        
        editor.putBoolean("hide_bottom_bar", themeSettings.optBoolean("hide_bottom_bar", false))
        editor.putFloat("background_transparency", themeSettings.optDouble("background_transparency", 0.0).toFloat())
        editor.putFloat("background_blur", themeSettings.optDouble("background_blur", 0.0).toFloat())
        editor.putFloat("ui_transparency", themeSettings.optDouble("ui_transparency", 0.0).toFloat())
        editor.putInt("app_dpi", themeSettings.optInt("app_dpi", 160))
        editor.putInt("original_system_dpi", themeSettings.optInt("original_system_dpi", 160))
        
        // Handle background image restoration
        val originalBackgroundUri = themeSettings.optString("background_image_uri", "")
        if (originalBackgroundUri.isNotEmpty()) {
            // Look for background image files in the backup
            val backgroundFiles = SuFile(tempDir).listFiles()?.filter {
                it.name.startsWith("background_image.")
            }
            
            if (!backgroundFiles.isNullOrEmpty()) {
                val backgroundFile = backgroundFiles.first()
                val internalDir = File(context.filesDir, "backgrounds")
                if (!internalDir.exists()) internalDir.mkdirs()
                
                val restoredFile = File(internalDir, "background.${backgroundFile.extension}")
                val copyCmd = "cp \"${backgroundFile.absolutePath}\" \"${restoredFile.absolutePath}\""
                
                if (ShellUtils.fastCmdResult(copyCmd)) {
                    editor.putString("background_image_uri", restoredFile.absolutePath)
                }
            } else {
                // Clear background image if file not found in backup
                editor.remove("background_image_uri")
            }
        } else {
            editor.remove("background_image_uri")
        }
        
        editor.commit()
        
        // Clean up temp files
        ShellUtils.fastCmdResult("rm -rf $tempDir")
        SuFile(zipPath).delete()
        
        return true
    } catch (e: Exception) {
        Log.e(TAG, "Theme restore failed: ${e.message}")
        return false
    }
}

fun themeRestore(customPath: String? = null): Boolean {
    try {
        val context = ksuApp
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        
        val backupDir = customPath ?: "/data/adb/ksu/backup/theme"
        
        // Find the latest theme backup zip file
        val findZipCmd = "ls -t $backupDir/theme_backup_*.zip 2>/dev/null | head -n 1"
        val zipPath = ShellUtils.fastCmd(findZipCmd).trim()
        if (zipPath.isEmpty()) return false
        
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val tempDir = "/data/local/tmp/theme_restore_$timestamp"
        
        // Create temp directory and extract zip
        if (!SuFile(tempDir).mkdirs()) return false
        
        val extractCmd = "$BUSYBOX unzip -o $zipPath -d $tempDir"
        if (!ShellUtils.fastCmdResult(extractCmd)) {
            ShellUtils.fastCmdResult("rm -rf $tempDir")
            return false
        }
        
        // Read and parse JSON settings
        val jsonFile = SuFile("$tempDir/theme_settings.json")
        if (!jsonFile.exists()) {
            ShellUtils.fastCmdResult("rm -rf $tempDir")
            return false
        }
        
        val jsonContent = jsonFile.readText()
        val themeSettings = JSONObject(jsonContent)
        
        // Restore theme settings to SharedPreferences
        val editor = prefs.edit()
        
        if (themeSettings.has("theme_mode")) {
            editor.putString("theme_mode", themeSettings.getString("theme_mode"))
        }
        if (themeSettings.has("hide_bottom_bar")) {
            editor.putBoolean("hide_bottom_bar", themeSettings.getBoolean("hide_bottom_bar"))
        }
        if (themeSettings.has("background_transparency")) {
            editor.putFloat("background_transparency", themeSettings.getDouble("background_transparency").toFloat())
        }
        if (themeSettings.has("background_blur")) {
            editor.putFloat("background_blur", themeSettings.getDouble("background_blur").toFloat())
        }
        if (themeSettings.has("ui_transparency")) {
            editor.putFloat("ui_transparency", themeSettings.getDouble("ui_transparency").toFloat())
        }
        if (themeSettings.has("app_dpi")) {
            editor.putInt("app_dpi", themeSettings.getInt("app_dpi"))
        }
        if (themeSettings.has("original_system_dpi")) {
            editor.putInt("original_system_dpi", themeSettings.getInt("original_system_dpi"))
        }
        
        // Handle background image restoration
        if (themeSettings.has("background_image_uri") && !themeSettings.getString("background_image_uri").isEmpty()) {
            // Look for background image file in extracted content
            val backgroundFiles = SuFile(tempDir).listFiles()?.filter { 
                it.name.startsWith("background_image.") 
            }
            
            if (!backgroundFiles.isNullOrEmpty()) {
                val backgroundFile = backgroundFiles.first()
                val internalDir = File(context.filesDir, "backgrounds")
                if (!internalDir.exists()) internalDir.mkdirs()
                
                val restoredFile = File(internalDir, "background.${backgroundFile.extension}")
                val copyCmd = "cp \"${backgroundFile.absolutePath}\" \"${restoredFile.absolutePath}\""
                
                if (ShellUtils.fastCmdResult(copyCmd)) {
                    editor.putString("background_image_uri", restoredFile.absolutePath)
                }
            } else {
                // Clear background image if file not found in backup
                editor.remove("background_image_uri")
            }
        } else {
            editor.remove("background_image_uri")
        }
        
        editor.commit()
        
        // Clean up temp directory
        ShellUtils.fastCmdResult("rm -rf $tempDir")
        
        return true
    } catch (e: Exception) {
        Log.e(TAG, "Theme restore failed: ${e.message}")
        return false
    }
}

fun moduleMigration(): Boolean {
    val command = "cp -rp /data/adb/modules/* /data/adb/modules_update"
    return ShellUtils.fastCmdResult(command)
}

private val suSFSDaemonPath by lazy {
    "${ksuApp.applicationInfo.nativeLibraryDir}${File.separator}libsusfsd.so"
}

fun getSuSFS(): String {
    return ShellUtils.fastCmd("$suSFSDaemonPath support")
}

fun getSuSFSVersion(): String {
    return ShellUtils.fastCmd("$suSFSDaemonPath version")
}

fun getSuSFSVariant(): String {
    return ShellUtils.fastCmd("$suSFSDaemonPath variant")
}

fun getSuSFSFeatures(): String {
    return ShellUtils.fastCmd("$suSFSDaemonPath features")
}

fun hasSuSFs_SUS_SU(): String {
    return ShellUtils.fastCmd("$suSFSDaemonPath sus_su support")
}

fun susfsSUS_SU_0(): String {
    return ShellUtils.fastCmd("$suSFSDaemonPath sus_su 0")
}

fun susfsSUS_SU_2(): String {
    return ShellUtils.fastCmd("$suSFSDaemonPath sus_su 2")
}

fun susfsSUS_SU_Mode(): String {
    return ShellUtils.fastCmd("$suSFSDaemonPath sus_su mode")
}

fun currentMountSystem(): String {
    val result = ShellUtils.fastCmd("${getKsuDaemonPath()} module mount").trim()
    return result.substringAfter(":").substringAfter(" ").trim()
}

fun getModuleSize(dir: File): Long {
    val result = ShellUtils.fastCmd("$BUSYBOX du -sb '${dir.absolutePath}' | awk '{print $1}'").trim()
    return result.toLongOrNull() ?: 0L
}

fun isSuCompatDisabled(): Boolean {
    return Natives.version >= Natives.MINIMAL_SUPPORTED_SU_COMPAT && !Natives.isSuEnabled()
}

fun zygiskRequired(dir: File): Boolean {
    return (SuFile(dir, "zygisk").listFiles()?.size ?: 0) > 0
}

fun setAppProfileTemplate(id: String, template: String): Boolean {
    val escapedTemplate = template.replace("\"", "\\\"")
    val cmd = """${getKsuDaemonPath()} profile set-template "$id" "$escapedTemplate'""""
    return Shell.cmd(cmd)
        .to(ArrayList(), null).exec().isSuccess
}

fun deleteAppProfileTemplate(id: String): Boolean {
    return Shell.cmd("${getKsuDaemonPath()} profile delete-template '${id}'")
        .to(ArrayList(), null).exec().isSuccess
}

fun forceStopApp(packageName: String) {
    val result = Shell.cmd("am force-stop $packageName").exec()
    Log.i(TAG, "force stop $packageName result: $result")
}

fun launchApp(packageName: String) {
    val result =
        Shell.cmd("cmd package resolve-activity --brief $packageName | tail -n 1 | xargs cmd activity start-activity -n")
            .exec()
    Log.i(TAG, "launch $packageName result: $result")
}

fun restartApp(packageName: String) {
    forceStopApp(packageName)
    launchApp(packageName)
}
