package com.polarts.shotty

import android.os.Environment
import android.util.Log
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException


private const val filename = "Shotty_Log"
private val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)

fun log(message: String) {


    if (!dir.exists()) {
        Log.d("Dir created ", "Dir created ")
        dir.mkdirs()
    }
    val logFile = File(dir.absolutePath + "/" + filename + ".txt")
    if (!logFile.exists()) {
        try {
            Log.d("File created ", "File created ")
            logFile.createNewFile()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
    try {
        //BufferedWriter for performance, true to set append to file flag
        val buf = BufferedWriter(FileWriter(logFile, true))
        buf.write("""$message""".trimIndent())
        Log.d("", message)
        buf.newLine()
        buf.flush()
        buf.close()
    } catch (e: IOException) {
        e.printStackTrace()
    }
}
