package com.rifsxd.ksunext.ui.util

import android.app.Activity
import android.content.Intent
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
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * @author weishu
 * @date 2023/1/1.
 */
private const val TAG = "KsuCli"
private const val BUSYBOX = "/data/adb/ksu/bin/busybox"

private fun getKsuDaemonPath(): String {
    return ksuApp.applicationInfo.nativeLibraryDir + File.separator + "libksud.so"
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
            append("${getKsuDaemonPath()} debug su")
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

suspend fun getFeatureStatus(feature: String): String = withContext(Dispatchers.IO) {
    val shell = createRootShell(true)
    
    val out = shell.newJob()
        .add("${getKsuDaemonPath()} feature check $feature").to(ArrayList<String>(), null).exec().out
    out.firstOrNull()?.trim().orEmpty()
}

suspend fun getFeaturePersistValue(feature: String): Long? = withContext(Dispatchers.IO) {
    val shell = createRootShell(true)
    
    val out = shell.newJob()
        .add("${getKsuDaemonPath()} feature get --config $feature").to(ArrayList<String>(), null).exec().out
    val valueLine = out.firstOrNull { it.trim().startsWith("Value:") } ?: return@withContext null
    valueLine.substringAfter("Value:").trim().toLongOrNull()
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

private fun processUiPrintLine(s: String?): Pair<Int, String?> {
    if (s == null) {
        return Pair(1,null)
    }

    val check1 = s.startsWith("ui_print")
    val trimmed = s.trim()
    val check2 = trimmed.startsWith("ui_print")
    if (!check1 && check2) return Pair(1,null)

    return if(check1) {
        Pair(1,trimmed.drop(8).dropWhile { it.isWhitespace() })
    }
    else {
        Pair(2, trimmed)
    }
}

private fun flashWithIO_ak3(
    cmd: String,
    onStdout: (String) -> Unit,
    onStderr: (String) -> Unit
): Shell.Result {

    val stdoutCallback: CallbackList<String?> = object : CallbackList<String?>() {
        override fun onAddElement(s: String?) {
            val (type, text) = processUiPrintLine(s)
            if(type == 1) {
                text?.let(onStdout)
            } else {
                text?.let(onStderr)
            }
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
    allowShell: Boolean,
    enableAdbd: Boolean,
    noInstall: Boolean,
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

    if (allowShell) {
        cmd += " --allow-shell"
    }
    if (enableAdbd) {
        cmd += " --enable-adbd"
    }
    if (noInstall) {
        cmd += " --no-install"
    }

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

fun flashAnyKernelZip(
    uri: Uri,
    onStdout: (String) -> Unit,
    onStderr: (String) -> Unit
): FlashResult {
    val resolver = ksuApp.contentResolver

    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val tmpFile = File(ksuApp.cacheDir, "anykernel_${timestamp}.zip")
    resolver.openInputStream(uri).use { input ->
        tmpFile.outputStream().use { out ->
            input?.copyTo(out)
        }
    }

    val destZip = tmpFile.absolutePath
    val destZipName = File(destZip).name
    val destDirFile = File(ksuApp.cacheDir, "anykernel3_${timestamp}")
    val destDir = destDirFile.absolutePath

    val cmd = """
                mkdir -p '$destDir' && \
                $BUSYBOX unzip -p -o '$destZip' "META-INF/com/google/android/update-binary" > '$destDir/update-binary' 2>/dev/null && \
                cp '$destZip' '$destDir/$destZipName' 2>/dev/null || true && \
                $BUSYBOX chmod 755 '$destDir/update-binary' && \
                $BUSYBOX chown root:root '$destDir/update-binary' && \
                (cd '$destDir' && \
                    if [ -f './update-binary' ]; then \
                        AKHOME='$destDir/tmp' $BUSYBOX ash '$destDir/update-binary' 3 1 '$destDir/$destZipName'; \
                    else \
                        echo 'No installer script found' >&2; exit 1; \
                    fi)
            """.trimIndent().replace(Regex("\\s+\\\\\\s*"), " ")

    val result = flashWithIO_ak3(cmd, onStdout, onStderr)
    try {
        return FlashResult(result, result.isSuccess)
    } finally {
        try {
            runCatching {
                createRootShell(true).use { sh ->
                    sh.newJob().add("rm -rf '$destDir' '$destZip'").exec()
                }
            }
        } catch (_: Throwable) {
        }
    }
}

fun magiskBootRepack(
    zipUri: Uri,
    targetBootUri: Uri?,
    onStdout: (String) -> Unit,
    onStderr: (String) -> Unit
): FlashResult {
    return magiskBootRepack(zipUri, targetBootUri, false, onStdout, onStderr)
}

fun magiskBootRepack(
    zipUri: Uri,
    targetBootUri: Uri?,
    enableKpn: Boolean,
    onStdout: (String) -> Unit,
    onStderr: (String) -> Unit
): FlashResult {
    val resolver = ksuApp.contentResolver
    val context = ksuApp.applicationContext
    
    // Staging directory in app cache (accessible by Kotlin)
    val stagingDir = File(context.cacheDir, "magiskboot_staging")
    stagingDir.mkdirs()
    stagingDir.deleteRecursively() // Ensure clean start
    stagingDir.mkdirs()

    // Working directory in /data/local/tmp (accessible by Root)
    val workDirInfo = "/data/local/tmp/magiskboot_repack"
    val magiskboot = File(ksuApp.applicationInfo.nativeLibraryDir, "libmagiskboot.so").absolutePath

    try {
        onStdout("Preparing workspace...")
        
        // Clean and create root workdir
        val setupCmd = "rm -rf '$workDirInfo' && mkdir -p '$workDirInfo'"
        val setupResult = flashWithIO(setupCmd, { }, { })
        if (!setupResult.isSuccess) {
            return FlashResult(setupResult.code, "Failed to create temporary workspace: ${setupResult.err}", false)
        }

        // 1. Save Zip to Staging
        val stagingZip = File(stagingDir, "kernel.zip")
        resolver.openInputStream(zipUri)?.use { input ->
            stagingZip.outputStream().use { output -> input.copyTo(output) }
        } ?: return FlashResult(1, "Failed to read zip file", false)

        // 2. Save Target Boot to Staging (if exists)
        val stagingBoot = File(stagingDir, "boot.img")
        if (targetBootUri != null) {
            resolver.openInputStream(targetBootUri)?.use { input ->
                stagingBoot.outputStream().use { output -> input.copyTo(output) }
            } ?: return FlashResult(1, "Failed to read target boot image", false)
        }

        // 3. Move Staging files to Root WorkDir
        onStdout("Moving files to temporary workspace...")
        val moveCmd = StringBuilder()
        moveCmd.append("cp '${stagingZip.absolutePath}' '$workDirInfo/kernel.zip'")
        moveCmd.append(" && chmod 644 '$workDirInfo/kernel.zip'")
        
        if (targetBootUri != null) {
            moveCmd.append(" && cp '${stagingBoot.absolutePath}' '$workDirInfo/boot.img'")
            moveCmd.append(" && chmod 644 '$workDirInfo/boot.img'")
        }
        
        val moveResult = flashWithIO(moveCmd.toString(), { }, { })
        if (!moveResult.isSuccess) {
            return FlashResult(moveResult.code, "Failed to move files to temp dir", false)
        }

        // 4. Extract Zip
        val unzipCmd = "$BUSYBOX unzip -o '$workDirInfo/kernel.zip' -d '$workDirInfo'"
        val unzipResult = flashWithIO(unzipCmd, { /* ignore */ }, { /* ignore */ })
        if (!unzipResult.isSuccess) {
            return FlashResult(unzipResult.code, "Failed to unzip archive", false)
        }

        // 5. Find Kernel Image (via Shell)
        val kernelNames = listOf("Image", "Image.gz", "Image.lz4", "zImage", "kernel", "Image.gz-dtb", "zImage-dtb")
        val findConditions = kernelNames.joinToString(" -o ") { "-name \"$it\"" }
        val findCmd = "find '$workDirInfo' -type f \\( $findConditions \\) | head -n 1"
        
        var kernelPath = ""
        withNewRootShell {
            val out = newJob().add(findCmd).to(ArrayList(), null).exec().out
            kernelPath = out.firstOrNull()?.trim() ?: ""
        }

        if (kernelPath.isEmpty()) {
            return FlashResult(1, "No kernel image found in zip", false)
        }
        val kernelName = File(kernelPath).name
        onStdout("Found kernel image: $kernelName")

        // 6. Prepare Boot Image (if not provided, dump it)
        if (targetBootUri == null) {
            onStdout("Dumping current boot image...")
            val findBootCmd = """
                SLOT=${'$'}(getprop ro.boot.slot_suffix)
                if [ -e /dev/block/by-name/boot${'$'}SLOT ]; then
                    echo /dev/block/by-name/boot${'$'}SLOT
                elif [ -e /dev/block/bootdevice/by-name/boot${'$'}SLOT ]; then
                    echo /dev/block/bootdevice/by-name/boot${'$'}SLOT
                elif [ -e /dev/block/by-name/boot ]; then
                    echo /dev/block/by-name/boot
                else
                    echo ""
                fi
            """.trimIndent()
            
            var bootPart = ""
            withNewRootShell {
                val out = newJob().add(findBootCmd).to(ArrayList(), null).exec().out
                bootPart = out.firstOrNull()?.trim() ?: ""
            }

            if (bootPart.isEmpty()) {
                return FlashResult(1, "Could not detect boot partition. Please use 'Select File' mode.", false)
            }
            onStdout("Boot partition: $bootPart")
            
            val dumpResult = flashWithIO("dd if='$bootPart' of='$workDirInfo/boot.img'", { }, { })
            if (!dumpResult.isSuccess) return FlashResult(dumpResult.code, "Failed to dump boot image", false)
        }

        // 7. Unpack Boot
        onStdout("Unpacking boot image...")
        val unpackCmd = "cd '$workDirInfo' && $magiskboot unpack '$workDirInfo/boot.img'"
        val unpackResult = flashWithIO(unpackCmd, onStdout, onStderr)
        if (!unpackResult.isSuccess) return FlashResult(unpackResult.code, "Failed to unpack boot image", false)

        // 8. Replace Kernel
        onStdout("Replacing kernel...")
        // Check if 'kernel' exists
        val checkKernelCmd = "[ -f '$workDirInfo/kernel' ]"
        val checkResult = withNewRootShell { newJob().add(checkKernelCmd).exec() }
        if (!checkResult.isSuccess) {
             return FlashResult(1, "Unpacked boot image does not contain a 'kernel' file", false)
        }
        
        val replaceCmd = "cp -f '$kernelPath' '$workDirInfo/kernel'"
        val replaceResult = flashWithIO(replaceCmd, onStdout, onStderr)
        if (!replaceResult.isSuccess) return FlashResult(replaceResult.code, "Failed to replace kernel", false)

        // KPN Auto-Patch
        if (enableKpn) {
            onStdout("KPN enabled. Patching kernel...")
            val libDir = ksuApp.applicationInfo.nativeLibraryDir
            val copyLibsCmd = """
                cp '$libDir/libkptools.so' '$workDirInfo/kptools' && \
                cp '$libDir/libkpimg.so' '$workDirInfo/kpimg' && \
                chmod 755 '$workDirInfo/kptools'
            """.trimIndent()
            
            val copyLibsResult = flashWithIO(copyLibsCmd, onStdout, onStderr)
             if (!copyLibsResult.isSuccess) return FlashResult(copyLibsResult.code, "Failed to copy KPN tools", false)

             val patchCmd = """
                cd '$workDirInfo' && \
                mv kernel kernel.ori && \
                ./kptools -p -i kernel.ori -k kpimg -o kernel
             """.trimIndent()
             
             val patchResult = flashWithIO(patchCmd, onStdout, onStderr)
             if (!patchResult.isSuccess) {
                  return FlashResult(patchResult.code, "KPN Patch failed", false)
             }
             onStdout("KPN Patch successful!")
        }

        // 9. Repack
        onStdout("Repacking boot image...")
        val repackCmd = "cd '$workDirInfo' && $magiskboot repack '$workDirInfo/boot.img'"
        val repackResult = flashWithIO(repackCmd, onStdout, onStderr)
        if (!repackResult.isSuccess) return FlashResult(repackResult.code, "Failed to repack boot image", false)

        // 10. Finalize
        val newBootPath = "$workDirInfo/new-boot.img"
        val checkNewBootCmd = "[ -f '$newBootPath' ]"
        val checkNewBootResult = withNewRootShell { newJob().add(checkNewBootCmd).exec() }
        if (!checkNewBootResult.isSuccess) {
             return FlashResult(1, "Repack failed: new-boot.img not found", false)
        }

        if (targetBootUri == null) {
            // Direct Install -> Flash
            onStdout("Flashing new boot image...")
            val findBootCmd = """
                SLOT=${'$'}(getprop ro.boot.slot_suffix)
                if [ -e /dev/block/by-name/boot${'$'}SLOT ]; then
                    echo /dev/block/by-name/boot${'$'}SLOT
                elif [ -e /dev/block/bootdevice/by-name/boot${'$'}SLOT ]; then
                    echo /dev/block/bootdevice/by-name/boot${'$'}SLOT
                elif [ -e /dev/block/by-name/boot ]; then
                    echo /dev/block/by-name/boot
                else
                    echo ""
                fi
            """.trimIndent()
            
            var bootPart = ""
            withNewRootShell {
                val out = newJob().add(findBootCmd).to(ArrayList(), null).exec().out
                bootPart = out.firstOrNull()?.trim() ?: ""
            }
            
            if (bootPart.isEmpty()) return FlashResult(1, "Boot partition not found for flashing", false)
            
            val flashCmd = "dd if='$newBootPath' of='$bootPart'"
            val flashResult = flashWithIO(flashCmd, onStdout, onStderr)
            
            if (flashResult.isSuccess) {
                onStdout("Flashing complete!")
                return FlashResult(0, "", true)
            } else {
                return FlashResult(flashResult.code, "Failed to flash boot image", false)
            }
        } else {
            // Patch File -> Save
            val timestamp2 = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val destPath = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "boot-magiskboot-${timestamp2}.img")
            
            onStdout("Saving to ${destPath.absolutePath}...")
            val saveCmd = "cp '$newBootPath' '${destPath.absolutePath}' && chmod 644 '${destPath.absolutePath}'"
            val saveResult = flashWithIO(saveCmd, onStdout, onStderr)
            
            if (saveResult.isSuccess) {
                 onStdout("Done!")
                 return FlashResult(0, "", false)
            } else {
                 return FlashResult(saveResult.code, "Failed to save to Downloads", false)
            }
        }

    } catch (e: Exception) {
        return FlashResult(1, "Exception: ${e.message}", false)
    } finally {
        // Cleanup staging
        stagingDir.deleteRecursively()
        // Cleanup workDir (Root)
        withNewRootShell {
             ShellUtils.fastCmdResult(this, "rm -rf '$workDirInfo'")
        }
    }
}

fun patchAnyKernelZip(
    uri: Uri,
    onStdout: (String) -> Unit,
    onStderr: (String) -> Unit
): FlashResult {
    val resolver = ksuApp.contentResolver

    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val tmpFile = File(ksuApp.cacheDir, "anykernel_${timestamp}.zip")
    resolver.openInputStream(uri).use { input ->
        tmpFile.outputStream().use { out ->
            input?.copyTo(out)
        }
    }

    val destZip = tmpFile.absolutePath
    val destZipName = File(destZip).name
    val destDirFile = File(ksuApp.cacheDir, "anykernel3_${timestamp}")
    val destDir = destDirFile.absolutePath

    // We append the override functions to ak3-core.sh (if it exists) or inject them via a wrapper script.
    // Since we don't know exactly where ak3-core.sh is (it's inside the zip), we'll rely on the fact that
    // standard AnyKernel3 zips have tools/ak3-core.sh.
    // However, the safest way is to prepend the override to the execution environment or modify the files after unzip.
    // We unzip first, then modify, then run.

    val overrides = """
        # Override flash_boot to save instead of flash
        flash_boot() {
          ui_print " " "NOT FLASHING - SAVING TO FILE..."
          local timestamp=$(date +%Y%m%d_%H%M%S)
          local dest="/sdcard/Download/boot-patched-${timestamp}.img"
          cp boot-new.img "${"$"}dest"
          ui_print " " "Saved to ${"$"}dest"
        }

        # Override generic flash (for vendor_boot, dtbo, etc.)
        flash_generic() {
          local name="${"$"}1"
          local imgFile="${"$"}name"
          [ -f "${"$"}name.img" ] && imgFile="${"$"}name.img"

          if [ -f "${"$"}imgFile" ]; then
             ui_print " " "NOT FLASHING ${"$"}name - SAVING TO FILE..."
             local timestamp=$(date +%Y%m%d_%H%M%S)
             local dest="/sdcard/Download/${"$"}name-patched-${timestamp}.img"
             cp "${"$"}imgFile" "${"$"}dest"
             ui_print " " "Saved to ${"$"}dest"
          fi
        }
    """.trimIndent()

    // We need to write this override to a file and source it, or append it to ak3-core.sh.
    // Let's try appending to tools/ak3-core.sh if it exists, otherwise we might be in trouble if the zip structure is weird.
    // But wait, the update-binary (shell script) usually sources ak3-core.sh.
    // If we append to ak3-core.sh, it will be loaded.

    // Unzip command
    // We need to unzip everything first to modify it.
    // But flashAnyKernelZip uses "unzip -p ... > update-binary" and then runs it with the zip as argument.
    // Standard AK3 runs from the zip directly (passed as arg 3).
    // Wait, `anykernel.sh` (the update-binary) unzips the rest of itself.
    // If we want to intercept, we need to modify the files *after* `anykernel.sh` extracts them?
    // No, `anykernel.sh` usually contains `unzip -o "$3"` (where $3 is the zip path) to extract itself to /tmp/anykernel.

    // If we want to override, we have a problem: the script extracts *fresh* copies of tools/ak3-core.sh from the zip, overwriting anything we put there.
    // UNLESS we modify the ZIP file itself? No, that's hard.
    // OR we modify the `anykernel.sh` that we extract and run as `update-binary`.
    // In `flashAnyKernelZip`:
    // $BUSYBOX unzip -p -o '$destZip' "META-INF/com/google/android/update-binary" > '$destDir/update-binary'
    // This extracts the entry point.
    // If we append our overrides to *this* `update-binary` (which is `anykernel.sh`), they will be defined.
    // AND since `anykernel.sh` sources `ak3-core.sh`, if we define our functions *after* the source command (or if we define them *before* and `ak3-core.sh` uses `function name() { ... }` style which might overwrite... shell functions can be overwritten).
    // If `ak3-core.sh` defines `flash_boot`, and we define `flash_boot` *after* sourcing it, ours wins.
    // If we define it *before*, `ak3-core.sh` wins.
    // `anykernel.sh` usually has `. tools/ak3-core.sh` near the top.
    // So we should append our overrides to the *end* of `update-binary`.
    // BUT `anykernel.sh` usually ends with `write_boot`.
    // If we append *after* `write_boot`, it's too late.

    // So we need to insert our overrides *after* `. tools/ak3-core.sh` but *before* `flash_boot` or `write_boot` is called.
    // Or, we can use `sed` to inject the overrides right after the source line.

    // The source line is usually `. tools/ak3-core.sh`.
    // Let's try to inject right after that.

    val cmd = """
                mkdir -p '$destDir' && \
                $BUSYBOX unzip -p -o '$destZip' "META-INF/com/google/android/update-binary" > '$destDir/update-binary' 2>/dev/null && \
                cp '$destZip' '$destDir/$destZipName' 2>/dev/null || true && \
                $BUSYBOX chmod 755 '$destDir/update-binary' && \
                $BUSYBOX chown root:root '$destDir/update-binary' && \
                (cd '$destDir' && \
                    if [ -f './update-binary' ]; then \
                        # Inject overrides
                        $BUSYBOX sed -i '/\. tools\/ak3-core.sh/a \
                        flash_boot() { \
                          ui_print " " "NOT FLASHING - SAVING TO FILE..."; \
                          local timestamp=$(date +%Y%m%d_%H%M%S); \
                          local dest="/sdcard/Download/boot-patched-${"$"}{timestamp}.img"; \
                          cp boot-new.img "${"$"}{dest}"; \
                          ui_print " " "Saved to ${"$"}{dest}"; \
                        } \
                        flash_generic() { \
                          local name="${"$"}{1}"; \
                          local imgFile="${"$"}{name}"; \
                          [ -f "${"$"}{name}.img" ] && imgFile="${"$"}{name}.img"; \
                          if [ -f "${"$"}{imgFile}" ]; then \
                             ui_print " " "NOT FLASHING ${"$"}{name} - SAVING TO FILE..."; \
                             local timestamp=$(date +%Y%m%d_%H%M%S); \
                             local dest="/sdcard/Download/${"$"}{name}-patched-${"$"}{timestamp}.img"; \
                             cp "${"$"}{imgFile}" "${"$"}{dest}"; \
                             ui_print " " "Saved to ${"$"}{dest}"; \
                          fi; \
                        }' ./update-binary; \
                        AKHOME='$destDir/tmp' $BUSYBOX ash '$destDir/update-binary' 3 1 '$destDir/$destZipName'; \
                    else \
                        echo 'No installer script found' >&2; exit 1; \
                    fi)
            """.trimIndent().replace(Regex("\\s+\\\\\\s*"), " ")

    val result = flashWithIO_ak3(cmd, onStdout, onStderr)
    try {
        return FlashResult(result, result.isSuccess)
    } finally {
        try {
            runCatching {
                createRootShell(true).use { sh ->
                    sh.newJob().add("rm -rf '$destDir' '$destZip'").exec()
                }
            }
        } catch (_: Throwable) {
        }
    }
}

fun rootAvailable() = Shell.isAppGrantedRoot() == true

fun isInitBoot(): Boolean {
    return !Os.uname().release.contains("android12-")
}

suspend fun getCurrentKmi(): String = withContext(Dispatchers.IO) {
    val cmd = "boot-info current-kmi"
    ShellUtils.fastCmd("${getKsuDaemonPath()} $cmd")
}

suspend fun getSupportedKmis(): List<String> = withContext(Dispatchers.IO) {
    val cmd = "boot-info supported-kmis"
    val out = Shell.cmd("${getKsuDaemonPath()} $cmd").to(ArrayList(), null).exec().out
    out.filter { it.isNotBlank() }.map { it.trim() }
}

suspend fun isAbDevice(): Boolean = withContext(Dispatchers.IO) {
    val cmd = "boot-info is-ab-device"
    ShellUtils.fastCmd("${getKsuDaemonPath()} $cmd").trim().toBoolean()
}

suspend fun getDefaultPartition(): String = withContext(Dispatchers.IO) {
    if (rootAvailable()) {
        val cmd = "boot-info default-partition"
        ShellUtils.fastCmd("${getKsuDaemonPath()} $cmd").trim()
    } else {
        if (!Os.uname().release.contains("android12-")) "init_boot" else "boot"
    }
}

suspend fun getSlotSuffix(ota: Boolean): String = withContext(Dispatchers.IO) {
    val cmd = if (ota) {
        "boot-info slot-suffix --ota"
    } else {
        "boot-info slot-suffix"
    }
    ShellUtils.fastCmd("${getKsuDaemonPath()} $cmd").trim()
}

suspend fun getAvailablePartitions(): List<String> = withContext(Dispatchers.IO) {
    val shell = createRootShell(true)
    val cmd = "boot-info available-partitions"
    val out = shell.newJob().add("${getKsuDaemonPath()} $cmd").to(ArrayList(), null).exec().out
    out.filter { it.isNotBlank() }.map { it.trim() }
}

fun hasMagisk(): Boolean {
    val result = ShellUtils.fastCmdResult("which magisk")
    Log.i(TAG, "has magisk: $result")
    return result
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

fun moduleMigration(): Boolean {
    val command = "cp -rp /data/adb/modules/* /data/adb/modules_update"
    return ShellUtils.fastCmdResult(command)
}

fun getSuSFSVersion(): String? {
    val result = ShellUtils.fastCmd("ksu_susfs show version").trim()
    val versionRegex = Regex("v\\d+\\.\\d+\\.\\d+")
    return if (result.isNotBlank() && versionRegex.containsMatchIn(result)) {
        result
    } else {
        null
    }
}

fun getBootId(): String {
    return runCatching {
        File("/proc/sys/kernel/random/boot_id").readText().trim()
    }.getOrDefault("unknown")
}

fun getBBGVersion(): String? {
    // 1. Check cache first
    val prefs = ksuApp.getSharedPreferences("bbg_cache", Context.MODE_PRIVATE)
    val cachedBootId = prefs.getString("boot_id", null)
    val currentBootId = getBootId()
    
    if (cachedBootId == currentBootId) {
        val cachedVersion = prefs.getString("version", null)
        if (cachedVersion != null) return cachedVersion
    }

    // 2. Scan dmesg if cache miss or invalid
    val result = ShellUtils.fastCmd("grep 'baseband_guard version:' /data/adb/ksu/log/dmesg.log | tail -n 1").trim()
    
    return if (result.isNotBlank()) {
        val version = result.substringAfter("baseband_guard version: ").trim()
        // 3. Update cache
        prefs.edit()
            .putString("boot_id", currentBootId)
            .putString("version", version)
            .apply()
        version
    } else {
        null
    }
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
    return !Natives.isSuEnabled()
}

fun zygiskRequired(dir: File): Boolean {
    return (SuFile(dir, "zygisk").listFiles()?.size ?: 0) > 0
}

fun getZygiskImplementation(property: String): String {
    val modulesPath = "/data/adb/modules"
    val zygiskModuleIds = arrayOf("rezygisk", "zygisksu")

    for (moduleId in zygiskModuleIds) {
        val moduleDir = SuFile.open("$modulesPath/$moduleId")
        if (!moduleDir.isDirectory) continue
        if (SuFile.open("$modulesPath/$moduleId/disable").isFile ||
            SuFile.open("$modulesPath/$moduleId/remove").isFile
        ) continue

        val propFile = SuFile.open("$modulesPath/$moduleId/module.prop")
        if (!propFile.isFile) continue

        val prop = Properties().apply { load(propFile.newInputStream()) }
        prop.getProperty(property)?.let {
            Log.i(TAG, "Zygisk $property: $it")
            return it
        }
    }

    Log.i(TAG, "Zygisk $property: None")
    return "None"
}

fun refreshActivity(context: Context) {
    if (context is Activity) {
        context.recreate()
    }
}

fun restartActivity(context: Context) {
    val packageManager = context.packageManager
    val intent = packageManager.getLaunchIntentForPackage(context.packageName)
    intent?.addFlags(
        Intent.FLAG_ACTIVITY_NEW_TASK or
        Intent.FLAG_ACTIVITY_CLEAR_TASK or
        Intent.FLAG_ACTIVITY_CLEAR_TOP
    )
    context.startActivity(intent)
    if (context is Activity) {
        context.finish()
    }
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

fun installKpn(
    bootUri: Uri?,
    onStdout: (String) -> Unit,
    onStderr: (String) -> Unit
): FlashResult {
    val resolver = ksuApp.contentResolver

    // Prepare work directory
    val workDir = File(ksuApp.cacheDir, "kpn_patch")
    if (workDir.exists()) {
        workDir.deleteRecursively()
    }
    workDir.mkdirs()

    // Copy boot.img
    val bootFile = File(workDir, "boot.img")
    var bootDevice: String? = null

    if (bootUri != null) {
        try {
            resolver.openInputStream(bootUri)?.use { input ->
                bootFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return FlashResult(1, "Failed to open boot URI", false)
        } catch (e: Exception) {
            return FlashResult(1, "Failed to copy boot image: ${e.message}", false)
        }
    } else {
        // Direct install
        if (!rootAvailable()) {
            return FlashResult(1, "Root access required for direct install", false)
        }
        onStdout("- Detecting boot partition...\n")
        // KPN always patches the kernel, which is in boot partition (never init_boot)
        val partition = "boot"
        val suffix = ShellUtils.fastCmd("${getKsuDaemonPath()} boot-info slot-suffix").trim()
        
        // Verify partition exists
        val checkPath = "/dev/block/by-name/$partition$suffix"
        if (!ShellUtils.fastCmdResult("ls $checkPath")) {
            return FlashResult(1, "Boot partition not found: $checkPath", false)
        }

        bootDevice = "/dev/block/by-name/$partition$suffix"
        onStdout("- Boot partition: $bootDevice\n")
        
        onStdout("- Dumping boot image...\n")
        val dumpResult = ShellUtils.fastCmdResult("dd if=$bootDevice of=${bootFile.absolutePath}")
        if (!dumpResult) {
            return FlashResult(1, "Failed to dump boot image", false)
        }
    }

    // Copy tools
    val libDir = ksuApp.applicationInfo.nativeLibraryDir
    val tools = mapOf(
        "libmagiskboot.so" to "magiskboot",
        "libkptools.so" to "kptools",
        "libkpimg.so" to "kpimg"
    )

    try {
        tools.forEach { (libName, binName) ->
            val libFile = File(libDir, libName)
            val binFile = File(workDir, binName)
            if (!libFile.exists()) {
                return FlashResult(1, "Required binary $libName not found in $libDir", false)
            }
            libFile.copyTo(binFile, overwrite = true)
        }
    } catch (e: Exception) {
        return FlashResult(1, "Failed to copy tools: ${e.message}", false)
    }

    // Construct command
    val cmd = """
        cd '${workDir.absolutePath}' && \
        chmod 755 magiskboot kptools && \
        echo "- Unpacking boot image" && \
        ./magiskboot unpack boot.img && \
        if [ -f kernel ]; then \
            mv kernel kernel.ori && \
            echo "- Patching kernel" && \
            ./kptools -p -i kernel.ori -k kpimg -o kernel && \
            echo "- Repacking boot image" && \
            ./magiskboot repack boot.img; \
        else \
            echo "! Kernel not found"; exit 1; \
        fi
    """.trimIndent()

    val result = flashWithIO(cmd, onStdout, onStderr)

    if (result.isSuccess) {
        val newBoot = File(workDir, "new-boot.img")
        if (newBoot.exists()) {
            if (bootUri != null) {
                // Copy new-boot.img to Downloads
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val destFile = File(downloadsDir, "kpn-new-boot.img")
                try {
                    newBoot.copyTo(destFile, overwrite = true)
                    onStdout("\nPatch successful! Output saved to: ${destFile.absolutePath}")
                } catch (e: Exception) {
                    onStderr("\nFailed to copy output file: ${e.message}")
                }
            } else {
                // Flash back
                if (bootDevice != null) {
                    onStdout("- Flashing new boot image to $bootDevice...\n")
                    val flashResult = ShellUtils.fastCmdResult("dd if=${newBoot.absolutePath} of=$bootDevice")
                    if (flashResult) {
                        onStdout("\nFlash successful!\n")
                        return FlashResult(0, "", true)
                    } else {
                        return FlashResult(1, "Failed to flash boot image", false)
                    }
                }
            }
        } else {
            onStderr("\nPatch failed: new-boot.img not found")
            return FlashResult(1, "Patch failed: output not generated", false)
        }
    }

    return FlashResult(result, false)
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
