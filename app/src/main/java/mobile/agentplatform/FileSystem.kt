package mobile.agentplatform

import android.content.Context
import android.net.Uri
import android.os.AsyncTask
import android.os.Environment
import android.util.Log
import org.json.JSONObject
import java.io.*
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

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

    fun getFileContent(filePath: String): String {
        var stream = FileInputStream(filePath)
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

    fun getAppsDir(): String {
        return context.filesDir.absolutePath + "/apps"
    }

    fun getPackagesDir(): String {
        return context.filesDir.absolutePath + "/packages"
    }

    /**
     * Copy assets files into working directory of the application
     */
    fun copyAssetsToWorkingDir(assetsParentName: String, fileName: String, pathToWorkingDir: String): Boolean {
        var assetFiles = context.assets.list(assetsParentName)

        if (assetFiles.contains(fileName)) {
            val toPath = File(pathToWorkingDir, fileName)
            return copyAsset("$assetsParentName/$fileName", toPath)
        }
        return false
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

        val nodeDirectory = File(context.filesDir.toString(), "node_modules")
        if (nodeDirectory.exists()) {
            nodeDirectory.delete()
            val date1 = Date()
            val time1 = date1.getTime()
            Log.i("App-Migratory-Platform", "deleted: " + time1.toString())
        }

//        parallelUnzip(file,targetDirectory)

//        unzip(file, targetDirectory)
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

    /**
     * Unzip given zip file into particular directory
     */
    fun unzip(
        zipFile: File,
        targetDirectory: File,
        asyncTask: FreshConfigActivity.UnzipNodeModulesAsyncTask?
    ): Boolean {
        val BUFFER_SIZE = 2048//8192
        var unzipSuccess = true

        val zipInputStream = ZipInputStream(
            BufferedInputStream(FileInputStream(zipFile))
        )

        try {
            var zipEntry: ZipEntry? = null
            var count = 0
            var progress = 0
            val buffer = ByteArray(BUFFER_SIZE)

            while ({ zipEntry = zipInputStream.nextEntry; zipEntry }() != null) {
                val file = File(targetDirectory, zipEntry?.name)
                val dir = if (zipEntry!!.isDirectory) file else file.parentFile

                if (!dir.isDirectory && !dir.mkdirs()) {
                    throw FileNotFoundException("Failed to ensure directory: " + dir.absolutePath)
                }

                if (zipEntry!!.isDirectory) {
                    continue
                }

                val fileOutputStream = FileOutputStream(file)
                fileOutputStream.use { fileOutputStream ->
                    while ({ count = zipInputStream.read(buffer); count }() != -1) {
                        fileOutputStream.write(buffer, 0, count)
                    }
                }

                asyncTask?.publishProgressCallBack(progress++)
            }

        } catch (e: Exception) {
            e.printStackTrace()
            unzipSuccess = false
        } finally {
            zipInputStream.close()
        }

        return unzipSuccess
    }

    fun getZipEntriesCount(zipFile: File): Long? {
        val zipInputStream = ZipInputStream(
            BufferedInputStream(FileInputStream(zipFile))
        )
        var count: Long = 0
        var failed = false

        try {
            var zipEntry: ZipEntry? = null

            while ({ zipEntry = zipInputStream.nextEntry; zipEntry }() != null) {

                if (!zipEntry!!.isDirectory) {
                    count++
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
            failed = true
        } finally {
            zipInputStream.close()
        }

        return if (failed) null else count
    }

    /**
     * Delete only file
     */
    fun deleteFile(file: File): Boolean {
        if (!file.isDirectory) {
            try {
                return file.delete()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return false
    }

    /**
     * Extract content by help of content resolver
     */
    fun unzipByIntent(
        zipUri: Uri,
        targetDirectory: File,
        asyncTask: ActionSendZipActivity.ExtractPackageAsyncTask?
    ): Boolean {
        val zipInputStream = ZipInputStream(
            BufferedInputStream(context.contentResolver.openInputStream(zipUri))
        )
        val BUFFER_SIZE = 2048
        var unzipSuccess = true

        try {
            var zipEntry: ZipEntry? = null
            var count = 0
            var progress = 0
            val buffer = ByteArray(BUFFER_SIZE)
            while ({ zipEntry = zipInputStream.nextEntry; zipEntry }() != null) {
                val file = File(targetDirectory, zipEntry?.name)
                val dir = if (zipEntry!!.isDirectory) file else file.parentFile

                if (!dir.isDirectory && !dir.mkdirs()) {
                    throw FileNotFoundException("Failed to ensure directory: " + dir.absolutePath)
                }

                if (zipEntry!!.isDirectory) {
                    continue
                }
                val fileOutputStream = FileOutputStream(file)
                try {
                    while ({ count = zipInputStream.read(buffer); count }() != -1) {
                        fileOutputStream.write(buffer, 0, count)
                    }
                } finally {
                    fileOutputStream.close()
                }

                asyncTask?.publishProgressCallBack(progress++)

            }
        } catch (e: IllegalStateException) {
            val date = Date()
            val time = date.getTime()
            Log.i("App-Migratory-Platform", "unzip finished: " + time.toString())
        } finally {
            zipInputStream.close()
        }

        return unzipSuccess
    }

    /**
     * Search package.json file in zip file and return the content
     */
    fun scanPackageJson(zipUri: Uri): JSONObject? {
        val zipInputStream = ZipInputStream(
            BufferedInputStream(context.contentResolver.openInputStream(zipUri))
        )
        val BUFFER_SIZE = 2048

        try {
            var zipEntry: ZipEntry? = null
            var count = 0
            val buffer = ByteArray(BUFFER_SIZE)
            while ({ zipEntry = zipInputStream.nextEntry; zipEntry }() != null) {

                if (zipEntry?.name!!.contains(AppConstant.PACKAGE_JSON) && !zipEntry?.name!!.contains(AppConstant.NODE_MODULES)) {

                    var str = ""
                    while ({ count = zipInputStream.read(buffer); count }() != -1) {
                        str += String(buffer, StandardCharsets.UTF_8)
                    }

                    return JSONObject(str)
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            zipInputStream.close()
        }

        return null
    }

    /**
     * Return array of zip entries names except node_modules
     */
    fun getZipEntries(zipUri: Uri): Pair<MutableList<String>, MutableList<String>>? {
        val zipInputStream = ZipInputStream(
            BufferedInputStream(context.contentResolver.openInputStream(zipUri))
        )

        try {
            var zipEntry: ZipEntry? = null
            var zipEntries = mutableListOf<String>()
            var zipEntriesNodeModules = mutableListOf<String>()
            while ({ zipEntry = zipInputStream.nextEntry; zipEntry }() != null) {

                if (!zipEntry!!.isDirectory) {
                    if (!zipEntry?.name!!.contains(AppConstant.NODE_MODULES)) {

                        zipEntries.add(zipEntry!!.name)
                    } else {
                        zipEntriesNodeModules.add(zipEntry!!.name)
                    }
                }
            }

            return Pair(zipEntries, zipEntriesNodeModules)

        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            zipInputStream.close()
        }

        return null
    }


    @Volatile
    lateinit var zis: ZipInputStream
    @Volatile
    var finished: AtomicBoolean = AtomicBoolean(false)

    @Synchronized
    fun getNextEntry(): ZipEntry {
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
            while (!finished.get()) {
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

    fun zipDir(src: File, dest: File): Boolean {
        try {
            val fileOut = FileOutputStream(dest)
            val zipOut = ZipOutputStream(BufferedOutputStream(fileOut))

            return if (src.isDirectory) {
                zipSubFolder(zipOut, src, src.parent.length)
                zipOut.close()

                true
            } else {
                zipOut.close()

                false
            }

        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    @Throws(IOException::class)
    private fun zipSubFolder(zipOut: ZipOutputStream, dir: File, basePathLength: Int) {

        val BUFFER_SIZE = 2048

        val fileList = dir.listFiles()
        var bufferedInputStream: BufferedInputStream? = null
        for (file in fileList) {
            if (file.isDirectory) {
                if (file.listFiles().isNotEmpty()) {
                    zipSubFolder(zipOut, file, basePathLength)

                } else {
                    val relativePath = file.path.substring(basePathLength).substring(1) + "/"
                    val entry = ZipEntry(relativePath)
                    entry.time = file.lastModified()
                    zipOut.putNextEntry(entry)
                }

            } else {
                val data = ByteArray(BUFFER_SIZE)
                val relativePath = file.absolutePath.substring(basePathLength).substring(1)
                val fileInputStream = FileInputStream(file)
                bufferedInputStream = BufferedInputStream(fileInputStream, BUFFER_SIZE)
                val entry = ZipEntry(relativePath)
                entry.time = file.lastModified()
                zipOut.putNextEntry(entry)
                var count = 0
                while ({ count = bufferedInputStream.read(data, 0, BUFFER_SIZE);count }() != -1) {
                    zipOut.write(data, 0, count)
                }
                bufferedInputStream.close()
            }
        }
    }
}
