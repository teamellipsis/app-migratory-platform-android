package mobile.agentplatform

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import kotlinx.android.synthetic.main.activity_qr_view.*
import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import android.widget.Toast


class QrViewActivity : AppCompatActivity() {
    private var white = 0xFFFFFFFF
    private var black = 0xFF000000
    private var encodedCode: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        if (intent.getBooleanExtra("SEND_FINISH", false)) {
            finish()
        }
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_view)

        supportActionBar?.setTitle(R.string.title_qr_view_activity)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val ipv4 = intent.getStringExtra("IPV4")
        val port = intent.getStringExtra("PORT")

        try {
            encodedCode = Encoder.encodeIpv4(ipv4, port)
            val bmp = encodeAsBitmap(encodedCode!!)
            qrCode.setImageBitmap(bmp)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, R.string.err_gen_qr_view_activity, Toast.LENGTH_LONG).show()
            finish()
        }
        encodedStr.text = encodedCode
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item!!.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun encodeAsBitmap(str: String): Bitmap {
        val result: BitMatrix
        var bitmap: Bitmap?
        val displayMetrics = resources.displayMetrics
        val width = displayMetrics.widthPixels
        result = MultiFormatWriter().encode(
            str,
            BarcodeFormat.QR_CODE, width, width, null
        )

        val w = result.width
        val h = result.height
        val pixels = IntArray(w * h)
        for (y in 0 until h) {
            val offset = y * w
            for (x in 0 until w) {
                pixels[offset + x] = if (result.get(x, y)) black.toInt() else white.toInt()
            }
        }
        bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        bitmap?.setPixels(pixels, 0, width, 0, 0, w, h)

        return bitmap
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        if (intent != null && intent.getBooleanExtra("SEND_FINISH", false)) {
            finish()
        }
    }
}
