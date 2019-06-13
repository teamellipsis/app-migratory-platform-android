package mobile.agentplatform

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.view.View
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), ActivityCompat.OnRequestPermissionsResultCallback {

    var fileContent: String? = null
    var projectPath: String? = null
    var fileSystem: FileSystem? = null
    var nodeThread: Thread? = null
    var projectThread: Thread? = null
    var webViewUrl: String? = null
    var PERMISSIONS_WRITE_EXTERNAL_STORAGE = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sample_text.text = stringFromJNI()
        getPermission()

        fileSystem = FileSystem()
    }

    fun loadContent(view: View) {
        try {
            fileContent = fileSystem?.getFileContent(filePath.text.toString())
            printLog(fileContent)
        } catch (e: Throwable) {
            printLog("Error: " + e.message)
        }

    }

    fun eval(view: View) {
        if (nodeThread == null || nodeThread?.isAlive() == false) {
            nodeThread = Thread(Runnable {
                startNodeWithArguments("node", "-e", fileContent!!)
            })
            nodeThread?.start()
            printLog("LOG: (eval) " + nodeThread?.name + " up")
        } else {
            printLog("LOG: (eval) " + nodeThread?.name + " busy")
        }
    }

    fun killNode(view: View) {
        if (nodeThread?.isAlive() == true) {
            nodeThread?.interrupt()
            printLog("LOG: (killNode) " + nodeThread?.name + " alive")
        } else {
            printLog("LOG: (killNode) " + nodeThread?.name + " not alive")
        }
    }

    fun loadProject(view: View) {
        projectPath = fileSystem?.getExternalStorageDir() + filePath.text.toString()
        printLog("LOG: (loadProject) " + projectPath)
    }

    fun evalProject(view: View) {
        if (projectThread == null || projectThread?.isAlive() == false) {
            projectThread = Thread(Runnable {
                startNodeWithArguments("node", projectPath!!)
            })
            projectThread?.start()
            printLog("LOG: (evalProject) " + projectThread?.name + " up")
        } else {
            printLog("LOG: (evalProject) " + projectThread?.name + " busy")
        }
    }

    fun killThread(view: View) {
        if (projectThread?.isAlive() == true) {
            projectThread?.interrupt()
            printLog("LOG: (killThread) " + projectThread?.name + " alive")
        } else {
            printLog("LOG: (killThread) " + projectThread?.name + " not alive")
        }
    }

    fun loadAgentActivitye(view: View) {
        val intent = Intent(applicationContext, AgentActivity::class.java).apply {
            putExtra("WEB_VIEW_URL", webViewUrl)
        }
        startActivity(intent)
    }

    fun assignUrl(view: View) {
        webViewUrl = url.text.toString()
        printLog("URL: " + webViewUrl)
    }

    fun printLog(text: String?) {
        var temp = sample_text.text.toString()
        temp += "\n\n" + text
        sample_text.text = temp
    }

    fun getPermission() {
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
            printLog("LOG: (getPermission) PERMISSION_GRANTED")
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == PERMISSIONS_WRITE_EXTERNAL_STORAGE) {
            if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                printLog("LOG: (onRequestPermissionsResult) PERMISSION_GRANTED")
            } else {
                printLog("LOG: (onRequestPermissionsResult) PERMISSION_DENIED")
            }
        }
    }

    external fun stringFromJNI(): String

    external fun startNodeWithArguments(vararg argv: String): Integer

    companion object {
        init {
            System.loadLibrary("native-lib")
            System.loadLibrary("node")
        }
    }
}
