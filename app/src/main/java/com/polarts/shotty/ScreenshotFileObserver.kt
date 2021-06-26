package com.polarts.shotty

import android.os.Build
import android.os.Environment
import android.os.FileObserver
import android.util.Log
import androidx.annotation.RequiresApi
import java.io.*

@RequiresApi(Build.VERSION_CODES.Q)
class ScreenshotFileObserver(var onFinalize: () -> Unit)
    : FileObserver(
        File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).absolutePath + "/Screenshots"),
        CREATE or MODIFY or MOVED_TO
    ) {

    private val tag = "ScreenshotFileObserver"
    private var rootPath = ""

    init {
        rootPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).absolutePath
    }

    override fun finalize() {
        onFinalize.invoke()
        super.finalize()
    }

    override fun onEvent(event: Int, path: String?) {
        if (path != null
            && !path.startsWith(".pending")) {

            val appName = path?.split('_')?.last()?.split('.')?.first()

            appName?.let {
                moveFile("$rootPath/Screenshots/", path, "$rootPath/$it/")
            }
        }
    }

    private fun moveFile(inputPath: String, inputFile: String, outputPath: String) {
        var inputStream: InputStream? = null
        var outputStream: OutputStream? = null
        try {

            //create output directory if it doesn't exist
            val dir = File(outputPath)
            if (!dir.exists()) {
                dir.mkdirs()
            }
            inputStream = FileInputStream(inputPath + inputFile)
            outputStream = FileOutputStream(outputPath + inputFile)
            val buffer = ByteArray(1024)
            var read: Int
            while (inputStream.read(buffer).also { read = it } != -1) {
                outputStream.write(buffer, 0, read)
            }
            inputStream.close()
            inputStream = null

            // write the output file
            outputStream.flush()
            outputStream.close()
            outputStream = null

            // delete the original file
            File(inputPath + inputFile).delete()
        } catch (e: FileNotFoundException) {
            e.message?.let { Log.e(tag, it) }
        } catch (e: Exception) {
            e.message?.let { Log.e(tag, it) }
        }
    }

}