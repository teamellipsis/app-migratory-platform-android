package mobile.agentplatform

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.Window
import kotlinx.android.synthetic.main.activity_agent.*
import android.content.DialogInterface
import android.support.v7.app.AlertDialog
import android.webkit.WebView
import android.util.Log
import android.webkit.WebChromeClient
import java.util.*


class AgentActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        getSupportActionBar()?.hide()
        setContentView(R.layout.activity_agent)

        val url = intent.getStringExtra("WEB_VIEW_URL")
        webView.settings.javaScriptEnabled = true

        webView.webChromeClient = object : WebChromeClient() {

            override fun onProgressChanged(view: WebView, progress: Int) {
                if (progress == 0) {
                    val date = Date()
                    val time = date.getTime()
                    Log.i("App-Migratory-Platform","start: " + time.toString())
                }
                if (progress == 100) {
                    val date = Date()
                    val time = date.getTime()
                    Log.i("App-Migratory-Platform","stop: " + time.toString())
                }
            }
        }

        webView.loadUrl(url)
    }

    override fun onBackPressed() {
//        super.onBackPressed()
        val alertDialog = AlertDialog.Builder(this).create()
        alertDialog.setTitle("Alert")
        alertDialog.setMessage("Do you want to exit?")
        alertDialog.setButton(
            AlertDialog.BUTTON_POSITIVE, "No",
            DialogInterface.OnClickListener { dialog, which -> dialog.dismiss() }
        )
        alertDialog.setButton(
            AlertDialog.BUTTON_NEGATIVE, "Yes",
            DialogInterface.OnClickListener { dialog, which -> super.onBackPressed() }
        )
        alertDialog.show()
    }
}
