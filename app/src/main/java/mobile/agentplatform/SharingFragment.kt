package mobile.agentplatform

import android.Manifest
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.LocalSocket
import android.net.LocalSocketAddress
import android.os.Bundle
import android.os.FileObserver
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
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
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
        // Init Node.js process with required args (path_to_pid_dir, path_to_node_modules, path_to_logs)
        val nativeClient = NativeClient()
        val script =
            """
                const pathToSockDir = process.argv[1];
                const pathToNodeModules = process.argv[2];
                const pathToLogs = process.argv[3];

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

                const pathToSock = path.join(pathToSockDir, process.pid.toString());

                const net = require('net');

                const server = net.createServer((socket) => {
                    socket.end('goodbye\n', () => {
                        console.log('goodbye\n');
                    });
                }).on('error', (err) => {
                    console.log("Server error");
                    console.log(err);
                });

                server.on('connection', (socket) => {
                    console.log("Server connection");
                    console.log(socket.bufferSize);
                    socket.on('data', (data) => {
                        console.log(data);
                    })
                });

                function closeServer() {
                    server.close((err) => {
                        if (err) {
                            console.log("close error:");
                            console.log(err);
                        }
                    });
                }

                process.on('exit', (code) => {
                    closeServer();
                    console.log("exit: ", code);
                });

                server.listen(pathToSock, () => {
                    console.log('opened server on', server.address());
                });
            """

        val appConfig = AppConfig(context!!)
        val pathToNodeModules = appConfig.get(AppConstant.KEY_NODE_MODULES_DIR)
        val timestamp = Date().time.toString()
        val logsDir = File(appConfig.get(AppConstant.KEY_LOGS_DIR))
        logsDir.mkdirs()
        val pathToLogs = logsDir.absolutePath + "/receive-$timestamp.log"

        val childProcessPid = nativeClient.startNodeWithArgs(
            "node",
            "-e",
            script,
            socketDir.absolutePath,
            pathToNodeModules,
            pathToLogs
        )

        // Wait for Node.js create socket file
        val observer = object : FileObserver(socketDir.absolutePath, FileObserver.CREATE) {
            override fun onEvent(event: Int, path: String?) {
                if (childProcessPid.toString() == path) {
                    stopWatching()
                    // Connect local client to Node.js socket
                    connectClient(socketDir, childProcessPid)
                }
            }
        }
        observer.startWatching()
    }

    fun connectClient(socketDir: File, pid: Int) {
        val pathToSock = "${socketDir.absolutePath}/$pid"
        val localSocketAddress = LocalSocketAddress(pathToSock, LocalSocketAddress.Namespace.FILESYSTEM)

        val client = LocalSocket()
        client.connect(localSocketAddress)
        client.receiveBufferSize = 2048
        client.soTimeout = 3000
        var stream = FileInputStream(client.fileDescriptor)
        val inputReader = InputStreamReader(stream)
        val bufferReader = BufferedReader(inputReader)
        // Todo(Read the buffer)
    }

    companion object {
        var qrScanResult: Result? = null
        private const val SCAN_QR_CODE = "Scan QR code"
        private const val TYPE_CODE = "Type code"
        private const val PERMISSIONS_CAMERA = 0
    }
}
