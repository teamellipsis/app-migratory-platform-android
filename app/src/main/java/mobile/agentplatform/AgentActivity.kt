package mobile.agentplatform

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.Window
import kotlinx.android.synthetic.main.activity_agent.*
import android.content.DialogInterface
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.AsyncTask
import android.support.v7.app.AlertDialog
import android.webkit.WebView
import android.view.View
import android.webkit.WebChromeClient
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.net.URL


class AgentActivity : AppCompatActivity() {

    private var webViewUrl: String? = null
    private var appPath: String? = null
    private var serverJson: JSONObject? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        supportActionBar?.hide()
        setContentView(R.layout.activity_agent)

        webView.visibility = View.GONE
        appPath = intent.getStringExtra("APP_PATH")
        imageView.setImageDrawable(Drawable.createFromPath("$appPath/splash_screen.png"))

        val serverJsonStr = intent.getStringExtra("SERVER_JSON_STR")
        if (serverJsonStr !== null) {
            try {
                serverJson = JSONObject(serverJsonStr)
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }
        webView.settings.javaScriptEnabled = true

        webView.webChromeClient = object : WebChromeClient() {

            override fun onProgressChanged(view: WebView, progress: Int) {
                progressBar.progress = progress
                if (progress == 100) {
                    imageView.visibility = View.GONE
                    webView.visibility = View.VISIBLE
                }
            }
        }

        ServerListeningAsyncTask().execute()
    }

    override fun onBackPressed() {
        val alertDialog = AlertDialog.Builder(this).create()
        alertDialog.setTitle("Alert")
        alertDialog.setMessage("Do you want to exit?")
        alertDialog.setButton(
            AlertDialog.BUTTON_POSITIVE, "No",
            DialogInterface.OnClickListener { dialog, which -> dialog.dismiss() }
        )
        alertDialog.setButton(
            AlertDialog.BUTTON_NEGATIVE, "Yes",
            DialogInterface.OnClickListener { dialog, which ->
                val intent = Intent(applicationContext, AppManagementActivity::class.java)
                startActivity(intent)
                super.onBackPressed()
            }
        )
        alertDialog.show()
    }

    private inner class ServerListeningAsyncTask : AsyncTask<URL, Int, Int>() {
        override fun doInBackground(vararg urls: URL): Int? {
            val oldTime = serverJson?.getString("time")
            var port: Int = -1

            while (port == -1) {
                var fileManager = FileManager(applicationContext)
                val serverFile = File(appPath, AppConstant.SERVER_JSON_FILE)
                val serverJsonStr = fileManager.getFileContent(serverFile)
                if (serverJsonStr !== null) {
                    try {
                        val server = JSONObject(serverJsonStr)
                        val newTime = server.getString("time")
                        if (oldTime == null) {
                            port = server.getJSONObject("server").getInt("port")
                        } else if (oldTime != newTime) {
                            port = server.getJSONObject("server").getInt("port")
                        }
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                }

                Thread.sleep(16) // Since, devices display 60 frames per second
            }

            return port
        }

        override fun onProgressUpdate(vararg values: Int?) {}

        override fun onPostExecute(port: Int?) {
            webViewUrl = "http://localhost:$port"
            webView.loadUrl(webViewUrl)
        }
    }
}
