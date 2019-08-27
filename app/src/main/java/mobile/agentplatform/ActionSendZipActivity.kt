package mobile.agentplatform

import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.AsyncTask
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.view.MenuItem
import android.view.View
import kotlinx.android.synthetic.main.activity_action_send_zip.*
import org.json.JSONObject
import java.io.File

class ActionSendZipActivity : AppCompatActivity() {

    private lateinit var fileSystem: FileSystem
    private lateinit var appConfig: AppConfig
    private lateinit var uri: Uri
    private var packageJson: JSONObject? = null
    private var zipEntries: MutableList<String>? = null
    private var zipEntriesNodeModules: MutableList<String>? = null
    private var extractPackageAsyncTask: ExtractPackageAsyncTask? = null
    private var appName: String? = null
    private var zipRootDirName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_action_send_zip)

        supportActionBar?.setTitle(R.string.title_action_send_zip_activity)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        uri = if (intent.action == Intent.ACTION_VIEW) {
            intent.data
        } else {
            intent.getParcelableExtra(Intent.EXTRA_STREAM) as Uri
        }

        fileSystem = FileSystem(applicationContext)
        appConfig = AppConfig(applicationContext)

        CheckPackageJsonAsyncTask().execute()
        init()
    }

    private fun init() {
        progressAppDetails.visibility = View.VISIBLE
        txtAppDetail.visibility = View.GONE
        cardExecutable.visibility = View.GONE
        progressExecutable.visibility = View.VISIBLE
        txtExecutable.visibility = View.GONE
        btnExtract.isEnabled = false
        btnOpen.visibility = View.GONE
        btnCancel.visibility = View.VISIBLE
        editTxtAppName.isEnabled = true

        zipRootDirName = fileSystem.getZipRootDirName(uri)
        if (zipRootDirName!!.isNotEmpty()) {
            editTxtAppName.setText(zipRootDirName)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        uri = if (intent?.action == Intent.ACTION_VIEW) {
            intent.data
        } else {
            intent?.getParcelableExtra(Intent.EXTRA_STREAM) as Uri
        }

        CheckPackageJsonAsyncTask().execute()
        init()
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item!!.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setAppDetails() {
        txtAppDetail.text = resources.getString(R.string.name_app_detail) + getString("name") +
                resources.getString(R.string.version_app_detail) + getString("version") +
                resources.getString(R.string.description_app_detail) + getString("description") +
                resources.getString(R.string.author_app_detail) + getString("author") +
                resources.getString(R.string.license_app_detail) + getString("license")
    }

    private fun getString(name: String): String {
        var str = packageJson?.getString(name)
        if (str == null || str == "") {
            str = "~"
        }
        return str
    }

    fun extract(view: View) {
        editTxtAppName.isEnabled = false
        btnExtract.isEnabled = false
        extractPackageAsyncTask = ExtractPackageAsyncTask()
        extractPackageAsyncTask?.execute()
    }

    fun cancel(view: View) {
        val alertDialog = AlertDialog.Builder(this).create()
        alertDialog.setTitle("Do you want cancel the extraction?")
        alertDialog.setButton(
            AlertDialog.BUTTON_POSITIVE, "No",
            DialogInterface.OnClickListener { dialog, which ->
                dialog.dismiss()
            }
        )
        alertDialog.setButton(
            AlertDialog.BUTTON_NEGATIVE, "Yes",
            DialogInterface.OnClickListener { dialog, which ->
                extractPackageAsyncTask?.cancel(true)
                val intent = Intent(applicationContext, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
        )
        alertDialog.show()
    }

    fun open(view: View) {
        val agentManager = AgentManager(applicationContext)
        val appsDir = appConfig.get(AppConstant.KEY_APPS_DIR)
        val appDir = File(appsDir, appName)

        agentManager.openApp(appDir)
        finish()
    }

    private inner class CheckPackageJsonAsyncTask : AsyncTask<String, Int, JSONObject>() {
        override fun doInBackground(vararg argv: String): JSONObject? {
            return fileSystem.scanPackageJson(uri)
        }

        override fun onProgressUpdate(vararg values: Int?) {}

        override fun onPostExecute(appPackageJson: JSONObject?) {
            progressAppDetails.visibility = View.GONE
            txtAppDetail.visibility = View.VISIBLE
            if (appPackageJson != null) {
                CheckPrerequisitesAsyncTask().execute()
                cardExecutable.visibility = View.VISIBLE
                packageJson = appPackageJson
                setAppDetails()
            } else {
                txtAppDetail.text = resources.getString(R.string.incompatible_package_app_detail)
            }
        }
    }

    private inner class CheckPrerequisitesAsyncTask : AsyncTask<String, Int, Boolean>() {
        override fun doInBackground(vararg argv: String): Boolean {
            val entries = fileSystem.getZipEntries(uri)
            zipEntries = entries?.a
            zipEntriesNodeModules = entries?.b
            return AgentRules.checkRequiredFiles(zipEntries!!)
        }

        override fun onProgressUpdate(vararg values: Int?) {}

        override fun onPostExecute(success: Boolean) {
            progressExecutable.visibility = View.GONE
            if (success) {
                btnExtract.isEnabled = true
            } else {
                txtExecutable.visibility = View.VISIBLE
                txtExecutable.text = resources.getString(R.string.not_executable_app_detail)
            }
        }
    }

    inner class ExtractPackageAsyncTask : AsyncTask<String, Int, Boolean>() {
        override fun onPreExecute() {
            super.onPreExecute()
            progressBarExtract.max = zipEntries!!.size + zipEntriesNodeModules!!.size
            progressBarExtract.visibility = View.VISIBLE
            appName = editTxtAppName.text.toString()
        }

        override fun doInBackground(vararg argv: String): Boolean {
            val appsDir = appConfig.get(AppConstant.KEY_APPS_DIR)
            val targetDirectory = File(appsDir)

            return fileSystem.unzipByIntent(uri, targetDirectory, appName, zipRootDirName, this)
        }

        /**
         * Make `publishProgress` public
         */
        fun publishProgressCallBack(vararg values: Int?) {
            this.publishProgress(*values)
        }

        override fun onProgressUpdate(vararg values: Int?) {
            progressBarExtract.progress = values[0]!!
        }

        override fun onPostExecute(success: Boolean) {
            progressBarExtract.visibility = View.GONE
            if (success) {
                btnCancel.visibility = View.GONE
                btnOpen.visibility = View.VISIBLE
            } else {
                txtExecutable.visibility = View.VISIBLE
                txtExecutable.text = resources.getString(R.string.extraction_failed_app_detail)
                btnExtract.isEnabled = true
                editTxtAppName.isEnabled = true
            }
        }
    }
}
