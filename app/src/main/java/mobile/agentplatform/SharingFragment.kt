package mobile.agentplatform

import android.Manifest
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.design.widget.TextInputLayout
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import com.google.zxing.BarcodeFormat
import kotlinx.android.synthetic.main.fragment_sharing.*
import com.google.zxing.Result
import java.io.File


class SharingFragment : Fragment(), ActivityCompat.OnRequestPermissionsResultCallback, DrawerFragmentInterface {
    private var listener: MainFragmentInteractionListener? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_sharing, container, false)
    }

    override fun onStart() {
        super.onStart()
        btnReceive.setOnClickListener {
            openReceiveAppDialog()
        }

        btnSend.setOnClickListener {
            // Todo(implement send apps)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        if (intent?.getStringExtra("QR_RESULT") != null) {
            if (SharingFragment.qrScanResult?.barcodeFormat.toString() == BarcodeFormat.QR_CODE.name) {
                processAppReceiving(SharingFragment.qrScanResult?.text.toString())
            } else {
                Toast.makeText(context, R.string.invalid_code_type_sharing_fragment, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onFragmentChange(extra: String?) {
        if (extra == "SEND_APP") {
            SendingAppAsyncTask(context!!, appPath!!).execute()
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is MainFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException("$context must implement MainFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    private fun openReceiveAppDialog() {
        val listItems = arrayOf(RECEIVE_FROM_TRUSTED, SCAN_QR_CODE, TYPE_CODE)
        val alertDialog: AlertDialog? = this.let {
            val builder = AlertDialog.Builder(context!!)
            builder.apply {
                setTitle("Receive app")
                setItems(listItems,
                    DialogInterface.OnClickListener { dialog, which ->
                        when (which) {
                            listItems.indexOf(RECEIVE_FROM_TRUSTED) -> {
                                receiveAppFromTrusted()
                            }
                            listItems.indexOf(SCAN_QR_CODE) -> {
                                checkCameraPermissionAndOpenQrScanner()
                            }
                            listItems.indexOf(TYPE_CODE) -> {
                                openInputDialog()
                            }
                            else -> {
                            }
                        }
                    })
            }
            builder.create()
        }
        alertDialog?.show()
    }

    private fun checkCameraPermissionAndOpenQrScanner() {
        if (ContextCompat.checkSelfPermission(
                context!!,
                Manifest.permission.CAMERA
            )
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.CAMERA),
                PERMISSIONS_CAMERA
            )
        } else {
            openQrScanner()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == PERMISSIONS_CAMERA) {
            if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openQrScanner()
            } else {
                Toast.makeText(context, R.string.camera_per_denied_sharing_fragment, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun openQrScanner() {
        val intent = Intent(context, QrActivity::class.java)
        startActivity(intent)
    }

    private fun openInputDialog() {
        val alertDialog: AlertDialog? = this.let {
            val builder = AlertDialog.Builder(context!!)

            val input = EditText(context)
            input.inputType = InputType.TYPE_CLASS_TEXT
            input.hint = "Enter code here"

            val textInputLayout = TextInputLayout(context)
            textInputLayout.setPadding(30, 35, 30, 0)
            textInputLayout.addView(input)

            builder.apply {
                setView(textInputLayout)
                setPositiveButton("Ok", DialogInterface.OnClickListener { dialog, _ ->
                    processAppReceiving(input.text.toString())
                    dialog.dismiss()
                })
                setNegativeButton("Cancel", DialogInterface.OnClickListener { dialog, _ ->
                    dialog.cancel()
                })
            }
            builder.create()
        }
        alertDialog?.show()
    }

    private fun receiveAppFromTrusted() {
        ReceivingAppTrustedAsyncTask(context!!, listener!!).execute()
    }

    private fun processAppReceiving(code: String) {
        ReceivingAppAsyncTask(context!!, code, listener!!).execute()
    }

    companion object {
        var qrScanResult: Result? = null
        var appPath: File? = null
        private const val SCAN_QR_CODE = "Scan QR code"
        private const val TYPE_CODE = "Type code"
        private const val RECEIVE_FROM_TRUSTED = "Receive from trusted"
        private const val PERMISSIONS_CAMERA = 0
    }
}
