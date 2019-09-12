package mobile.agentplatform

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import kotlinx.android.synthetic.main.activity_app_management.*
import java.io.File
import android.support.v4.content.FileProvider
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AlertDialog
import android.view.MenuItem
import android.view.Window


class AppManagementActivity : AppCompatActivity(), AdapterView.OnItemClickListener,
    NavigationView.OnNavigationItemSelectedListener {
    private var listFiles: MutableList<AppFile> = mutableListOf()
    private lateinit var fileManager: FileManager
    private lateinit var appConfig: AppConfig
    lateinit var context: Context
    private lateinit var arrayAdapter: ArrayAdapter<AppFile>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        supportActionBar?.hide()
        setContentView(R.layout.activity_app_management)
        setSupportActionBar(appToolbar)

        val toggle = ActionBarDrawerToggle(
            this, drawer_layout, appToolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        nav_view.setNavigationItemSelectedListener(this)

        listView.onItemClickListener = this

        fileManager = FileManager(applicationContext)
        appConfig = AppConfig(applicationContext)

        if (appConfig.get(AppConstant.KEY_WORKING_DIR).isEmpty()) {
            val intent = Intent(applicationContext, FreshConfigActivity::class.java)
            startActivity(intent)
            finish()
        }

        context = this

        arrayAdapter = ArrayAdapter(this, R.layout.app_list_item, R.id.listItemText, listFiles)
        arrayAdapter.setNotifyOnChange(true)
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
            textView.text = resources.getString(R.string.no_apps_found_app_management_activity)
            textView.visibility = View.VISIBLE
        }

        listView.adapter = arrayAdapter
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_apps -> {
                val intent = Intent(this, AppManagementActivity::class.java)
                startActivity(intent)
            }
            R.id.nav_devices -> {

            }
            R.id.nav_share -> {

            }
        }

        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        openDialog(listFiles[position])
    }

    private fun openDialog(appPath: File) {
        val alertDialog: AlertDialog? = this.let {
            val builder = AlertDialog.Builder(it)
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
                                    applicationContext.packageName + ".provider",
                                    file
                                )

                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    putExtra(Intent.EXTRA_STREAM, fileUri)
                                    type = "application/zip"
                                }

                                if (sendIntent.resolveActivity(packageManager) != null) {
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
        val agentManager = AgentManager(applicationContext)
        agentManager.openApp(appPath)
    }
}
