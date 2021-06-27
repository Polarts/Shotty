package com.polarts.shotty

import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.FileObserver
import androidx.annotation.RequiresApi
import java.io.*

@RequiresApi(Build.VERSION_CODES.Q)
class ScreenshotFileObserver(
    var onFinalize: () -> Unit,
    val context: Context
): FileObserver(
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

            var appName = path.split('_').last()
            appName = appName.substring(0, appName.lastIndexOf('.'))
            log("[$tag] New screenshot detected for app $appName! Trying to move...")
            moveFile("$rootPath/Screenshots/", path, "$rootPath/$appName/")
        }
    }

    private fun moveFile(inputPath: String, inputFile: String, outputPath: String) {
        try {
            //create output directory if it doesn't exist
            val dir = File(outputPath)
            if (!dir.exists()) {
                dir.mkdirs()
            }

            val inputChannel = FileInputStream(inputPath + inputFile).channel
            val outputChannel = FileOutputStream(outputPath + inputFile).channel
            inputChannel.transferTo(0, inputChannel.size(), outputChannel)
            inputChannel.close()
            outputChannel.close()

            // delete the original file
            if (!File(inputPath + inputFile).delete()) {
                log("[$tag] Could ot delete $inputPath$inputFile for unknown reason")
            }
        } catch (e: Exception) {
            e.message?.let { log("[$tag] EXCEPTION: could not complete file movement: $it") }
        }
    }

}