package mobile.agentplatform

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    var _startedNodeAlready = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sample_text.text = stringFromJNI()

        if (!_startedNodeAlready) {
            _startedNodeAlready = true
            Thread(Runnable {
                startNodeWithArguments("node", "-e", "var http = require('http'); " +
                        "var versions_server = http.createServer( (request, response) => { " +
                        "  response.end('Versions: ' + JSON.stringify(process.versions)); " +
                        "}); " +
                        "versions_server.listen(3000);"
                )
            }).start()
        }
    }

    external fun stringFromJNI(): String

    external fun startNodeWithArguments(vararg argv:String): Integer

    companion object {
        init {
            System.loadLibrary("native-lib")
            System.loadLibrary("node")
        }
    }
}
