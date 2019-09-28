package mobile.agentplatform

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.os.FileObserver
import android.support.v4.app.Fragment
import android.support.v4.content.FileProvider
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import kotlinx.android.synthetic.main.fragment_app_management.*
import java.io.File
import java.lang.Exception


class AppManagementFragment : Fragment(), AdapterView.OnItemClickListener, DrawerFragmentInterface {
    private var listFiles: MutableList<AppFile> = mutableListOf()
    private lateinit var fileManager: FileManager
    private lateinit var appConfig: AppConfig
    private lateinit var arrayAdapter: ArrayAdapter<AppFile>
    private lateinit var observer: FileObserver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fileManager = FileManager(context?.applicationContext!!)
        appConfig = AppConfig(context?.applicationContext!!)

        arrayAdapter = ArrayAdapter(context, R.layout.app_list_item, R.id.listItemText, listFiles)
        arrayAdapter.setNotifyOnChange(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_app_management, container, false)
    }

    override fun onStart() {
        super.onStart()
        listView.onItemClickListener = this

        val appsPath = appConfig.get(AppConstant.KEY_APPS_DIR)
        observer = object : FileObserver(appsPath) {
            override fun onEvent(event: Int, path: String?) {
                if (path != null) {
                    refreshAppList()
                }
            }
        }
        observer.startWatching()
    }

    override fun onResume() {
        super.onResume()
        refreshAppList()
    }

    override fun onDestroy() {
        super.onDestroy()

        if (::observer.isInitialized) {
            observer.stopWatching()
        }
    }

    override fun onPause() {
        super.onPause()

        if (::observer.isInitialized) {
            observer.stopWatching()
        }
    }

    override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        openDialog(listFiles[position])
    }

    private fun refreshAppList() {
        this.activity?.runOnUiThread {
            textView.visibility = View.GONE

            listFiles.clear()
            val appsDir = File(appConfig.get(AppConstant.KEY_APPS_DIR))
            if (appsDir.exists() && appsDir.listFiles() != null) {
                for (file in appsDir.listFiles()) {
                    listFiles.add(AppFile(file))
                }
            }

            if (listFiles.isEmpty()) {
                textView.text = resources.getString(R.string.no_apps_found_app_management_fragment)
                textView.visibility = View.VISIBLE
            }

            listView.adapter = arrayAdapter
        }
    }

    private fun openDialog(appPath: File) {
        val alertDialog: AlertDialog? = this.let {
            val builder = AlertDialog.Builder(context!!)
            builder.apply {
                setTitle(appPath.name)
                setItems(arrayOf(
                    AppDialogOptions.Open.name,
                    AppDialogOptions.Package.name,
                    AppDialogOptions.Send.name,
                    AppDialogOptions.Reset.name,
                    AppDialogOptions.Delete.name,
                    "View Logs"
                ),
                    DialogInterface.OnClickListener { dialog, which ->
                        when (which) {
                            AppDialogOptions.Open.ordinal -> {
                                openApp(appPath)
                            }
                            AppDialogOptions.Package.ordinal -> {
                                // TODO(Packaging should move to async task)
                                val packagesDir = File(appConfig.get(AppConstant.KEY_PACKAGES_DIR))
                                packagesDir.mkdirs()
                                fileManager.zipDir(appPath, File(packagesDir, appPath.name + ".zip"))
                            }
                            AppDialogOptions.Send.ordinal -> {
                                // TODO(Check package exist before send)
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

                                if (sendIntent.resolveActivity(context.packageManager) != null) {
                                    startActivity(sendIntent)
                                }
                            }
                            AppDialogOptions.Reset.ordinal -> {
                                // TODO(Reset the agent application)
                            }
                            AppDialogOptions.Delete.ordinal -> {
                                deleteApp(appPath)
                            }
                            AppDialogOptions.Log.ordinal -> {
                                viewAppLogs(appPath)
                            }
                            else -> {
                            }
                        }
                    })
            }
            builder.create()
        }
        alertDialog?.show()
    }

    enum class AppDialogOptions {
        Open, Package, Send, Reset, Delete, Log
    }

    private fun openApp(appPath: File) {
        val agentManager = AgentManager(context!!)
        agentManager.openApp(appPath)
    }

    private fun deleteApp(appPath: File) {
        val alertDialog = AlertDialog.Builder(context!!).create()
        alertDialog.setTitle("Delete")
        alertDialog.setMessage("Do you want to delete ${appPath.name}?")
        alertDialog.setButton(
            AlertDialog.BUTTON_POSITIVE, "No",
            DialogInterface.OnClickListener { dialog, _ -> dialog.dismiss() }
        )
        alertDialog.setButton(
            AlertDialog.BUTTON_NEGATIVE, "Yes",
            DialogInterface.OnClickListener { dialog, _ ->
                val del = appPath.deleteRecursively()
                if (del) {
                    Toast.makeText(context, R.string.success_delete_app_management_fragment, Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, R.string.failed_delete_app_management_fragment, Toast.LENGTH_LONG).show()
                }
                dialog.dismiss()
            }
        )
        alertDialog.show()
    }

    private fun viewAppLogs(appPath: File) {
        try {
            val file = File(appPath, "debug.log")

            val fileUri = FileProvider.getUriForFile(
                context!!,
                context?.packageName + ".provider",
                file
            )

            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                putExtra(Intent.EXTRA_STREAM, fileUri)
                type = "text/plain"
            }
            startActivity(sendIntent)
        } catch (e: IllegalArgumentException) {
            Toast.makeText(context, R.string.no_logs_app_management_fragment, Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(context, R.string.error_view_logs_app_management_fragment, Toast.LENGTH_LONG).show()
        }
    }
}
