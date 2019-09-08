package mobile.agentplatform

import android.content.Context
import android.content.Intent
import java.io.File

class AgentManager(context: Context) {

    private var applicationContext: Context = context

    fun openApp(appPath: File) {
        try {
            var projectThread = Thread(Runnable {
                val nativeClient = NativeClient()
                nativeClient.startNodeWithArgs("node", appPath.absolutePath + "/server.js")
            })
            projectThread.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val webViewUrl = "http://localhost:3000"  // TODO(need to get through Node.js process)
        val intent = Intent(applicationContext, AgentActivity::class.java).apply {
            putExtra("WEB_VIEW_URL", webViewUrl)
            putExtra("APP_PATH", appPath.absolutePath)
            addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
            addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
        }
        applicationContext.startActivity(intent)
    }
}
