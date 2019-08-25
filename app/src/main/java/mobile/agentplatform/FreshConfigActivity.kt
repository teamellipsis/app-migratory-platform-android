package mobile.agentplatform

import android.Manifest
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.os.AsyncTask
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.view.View
import android.widget.RadioButton
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_fresh_config.*
import java.io.File

class FreshConfigActivity : AppCompatActivity(), ActivityCompat.OnRequestPermissionsResultCallback,
    DirectoryChooserFragment.OnFragmentInteractionListener {

    lateinit var fileSystem: FileSystem
    lateinit var appConfig: AppConfig

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fresh_config)

        fileSystem = FileSystem(applicationContext)
        appConfig = AppConfig(applicationContext)
    }

    fun setDefaultLocation(view: View) {
        val radioBtnText = findViewById<RadioButton>(radioGroupLoc.checkedRadioButtonId).text.toString()

        if (radioBtnText == resources.getString(R.string.radio1_activity_fresh_config)) {
            appConfig.set(AppConstant.KEY_WORKING_DIR_TEMP, fileSystem.getFileDir())
            viewConfirmation.visibility = View.VISIBLE
            viewSelectLoc.visibility = View.GONE
        } else {
            getPermission()
        }
    }

    fun proceedExtraction(view: View) {
        viewProgress.visibility = View.VISIBLE
        viewConfirmation.visibility = View.GONE

        checkFileWritePermission()
    }

    private fun checkFileWritePermission() {
        txtProgress.setText(R.string.checking_permission)

        val workingDir = appConfig.get(AppConstant.KEY_WORKING_DIR_TEMP)
        val file = File(workingDir)

        if (file.canWrite()) {
            copyNodeModules()
            dot1.setImageResource(R.drawable.ic_tick_mark_dark)
            printLog(R.string.file_write_permission_ok)
        } else {
            dot1.setImageResource(R.drawable.ic_cancel_dark)
            printLog(R.string.file_write_permission_fail)
        }
    }

    private fun copyNodeModules() {
        txtProgress.setText(R.string.copying_assets)
        loader1.visibility = View.VISIBLE
        dot2.visibility = View.GONE
        CopyAssetsAsyncTask().execute()
    }

    private fun unzipNodeModules() {
        txtProgress.setText(R.string.extracting_assets)
        loader2.visibility = View.VISIBLE
        dot3.visibility = View.GONE
        UnzipNodeModulesAsyncTask().execute()
    }

    private fun cleanFiles() {
        txtProgress.setText(R.string.cleaning_disposable)
        loader3.visibility = View.VISIBLE
        dot4.visibility = View.GONE
        CleanZipFileAsyncTask().execute()
    }

    private fun printLog(resId: Int) {
        var temp = txtLog.text.toString()
        temp += "\n\n" + resources.getString(resId)
        txtLog.text = temp
    }

    private inner class CopyAssetsAsyncTask : AsyncTask<String, Int, Boolean>() {
        override fun doInBackground(vararg argv: String): Boolean? {
            val workingDir = appConfig.get(AppConstant.KEY_WORKING_DIR_TEMP)
            return fileSystem.copyAssetsToWorkingDir(
                AppConstant.ASSETS_PARENT_NAME,
                AppConstant.NODE_MODULES_ZIP,
                workingDir
            )
        }

        override fun onProgressUpdate(vararg values: Int?) {}

        override fun onPostExecute(success: Boolean?) {
            if (success!!) {
                unzipNodeModules()
                dot2.setImageResource(R.drawable.ic_tick_mark_dark)
                printLog(R.string.copy_assets_ok)
            } else {
                dot2.setImageResource(R.drawable.ic_cancel_dark)
                printLog(R.string.copy_assets_fail)
                txtProgress.visibility = View.GONE
            }
            dot2.visibility = View.VISIBLE
            loader1.visibility = View.GONE
        }
    }

    private inner class UnzipNodeModulesAsyncTask : AsyncTask<String, Int, Boolean>() {
        override fun doInBackground(vararg argv: String): Boolean? {
            val workingDir = appConfig.get(AppConstant.KEY_WORKING_DIR_TEMP)
            val file = File(workingDir, AppConstant.NODE_MODULES_ZIP)
            val targetDirectory = File(workingDir)

            return fileSystem.unzip(file, targetDirectory)
        }

        override fun onProgressUpdate(vararg values: Int?) {}

        override fun onPostExecute(success: Boolean?) {
            if (success!!) {
                cleanFiles()
                dot3.setImageResource(R.drawable.ic_tick_mark_dark)
                printLog(R.string.extract_assets_ok)
                val workingDir = appConfig.get(AppConstant.KEY_WORKING_DIR_TEMP)
                appConfig.set(AppConstant.KEY_WORKING_DIR, workingDir)
            } else {
                dot3.setImageResource(R.drawable.ic_cancel_dark)
                printLog(R.string.extract_assets_fail)
                txtProgress.visibility = View.GONE
            }
            dot3.visibility = View.VISIBLE
            loader2.visibility = View.GONE
        }
    }

    private inner class CleanZipFileAsyncTask : AsyncTask<String, Int, Boolean>() {
        override fun doInBackground(vararg argv: String): Boolean? {
            val workingDir = appConfig.get(AppConstant.KEY_WORKING_DIR_TEMP)
            val file = File(workingDir, AppConstant.NODE_MODULES_ZIP)

            return fileSystem.deleteFile(file)
        }

        override fun onProgressUpdate(vararg values: Int?) {}

        override fun onPostExecute(success: Boolean?) {
            if (success!!) {
                dot4.setImageResource(R.drawable.ic_tick_mark_dark)
                printLog(R.string.clean_disposable_ok)
            } else {
                dot4.setImageResource(R.drawable.ic_cancel_dark)
                printLog(R.string.clean_disposable_fail)
            }
            dot4.visibility = View.VISIBLE
            loader3.visibility = View.GONE
            txtProgress.visibility = View.GONE
        }
    }

    override fun onBackPressed() {
        if (viewSelectLoc.visibility == View.VISIBLE) {
            val alertDialog = AlertDialog.Builder(this).create()
            alertDialog.setTitle("Do you want close the application?")
            alertDialog.setButton(
                AlertDialog.BUTTON_POSITIVE, "No",
                DialogInterface.OnClickListener { dialog, which -> dialog.dismiss() }
            )
            alertDialog.setButton(
                AlertDialog.BUTTON_NEGATIVE, "Yes",
                DialogInterface.OnClickListener { dialog, which -> super.onBackPressed() }
            )
            alertDialog.show()
        } else if (viewConfirmation.visibility == View.VISIBLE) {
            viewConfirmation.visibility = View.GONE
            viewSelectLoc.visibility = View.VISIBLE
        } else if (viewDirChooser.visibility == View.VISIBLE) {
            viewDirChooser.visibility = View.GONE
            viewSelectLoc.visibility = View.VISIBLE
        }
    }

    private fun getPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                PERMISSIONS_WRITE_EXTERNAL_STORAGE
            )
        } else {
            showDirChooser()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == PERMISSIONS_WRITE_EXTERNAL_STORAGE) {
            if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showDirChooser()
            } else {
                Toast.makeText(applicationContext, R.string.file_system_permission_denied, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showDirChooser() {
        viewSelectLoc.visibility = View.GONE
        viewDirChooser.visibility = View.VISIBLE
    }

    override fun onDirectoryChoose(file: File) {
        appConfig.set(AppConstant.KEY_WORKING_DIR_TEMP, file.absolutePath)
        viewConfirmation.visibility = View.VISIBLE
        viewDirChooser.visibility = View.GONE
    }

    companion object {
        private const val PERMISSIONS_WRITE_EXTERNAL_STORAGE = 0
    }
}
