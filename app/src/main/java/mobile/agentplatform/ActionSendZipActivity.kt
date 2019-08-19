package mobile.agentplatform

import android.content.Intent
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import java.io.File

class ActionSendZipActivity : AppCompatActivity() {

    lateinit var fileSystem: FileSystem
    lateinit var uri: Uri

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_action_send_zip)

//        Log.i("App-Migratory-Platform", "here: " + intent.dataString)
//        Log.i("App-Migratory-Platform", "here: " + intent.action)
//        Log.i("App-Migratory-Platform", "intent: " + intent.type)
//        Log.i("App-Migratory-Platform", "intent: " + intent.getParcelableExtra(Intent.EXTRA_STREAM))
//        Log.i("App-Migratory-Platform", "intent: " + intent.getStringExtra(Intent.EXTRA_TEXT))

        uri = if(intent.action == Intent.ACTION_VIEW) {
            intent.data
        } else {
            intent.getParcelableExtra(Intent.EXTRA_STREAM) as Uri
        }

//        var file = File(uri.path)

        fileSystem = FileSystem(applicationContext)
    }

    fun unzip(view: View) {
        val targetDirectory = File(fileSystem.getExternalStorageDir() + "/FYP/new/1")

        fileSystem.unzipByIntent(uri, targetDirectory)
    }

    fun delete(view: View) {
        this.contentResolver.delete(uri,null,null)
    }
}
