package mobile.agentplatform

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import org.json.JSONObject
import java.io.*
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class FileManager {

    var context: Context

    constructor(context: Context) {
        this.context = context
    }

    fun getExternalStorageDir(): String {
        return Environment.getExternalStorageDirectory().absolutePath
    }

    fun getFileDir(): String {
        return context.filesDir.absolutePath
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
        appName: String?,
        zipRootDirName: String?,
        asyncTask: ActionSendZipActivity.ExtractPackageAsyncTask?
    ): Boolean {
        val zipInputStream = ZipInputStream(
            BufferedInputStream(context.contentResolver.openInputStream(zipUri))
        )
        val BUFFER_SIZE = 2048
        var unzipSuccess = true
        var isDiffAppName = false

        if (appName != null && zipRootDirName != null && zipRootDirName != appName) {
            isDiffAppName = true
        }

        try {
            var zipEntry: ZipEntry? = null
            var count = 0
            var progress = 0
            val buffer = ByteArray(BUFFER_SIZE)
            while ({ zipEntry = zipInputStream.nextEntry; zipEntry }() != null && !asyncTask!!.isCancelled) {
                var zipEntryName = zipEntry?.name
                if (isDiffAppName) {
                    zipEntryName = zipEntryName?.replaceFirst(zipRootDirName!!, appName!!, false)
                }
                val file = File(targetDirectory, zipEntryName)
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
        } catch (e: Exception) {
            unzipSuccess = false
            e.printStackTrace()
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
        var str = ""
        var success = false

        try {
            var zipEntry: ZipEntry? = null
            var count: Int
            val buffer = ByteArray(BUFFER_SIZE)
            while ({ zipEntry = zipInputStream.nextEntry; zipEntry }() != null) {

                if (zipEntry?.name!!.contains(AppConstant.PACKAGE_JSON) && !zipEntry?.name!!.contains(AppConstant.NODE_MODULES)) {

                    while ({ count = zipInputStream.read(buffer); count }() != -1) {
                        str += String(buffer, StandardCharsets.UTF_8)
                    }

                    success = true
                    break
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            zipInputStream.close()
        }

        return if (success) JSONObject(str) else null
    }

    /**
     * Return array of zip entries names except directories
     */
    fun getZipEntries(zipUri: Uri): Pair<MutableList<String>, MutableList<String>>? {
        val zipInputStream = ZipInputStream(
            BufferedInputStream(context.contentResolver.openInputStream(zipUri))
        )

        var zipEntries = mutableListOf<String>()
        var zipEntriesNodeModules = mutableListOf<String>()
        var failed = false

        try {
            var zipEntry: ZipEntry? = null
            while ({ zipEntry = zipInputStream.nextEntry; zipEntry }() != null) {

                if (!zipEntry!!.isDirectory) {
                    if (!zipEntry?.name!!.contains(AppConstant.NODE_MODULES)) {

                        zipEntries.add(zipEntry!!.name)
                    } else {
                        zipEntriesNodeModules.add(zipEntry!!.name)
                    }
                }
            }

        } catch (e: Exception) {
            failed = true
            e.printStackTrace()
        } finally {
            zipInputStream.close()
        }

        return if (failed) null else Pair(zipEntries, zipEntriesNodeModules)
    }

    fun getZipRootDirName(zipUri: Uri): String? {
        val zipInputStream = ZipInputStream(
            BufferedInputStream(context.contentResolver.openInputStream(zipUri))
        )

        var rootDirName = ""
        var failed = false

        try {
            var zipEntry: ZipEntry? = zipInputStream.nextEntry
            rootDirName = zipEntry?.name!!.replaceFirst(Regex("/.*$"), "")

        } catch (e: Exception) {
            failed = true
            e.printStackTrace()
        } finally {
            zipInputStream.close()
        }

        return if (failed) null else rootDirName
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

    fun getFileContent(file: File): String? {
        var fileContent = ""
        try {
            val stream = FileInputStream(file)
            val inputReader = InputStreamReader(stream)
            val buffReader = BufferedReader(inputReader)
            var line: String? = null
            while ({ line = buffReader.readLine(); line }() != null) {
                fileContent += line
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }

        return fileContent
    }
}
