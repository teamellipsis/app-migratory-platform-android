package mobile.agentplatform

import android.Manifest
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.LocalServerSocket
import android.net.LocalSocket
import android.net.LocalSocketAddress
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
import java.io.*
import java.util.*


class SharingFragment : Fragment(), ActivityCompat.OnRequestPermissionsResultCallback, DrawerFragmentInterface {

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

    private fun openReceiveAppDialog() {
        val listItems = arrayOf(SCAN_QR_CODE, TYPE_CODE)
        val alertDialog: AlertDialog? = this.let {
            val builder = AlertDialog.Builder(context!!)
            builder.apply {
                setTitle("Receive app")
                setItems(listItems,
                    DialogInterface.OnClickListener { dialog, which ->
                        when (which) {
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

    private fun processAppReceiving(code: String) {
        val alertDialog: AlertDialog? = this.let {
            val builder = AlertDialog.Builder(context!!)
            builder.apply {
                setCancelable(false)
            }
            builder.create()
        }
        alertDialog?.show()
        val ipPortPair = Encoder.decodeIpv4(code)
        if (ipPortPair == null) {
            alertDialog?.dismiss()
            Toast.makeText(context, R.string.invalid_code_sharing_fragment, Toast.LENGTH_LONG).show()
            return
        }

        val ipv4 = ipPortPair?.a
        val port = ipPortPair?.b

        lateinit var socketDir: File
        try {
            socketDir = File(context!!.filesDir.absoluteFile, "platform/sock")
            socketDir.mkdirs()
        } catch (e: SecurityException) {
            alertDialog?.dismiss()
            Toast.makeText(context, R.string.pids_dir_create_failed_sharing_fragment, Toast.LENGTH_LONG).show()
            return
        }

        alertDialog?.dismiss()

        // Todo(Move into async task if needed)
        // Create local socket
        val timestamp = Date().time.toString()
        val pathToSock = "${socketDir.absolutePath}/$timestamp.sock"
        val localSocketAddress = LocalSocketAddress(pathToSock, LocalSocketAddress.Namespace.FILESYSTEM)

        val socket = LocalSocket()
        socket.bind(localSocketAddress)

        val server = LocalServerSocket(socket.fileDescriptor)

        // Init Node.js process with required args (path_to_pid_dir, path_to_node_modules, path_to_logs)
        val nativeClient = NativeClient()
        val script =
            """
                const pathToSock = process.argv[1];
                const pathToNodeModules = process.argv[2];
                const pathToLogs = process.argv[3];
                const ipv4 = process.argv[4];
                const port = process.argv[5];
                const pathToApps = process.argv[6];

                const fs = require('fs');
                const path = require('path');
                const util = require('util');
                const logFile = fs.createWriteStream(pathToLogs, { flags: 'a' });

                console.error = (msg, ...optionalParams) => {
                    logFile.write(util.format(msg, ...optionalParams) + '\n');
                    process.stderr.write(util.format(msg, ...optionalParams) + '\n');
                };
                console.warn = (msg, ...optionalParams) => {
                    logFile.write(util.format(msg, ...optionalParams) + '\n');
                    process.stdout.write(util.format(msg, ...optionalParams) + '\n');
                };
                console.info = (msg, ...optionalParams) => {
                    logFile.write(util.format(msg, ...optionalParams) + '\n');
                    process.stdout.write(util.format(msg, ...optionalParams) + '\n');
                };
                console.log = (msg, ...optionalParams) => {
                    logFile.write(util.format(msg, ...optionalParams) + '\n');
                    process.stdout.write(util.format(msg, ...optionalParams) + '\n');
                };
                console.debug = (msg, optionalParams) => {
                    logFile.write(util.format(msg, ...optionalParams) + '\n');
                    process.stdout.write(util.format(msg, ...optionalParams) + '\n');
                };

                module.paths.push(pathToNodeModules);

                process.on('uncaughtException', (err) => {
                    console.log('uncaughtException');
                    console.log(err);
                });

                const net = require('net');
                const AdmZip = require('adm-zip');

                const remote = 'http://' + ipv4 + ':' + port;
                const remoteSocketClient = require('socket.io-client')(remote);

                remoteSocketClient.on('connect', () => {
                    console.log('connect');
                    remoteSocketClient.emit('request');
                });
                remoteSocketClient.on('event', (data) => {
                    console.log('event');
                    console.log(data);
                });
                remoteSocketClient.on('send', (data, ack) => {
                    let zip = new AdmZip(data);
                    zip.extractAllTo(pathToApps, true);

                    if (ack) {
                        ack();
                    }
                });
                remoteSocketClient.on('disconnect', () => {
                    console.log('disconnect');
                });

                const client = net.createConnection(pathToSock, () => {
                    console.log('connected to server!');
                });
                client.on('data', (data) => {
                    console.log(data.toString());
                });
                client.on('end', () => {
                    console.log('disconnected from server\n\n');
                });
            """

        val appConfig = AppConfig(context!!)
        val pathToNodeModules = appConfig.get(AppConstant.KEY_NODE_MODULES_DIR)
        val pathToApps = appConfig.get(AppConstant.KEY_APPS_DIR)

        val logsDir = File(appConfig.get(AppConstant.KEY_LOGS_DIR))
        logsDir.mkdirs()
        val pathToLogs = logsDir.absolutePath + "/receive-$timestamp.log"

        val childProcessPid = nativeClient.startNodeWithArgs(
            "node",
            "-e",
            script,
            pathToSock,
            pathToNodeModules,
            pathToLogs,
            ipv4,
            port.toString(),
            pathToApps
        )

        var projectThread = Thread(Runnable {
            try {
                // Only one connection will accept
                val sender = server.accept()
                sender.sendBufferSize = 1024
                sender.receiveBufferSize = 1024

                val inputReader = InputStreamReader(sender.inputStream)
                val buffReader = BufferedReader(inputReader)
                // Read buffer
                server.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }

        })
        projectThread.start()
    }

    companion object {
        var qrScanResult: Result? = null
        private const val SCAN_QR_CODE = "Scan QR code"
        private const val TYPE_CODE = "Type code"
        private const val PERMISSIONS_CAMERA = 0
    }
}
