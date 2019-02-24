package mobile.agentplatform

import android.os.Environment
import java.io.FileInputStream
import java.io.BufferedReader
import java.io.InputStreamReader

class FileSystem {

    fun isExternalStorageReadable(): Boolean {
        return Environment.getExternalStorageState() in
                setOf(Environment.MEDIA_MOUNTED, Environment.MEDIA_MOUNTED_READ_ONLY)
    }

    fun isExternalStorageWritable(): Boolean {
        return Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
    }

    fun getExternalStorageDir(): String {
        return Environment.getExternalStorageDirectory().absolutePath
    }

    fun getFileContent(fileName: String): String {
        var stream = FileInputStream(getExternalStorageDir() + "/" + fileName)
        val inputReader = InputStreamReader(stream)
        val buffReader = BufferedReader(inputReader)
        var line: String? = null
        var fileContent: String = ""
        while ({ line = buffReader.readLine(); line }() != null) {
            fileContent += line
        }

        return fileContent
    }
}
