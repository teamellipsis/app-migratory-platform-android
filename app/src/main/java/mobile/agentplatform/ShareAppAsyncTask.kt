package mobile.agentplatform

import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.support.v4.content.FileProvider
import android.support.v7.app.AlertDialog
import java.io.File

class ShareAppAsyncTask(
    private var context: Context,
    private var appPath: File
) : AsyncTask<String, Int, Boolean>() {

    private var alertDialog: AlertDialog? = null
    private lateinit var fileManager: FileManager
    private lateinit var appConfig: AppConfig

    override fun onPreExecute() {
        super.onPreExecute()
        createAlertDialog()?.show()
    }

    override fun doInBackground(vararg argv: String): Boolean {
        fileManager = FileManager(context.applicationContext!!)
        appConfig = AppConfig(context.applicationContext!!)
        val packagesDir = File(appConfig.get(AppConstant.KEY_PACKAGES_DIR))
        packagesDir.mkdirs()
        return fileManager.zipDir(appPath, File(packagesDir, appPath.name + ".zip"))
    }

    override fun onProgressUpdate(vararg values: Int?) {}

    override fun onPostExecute(appPackageJson: Boolean) {
        val file = File(appConfig.get(AppConstant.KEY_PACKAGES_DIR), appPath.name + ".zip")
        val fileUri = FileProvider.getUriForFile(
            context,
            context.packageName + ".provider",
            file
        )

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            putExtra(Intent.EXTRA_STREAM, fileUri)
            type = "application/zip"
        }

        alertDialog?.dismiss()
        if (sendIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(sendIntent)
        }
    }

    private fun createAlertDialog(): AlertDialog? {
        alertDialog = this.let {
            val builder = AlertDialog.Builder(context)
            builder.apply {
                setCancelable(false)
            }
            builder.create()
        }
        return alertDialog
    }
}
