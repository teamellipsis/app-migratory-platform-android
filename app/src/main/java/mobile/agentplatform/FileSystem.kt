package mobile.agentplatform

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import java.io.*
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class FileSystem {

    var context: Context

    constructor(context: Context) {
        this.context = context
    }

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

    fun getFileDir(): String {
//        return Environment.getDataDirectory().absolutePath // /data
//        return Environment.getRootDirectory().absolutePath  //  /system
//        return Environment.getDownloadCacheDirectory().absolutePath //  /cache
        return context.filesDir.absolutePath
    }

    fun copyAssetsToFilesDir(): String {
        var files = context.assets.list("node")
        var str = ""
        for (filePath in files) {
//            val file = File(context.filesDir.absolutePath, filePath)
//            val file = File(getExternalStorageDir()+"/FYP/new", filePath)
            val file = File(context.filesDir, filePath)
            copyAsset("node" + "/" + filePath, file)
            str += filePath + " | "
        }

        val date = Date()
        val time = date.getTime()
        Log.i("App-Migratory-Platform", "copied: " + time.toString())

        var count = 0
        for (filePath in File(context.filesDir.toString()).list()) {
            Log.i("App-Migratory-Platform", count++.toString() + " : " + filePath.toString())
        }

        Log.i("App-Migratory-Platform", "no of  processors: " + Runtime.getRuntime().availableProcessors())
//        return str

//        val file = File(getExternalStorageDir()+"/FYP/new", "node_modules.zip")
//        val targetDirectory = File(getExternalStorageDir() + "/FYP/new")

        val file = File(context.filesDir, "node_modules.zip")
        val targetDirectory = File(context.filesDir.toString())

        val nodeDirectory = File(context.filesDir.toString(),"node_modules")
        if (nodeDirectory.exists()) {
            nodeDirectory.delete()
            val date1 = Date()
            val time1 = date1.getTime()
            Log.i("App-Migratory-Platform", "deleted: " + time1.toString())
        }

//        parallelUnzip(file,targetDirectory)

        unzip(file, targetDirectory)
        return nodeDirectory.absolutePath
    }

    private fun copyAsset(fromAssetPath: String, toFile: File): Boolean {
        var inputStream: InputStream? = null
        var outStream: OutputStream? = null
        try {
            inputStream = context.assets.open(fromAssetPath)
            toFile.createNewFile()
            outStream = FileOutputStream(toFile)
            copyFile(inputStream!!, outStream)
            inputStream!!.close()
            inputStream = null
            outStream!!.flush()
            outStream!!.close()
            outStream = null
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    @Throws(IOException::class)
    private fun copyFile(inputStream: InputStream, outputStream: OutputStream) {
        val buffer = ByteArray(1024)
        var read: Int = -1
        while ({ read = inputStream.read(buffer); read }() != -1) {
            outputStream.write(buffer, 0, read)
        }
    }

    @Throws(IOException::class)
    fun unzip(zipFile: File, targetDirectory: File) {
        val zis = ZipInputStream(
            BufferedInputStream(FileInputStream(zipFile))
        )

        try {
            lateinit var ze: ZipEntry
            var count = 0
            val buffer = ByteArray(8192)
            while ({ ze = zis.getNextEntry(); ze }() != null) {
                val file = File(targetDirectory, ze.getName())
                val dir = if (ze.isDirectory()) file else file.parentFile
                if (!dir.isDirectory && !dir.mkdirs())
                    throw FileNotFoundException("Failed to ensure directory: " + dir.absolutePath)
                if (ze.isDirectory())
                    continue
                val fout = FileOutputStream(file)
                try {
//                    var count = 0
                    while ({ count = zis.read(buffer); count }() != -1)
                        fout.write(buffer, 0, count)
                } finally {
                    fout.close()
                }
                /* if time should be restored as well
            long time = ze.getTime();
            if (time > 0)
                file.setLastModified(time);
            */
            }
        } catch (e: IllegalStateException) {
            val date = Date()
            val time = date.getTime()
            Log.i("App-Migratory-Platform", "unzip finished: " + time.toString())
        } finally {
            zis.close()
        }
    }

    @Throws(IOException::class)
    fun unzipByIntent(zipUri: Uri, targetDirectory: File) {
        val zis = ZipInputStream(
            BufferedInputStream(context.contentResolver.openInputStream(zipUri))
        )

        try {
            lateinit var ze: ZipEntry
            var count = 0
            val buffer = ByteArray(8192)
            while ({ ze = zis.getNextEntry(); ze }() != null) {
                val file = File(targetDirectory, ze.getName())
                val dir = if (ze.isDirectory()) file else file.parentFile
                if (!dir.isDirectory && !dir.mkdirs())
                    throw FileNotFoundException("Failed to ensure directory: " + dir.absolutePath)
                if (ze.isDirectory())
                    continue
                val fout = FileOutputStream(file)
                try {
//                    var count = 0
                    while ({ count = zis.read(buffer); count }() != -1)
                        fout.write(buffer, 0, count)
                } finally {
                    fout.close()
                }
                /* if time should be restored as well
            long time = ze.getTime();
            if (time > 0)
                file.setLastModified(time);
            */
            }
        } catch (e: IllegalStateException) {
            val date = Date()
            val time = date.getTime()
            Log.i("App-Migratory-Platform", "unzip finished: " + time.toString())
        } finally {
            zis.close()
        }
    }


    @Volatile lateinit var zis:ZipInputStream
    @Volatile var finished:AtomicBoolean = AtomicBoolean(false)

    @Synchronized fun getNextEntry():ZipEntry{
        return zis.getNextEntry()
    }

    @Throws(IOException::class)
    fun parallelUnzip(zipFile: File, targetDirectory: File) {
        zis = ZipInputStream(
            BufferedInputStream(FileInputStream(zipFile))
        )

        var threads: MutableList<Thread> = mutableListOf<Thread>()

//        (Runtime.getRuntime().availableProcessors() - 1)
//        for (i in 0..3){
//            threads.add(
//            Thread(Runnable {
//                val date = Date()
//                val time = date.getTime()
//                Log.i("App-Migratory-Platform",i.toString() + "start: " + time.toString())


                try {
                    lateinit var ze: ZipEntry
                    var count = 0
                    val buffer = ByteArray(8192)
//            while ({ ze = zis.getNextEntry(); ze }() != null) {
                    while(!finished.get()) {
                        ze = getNextEntry()
                        val file = File(targetDirectory, ze.getName())
                        val dir = if (ze.isDirectory()) file else file.parentFile
                        if (!dir.isDirectory && !dir.mkdirs())
                            throw FileNotFoundException("Failed to ensure directory: " + dir.absolutePath)
                        if (ze.isDirectory())
                            continue
                        val fout = FileOutputStream(file)
                        try {
//                    var count = 0
                            while ({ count = zis.read(buffer); count }() != -1)
                                fout.write(buffer, 0, count)
                        } finally {
                            fout.close()
                        }
                        /* if time should be restored as well
                    long time = ze.getTime();
                    if (time > 0)
                        file.setLastModified(time);
                    */
                    }
                } catch (e: IllegalStateException) {
                    val date = Date()
                    val time = date.getTime()
                    finished.set(true)
                    Log.i("App-Migratory-Platform", "unzip finished: " + time.toString())
                } finally {
                    zis.close()
                }


//                val date1 = Date()
//                val time1 = date1.getTime()
//                Log.i("App-Migratory-Platform",i.toString() + "end: " + time1.toString())
//                Log.i("App-Migratory-Platform",i.toString() + "diff: " + (time1-time).toString())
//            })
//            )
//            threads[i].start()
//        }
//
//        for (i in 0..3){
//            threads[i].join()
//        }
    }
}
