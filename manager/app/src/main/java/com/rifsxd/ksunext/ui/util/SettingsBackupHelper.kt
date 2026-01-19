package com.rifsxd.ksunext.ui.util

import android.content.Context
import android.net.Uri
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

object SettingsBackupHelper {
    private const val PREFS_NAME = "settings"
    private const val JSON_ENTRY_NAME = "wild_ksu_settings_backup.json"
    private const val LEGACY_JSON_ENTRY_NAME = "ksu_next_settings_backup.json"

    fun backupSettings(context: Context, uri: Uri): Boolean {
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val allEntries = prefs.all
            val jsonObject = JSONObject()

            for ((key, value) in allEntries) {
                jsonObject.put(key, value)
            }

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(jsonObject.toString(4).toByteArray())
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun restoreSettings(context: Context, uri: Uri): Boolean {
        return try {
            val stringBuilder = StringBuilder()
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        stringBuilder.append(line)
                    }
                }
            }

            val jsonObject = JSONObject(stringBuilder.toString())
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val editor = prefs.edit()
            
            // Clear existing settings to ensure clean restore
            editor.clear()

            val keys = jsonObject.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val value = jsonObject.get(key)
                
                when (value) {
                    is Boolean -> editor.putBoolean(key, value)
                    is Int -> editor.putInt(key, value)
                    is Long -> editor.putLong(key, value)
                    is Float -> editor.putFloat(key, value.toFloat()) // JSON might parse as double
                    is Double -> editor.putFloat(key, value.toFloat())
                    is String -> editor.putString(key, value)
                }
            }
            editor.apply()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun backupSettingsArchive(context: Context, uri: Uri): Boolean {
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val allEntries = prefs.all
            val jsonObject = JSONObject()
            for ((key, value) in allEntries) {
                jsonObject.put(key, value)
            }

            val jsonBytes = jsonObject.toString(4).toByteArray(StandardCharsets.UTF_8)
            val backgroundFile = prefs.getString("background_uri", null)?.let { uriString ->
                ownedBackgroundFileOrNull(context, uriString)
            }

            context.contentResolver.openOutputStream(uri)?.use { rawOutput ->
                GZIPOutputStream(BufferedOutputStream(rawOutput)).use { gzipOutput ->
                    Tar.writeFileEntry(
                        output = gzipOutput,
                        name = JSON_ENTRY_NAME,
                        bytes = jsonBytes
                    )
                    if (backgroundFile != null) {
                        Tar.writeFileEntry(
                            output = gzipOutput,
                            name = "backgrounds/${backgroundFile.name}",
                            file = backgroundFile
                        )
                    }
                    Tar.finish(output = gzipOutput)
                }
            } ?: return false

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun restoreSettingsArchive(context: Context, uri: Uri): Boolean {
        return try {
            val backgroundsDir = File(context.filesDir, "backgrounds").apply { mkdirs() }

            var jsonBytes: ByteArray? = null
            context.contentResolver.openInputStream(uri)?.use { rawInput ->
                GZIPInputStream(BufferedInputStream(rawInput)).use { gzipInput ->
                    Tar.readEntries(
                        input = gzipInput,
                        onFile = { name, size, stream ->
                            when {
                                name == JSON_ENTRY_NAME || name == LEGACY_JSON_ENTRY_NAME -> {
                                    val buffer = ByteArrayOutputStream(size.coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
                                    stream.copyTo(buffer)
                                    jsonBytes = buffer.toByteArray()
                                }
                                name.startsWith("backgrounds/") -> {
                                    val fileName = name.substringAfterLast('/').takeIf { it.isNotBlank() } ?: return@readEntries
                                    val dst = File(backgroundsDir, fileName)
                                    dst.outputStream().use { out -> stream.copyTo(out) }
                                }
                            }
                        }
                    )
                }
            } ?: return false

            val bytes = jsonBytes ?: return false
            val jsonObject = JSONObject(String(bytes, StandardCharsets.UTF_8))

            val restoredBackground = (jsonObject.opt("background_uri") as? String)?.let { stored ->
                val storedUri = runCatching { Uri.parse(stored) }.getOrNull()
                storedUri?.lastPathSegment?.let { fileName ->
                    File(backgroundsDir, fileName).takeIf { it.exists() }
                }
            }
            if (restoredBackground != null) {
                jsonObject.put("background_uri", Uri.fromFile(restoredBackground).toString())
            }

            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val editor = prefs.edit()
            editor.clear()

            val keys = jsonObject.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val value = jsonObject.get(key)
                when (value) {
                    is Boolean -> editor.putBoolean(key, value)
                    is Int -> editor.putInt(key, value)
                    is Long -> editor.putLong(key, value)
                    is Float -> editor.putFloat(key, value)
                    is Double -> editor.putFloat(key, value.toFloat())
                    is String -> editor.putString(key, value)
                }
            }

            editor.apply()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun ownedBackgroundFileOrNull(context: Context, uriString: String): File? {
        val uri = runCatching { Uri.parse(uriString) }.getOrNull() ?: return null
        if (uri.scheme != "file") return null
        val path = uri.path ?: return null
        val ownedDir = File(context.filesDir, "backgrounds").absolutePath + File.separator
        if (!path.startsWith(ownedDir)) return null
        return File(path).takeIf { it.exists() && it.isFile }
    }

    private object Tar {
        fun finish(output: java.io.OutputStream) {
            output.write(ByteArray(1024))
        }

        fun writeFileEntry(output: java.io.OutputStream, name: String, bytes: ByteArray) {
            writeHeader(output = output, name = name, size = bytes.size.toLong(), mtime = System.currentTimeMillis() / 1000)
            output.write(bytes)
            padTo512(output, bytes.size.toLong())
        }

        fun writeFileEntry(output: java.io.OutputStream, name: String, file: File) {
            val size = file.length()
            writeHeader(output = output, name = name, size = size, mtime = file.lastModified() / 1000)
            file.inputStream().use { input -> input.copyTo(output) }
            padTo512(output, size)
        }

        fun readEntries(
            input: java.io.InputStream,
            onFile: (name: String, size: Long, stream: java.io.InputStream) -> Unit
        ) {
            val header = ByteArray(512)
            while (true) {
                readFully(input, header)
                if (header.all { it == 0.toByte() }) return

                val name = parseNullTerminatedString(header, 0, 100)
                val size = parseOctalLong(header, 124, 12)
                val typeFlag = header[156].toInt().toChar()

                val limited = LimitedInputStream(input, size)
                if (typeFlag == '0' || typeFlag.code == 0) {
                    onFile(name, size, limited)
                } else {
                    limited.skipRemaining()
                }
                limited.skipRemaining()

                val pad = (512 - (size % 512)).let { if (it == 512L) 0L else it }
                skipFully(input, pad)
            }
        }

        private fun writeHeader(output: java.io.OutputStream, name: String, size: Long, mtime: Long) {
            val header = ByteArray(512)

            writeString(header, 0, 100, name)
            writeOctal(header, 100, 8, 420L) // 0644

            writeOctal(header, 108, 8, 0L)
            writeOctal(header, 116, 8, 0L)
            writeOctal(header, 124, 12, size)
            writeOctal(header, 136, 12, mtime)

            for (i in 148 until 156) header[i] = 0x20
            header[156] = '0'.code.toByte()

            writeString(header, 257, 6, "ustar\u0000")
            writeString(header, 263, 2, "00")

            val checksum = header.sumOf { it.toUByte().toInt() }
            val chk = String.format("%06o\u0000 ", checksum).toByteArray(StandardCharsets.US_ASCII)
            System.arraycopy(chk, 0, header, 148, 8)

            output.write(header)
        }

        private fun padTo512(output: java.io.OutputStream, size: Long) {
            val pad = (512 - (size % 512)).let { if (it == 512L) 0L else it }.toInt()
            if (pad > 0) output.write(ByteArray(pad))
        }

        private fun writeString(buf: ByteArray, offset: Int, length: Int, value: String) {
            val bytes = value.toByteArray(StandardCharsets.US_ASCII)
            val n = minOf(bytes.size, length)
            System.arraycopy(bytes, 0, buf, offset, n)
        }

        private fun writeOctal(buf: ByteArray, offset: Int, length: Int, value: Long) {
            val oct = java.lang.Long.toOctalString(value)
            val str = oct.padStart(length - 1, '0') + "\u0000"
            writeString(buf, offset, length, str)
        }

        private fun parseNullTerminatedString(buf: ByteArray, offset: Int, length: Int): String {
            var end = offset
            val max = offset + length
            while (end < max && buf[end] != 0.toByte()) end++
            return String(buf, offset, end - offset, StandardCharsets.US_ASCII)
        }

        private fun parseOctalLong(buf: ByteArray, offset: Int, length: Int): Long {
            val str = parseNullTerminatedString(buf, offset, length).trim().trim('\u0000', ' ')
            if (str.isBlank()) return 0
            return str.toLong(8)
        }

        private fun readFully(input: java.io.InputStream, buf: ByteArray) {
            var read = 0
            while (read < buf.size) {
                val r = input.read(buf, read, buf.size - read)
                if (r < 0) throw java.io.EOFException()
                read += r
            }
        }

        private fun skipFully(input: java.io.InputStream, bytes: Long) {
            var remaining = bytes
            while (remaining > 0) {
                val skipped = input.skip(remaining)
                if (skipped <= 0) {
                    if (input.read() == -1) throw java.io.EOFException()
                    remaining--
                } else {
                    remaining -= skipped
                }
            }
        }

        private class LimitedInputStream(
            private val input: java.io.InputStream,
            private val limit: Long
        ) : java.io.InputStream() {
            private var remaining = limit

            override fun read(): Int {
                if (remaining <= 0) return -1
                val r = input.read()
                if (r >= 0) remaining--
                return r
            }

            override fun read(b: ByteArray, off: Int, len: Int): Int {
                if (remaining <= 0) return -1
                val toRead = minOf(len.toLong(), remaining).toInt()
                val r = input.read(b, off, toRead)
                if (r > 0) remaining -= r.toLong()
                return r
            }

            fun skipRemaining() {
                if (remaining <= 0) return
                skipFully(input, remaining)
                remaining = 0
            }
        }
    }
}
