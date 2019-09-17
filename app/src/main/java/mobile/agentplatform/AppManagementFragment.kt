package mobile.agentplatform

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.FileProvider
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import kotlinx.android.synthetic.main.fragment_app_management.*
import java.io.File


class AppManagementFragment : Fragment(), AdapterView.OnItemClickListener, DrawerFragmentInterface {
    private var listFiles: MutableList<AppFile> = mutableListOf()
    private lateinit var fileManager: FileManager
    private lateinit var appConfig: AppConfig
    private lateinit var arrayAdapter: ArrayAdapter<AppFile>

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
    }

    override fun onResume() {
        super.onResume()

        textView.visibility = View.GONE

        listFiles.clear()
        val appsDir = File(appConfig.get(AppConstant.KEY_APPS_DIR))
        if (appsDir.exists()) {
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

    override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        openDialog(listFiles[position])
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
                    AppDialogOptions.Delete.name
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
                                // TODO(Delete the agent application)
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
        Open, Package, Send, Reset, Delete
    }

    private fun openApp(appPath: File) {
        val agentManager = AgentManager(context!!)
        agentManager.openApp(appPath)
    }
}
