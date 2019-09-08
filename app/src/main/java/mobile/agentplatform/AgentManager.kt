package mobile.agentplatform

import android.content.Context
import android.content.Intent
import java.io.File

class AgentManager(context: Context) {

    private var applicationContext: Context = context

    fun openApp(appPath: File) {
        var fileManager = FileManager(applicationContext)
        val serverFile = File(appPath,AppConstant.SERVER_JSON_FILE)
        val serverJsonStr = fileManager.getFileContent(serverFile)
        try {
            var projectThread = Thread(Runnable {
                val nativeClient = NativeClient()
                nativeClient.startNodeWithArgs("node", appPath.absolutePath + "/server.js")
            })
            projectThread.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val intent = Intent(applicationContext, AgentActivity::class.java).apply {
            putExtra("SERVER_JSON_STR", serverJsonStr)
            putExtra("APP_PATH", appPath.absolutePath)
            addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
            addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
        }
        applicationContext.startActivity(intent)
    }
}
