package mobile.agentplatform

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_agent.*

class AgentActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_agent)

        val url = intent.getStringExtra("WEB_VIEW_URL")
        webView.settings.javaScriptEnabled = true
        webView.loadUrl(url)
    }
}
