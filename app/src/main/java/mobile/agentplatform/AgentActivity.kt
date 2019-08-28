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
import java.lang.Exception
import java.net.HttpURLConnection
import java.net.URL


class AgentActivity : AppCompatActivity() {

    private var webViewUrl: String? = null
    private var appPath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        supportActionBar?.hide()
        setContentView(R.layout.activity_agent)

        webView.visibility = View.GONE
        appPath = intent.getStringExtra("APP_PATH")
        imageView.setImageDrawable(Drawable.createFromPath("$appPath/splash_screen.png"))

        webViewUrl = intent.getStringExtra("WEB_VIEW_URL")
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

        HttpAsyncTask().execute()
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

    private inner class HttpAsyncTask : AsyncTask<URL, Int, Int>() {
        override fun doInBackground(vararg urls: URL): Int? {
            var code: Int? = null
            val url = URL("$webViewUrl/${AppConstant.PING_ENDPOINT}")

            while (true) {
                val urlConnection = url.openConnection() as HttpURLConnection
                try {
                    code = urlConnection.responseCode

                } catch (e: Exception) {
//                    e.printStackTrace()
                } finally {
                    urlConnection.disconnect()
                }

                if (code == HttpURLConnection.HTTP_OK) {
                    break
                }

                Thread.sleep(16) // Since, devices display 60 frames per second
            }

            return code
        }

        override fun onProgressUpdate(vararg values: Int?) {}

        override fun onPostExecute(result: Int?) {
            webView.loadUrl(webViewUrl)
        }
    }
}
