package mobile.agentplatform

import android.content.Context
import android.content.DialogInterface
import android.net.LocalServerSocket
import android.net.LocalSocket
import android.net.LocalSocketAddress
import android.os.AsyncTask
import android.support.design.widget.TextInputLayout
import android.support.v7.app.AlertDialog
import android.text.InputType
import android.widget.EditText
import android.widget.Toast
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.lang.Exception
import java.util.*

class ReceivingAppAsyncTask(private var context: Context, private var encodedString: String) :
    AsyncTask<String, String, Pair<Boolean, String?>>() {

    private var alertDialog: AlertDialog? = null
    private var socketDir: File? = null
    private var pathToSock: String? = null
    private var serverSocket: LocalServerSocket? = null
    private var ipv4: String? = null
    private var port: Int? = null
    private var timestamp: String? = null
    private var nodeClient: LocalSocket? = null

    override fun onPreExecute() {
        super.onPreExecute()
        createAlertDialog()?.show()
    }

    override fun doInBackground(vararg params: String?): Pair<Boolean, String?> {
        lateinit var result: Pair<Boolean, String?>
        try {
            // Decode encoded string into ipv4 and port
            if (!decodeToIpv4()) {
                return Pair(true, getString(R.string.invalid_code_sharing_fragment))
            }
            publishProgress(getString(R.string.successfully_decoded_sharing_fragment))

            // Create directory for store socket file
            if (!createSockDir()) {
                return Pair(true, getString(R.string.sock_dir_create_failed_sharing_fragment))
            }
            publishProgress(getString(R.string.created_sock_dir_sharing_fragment))

            // Create server socket
            if (!createServerSocket()) {
                serverSocket?.close()
                return Pair(true, getString(R.string.server_socket_create_failed_sharing_fragment))
            }
            publishProgress(getString(R.string.created_server_socket_sharing_fragment))

            // Initiate Node.js process
            if (createNodeClient() == -1) {
                return Pair(true, getString(R.string.node_process_create_failed_sharing_fragment))
            }
            publishProgress(getString(R.string.init_node_sharing_fragment))

            result = Pair(false, getString(R.string.cancel_receiving_sharing_fragment))

            // Handle server connection
            nodeClient = serverSocket?.accept()
            publishProgress(getString(R.string.node_connected_sharing_fragment))
            nodeClient?.sendBufferSize = 1024
            nodeClient?.receiveBufferSize = 1024

            val inputReader = InputStreamReader(nodeClient?.inputStream)
            val buffReader = BufferedReader(inputReader)
            var line: String? = null
            while ({ line = buffReader.readLine(); line }() != null) {
                if (line?.contains("message")!!) {
                    handleClientMessage(line!!)
                }

                if (line == MSG_LOCAL_EXTRACT_FINISH) {
                    result = Pair(false, getString(R.string.successfully_received_sharing_fragment))
                }
            }

        } catch (e: Exception) {
            result = Pair(true, getString(R.string.generic_error_sharing_fragment))
        } finally {
            serverSocket?.close()
            return result
        }
    }

    override fun onProgressUpdate(vararg progress: String) {
        if (progress[0] == MSG_LOCAL_ROOT_DIR_NAME) {
            val alertDialog: AlertDialog? = this.let {
                val builder = AlertDialog.Builder(context)

                val input = EditText(context)
                input.inputType = InputType.TYPE_CLASS_TEXT
                input.hint = "Enter app name"
                input.setText(progress[1])

                val textInputLayout = TextInputLayout(context)
                textInputLayout.setPadding(30, 35, 30, 0)
                textInputLayout.addView(input)

                builder.apply {
                    setCancelable(false)
                    setView(textInputLayout)
                    setPositiveButton("Ok", DialogInterface.OnClickListener { dialog, _ ->
                        val json = JSONObject()
                        json.put("message", MSG_LOCAL_APP_NAME)
                        json.put("appName", input.text.toString())
                        nodeClient?.outputStream?.write(json.toString().toByteArray())
                        nodeClient?.outputStream?.flush()
                        dialog.dismiss()
                    })
                    setNegativeButton("Cancel", DialogInterface.OnClickListener { dialog, _ ->
                        val json = JSONObject()
                        json.put("message", MSG_LOCAL_CANCEL)
                        nodeClient?.outputStream?.write(json.toString().toByteArray())
                        nodeClient?.outputStream?.flush()
                        dialog.cancel()
                    })
                }
                builder.create()
            }
            alertDialog?.show()
        } else {
            Toast.makeText(context, progress[0], Toast.LENGTH_LONG).show()
        }
    }

    override fun onPostExecute(result: Pair<Boolean, String?>) {
        var prefix = ""
        if (result.a) {
            prefix = "Error: "
        }
        Toast.makeText(context, prefix + result.b, Toast.LENGTH_LONG).show()
        alertDialog?.dismiss()
    }

    private fun getString(id: Int): String? {
        return context.resources.getString(id)
    }

    private fun createAlertDialog(): AlertDialog? {
        alertDialog = this.let {
            val builder = AlertDialog.Builder(context!!)
            builder.apply {
                setCancelable(false)
            }
            builder.create()
        }
        return alertDialog
    }

    private fun decodeToIpv4(): Boolean {
        val ipPortPair = Encoder.decodeIpv4(encodedString) ?: return false
        ipv4 = ipPortPair?.a
        port = ipPortPair?.b

        return true
    }

    private fun createSockDir(): Boolean {
        return try {
            socketDir = File(context!!.filesDir.absoluteFile, "platform/sock")
            socketDir?.mkdirs()
            true
        } catch (e: SecurityException) {
            false
        }
    }

    private fun createServerSocket(): Boolean {
        timestamp = Date().time.toString()
        pathToSock = "${socketDir?.absolutePath}/$timestamp.sock"
        val localSocketAddress = LocalSocketAddress(pathToSock, LocalSocketAddress.Namespace.FILESYSTEM)

        return try {
            val socket = LocalSocket()
            socket.bind(localSocketAddress)

            serverSocket = LocalServerSocket(socket.fileDescriptor)
            true
        } catch (e: IOException) {
            false
        }
    }

    private fun createNodeClient(): Int {
        val nativeClient = NativeClient()
        val appConfig = AppConfig(context!!)
        val pathToNodeModules = appConfig.get(AppConstant.KEY_NODE_MODULES_DIR)
        val pathToApps = appConfig.get(AppConstant.KEY_APPS_DIR)
        val pathToPackages = appConfig.get(AppConstant.KEY_PACKAGES_DIR)
        val logsDir = File(appConfig.get(AppConstant.KEY_LOGS_DIR))
        logsDir.mkdirs()
        val pathToLogs = logsDir.absolutePath + "/receive-$timestamp.log"

        return nativeClient.startNodeWithArgs(
            "node",
            "-e",
            SCRIPT,
            pathToSock!!,
            pathToNodeModules,
            pathToLogs,
            ipv4.toString(),
            port.toString(),
            pathToApps,
            pathToPackages
        )
    }

    private fun handleClientMessage(message: String) {
        try {
            val msg = JSONObject(message)
            if (msg.getString("message") == MSG_LOCAL_ROOT_DIR_NAME) {

                publishProgress(MSG_LOCAL_ROOT_DIR_NAME, msg.getString("appName"))
            }
        } catch (e: Exception) {
            throw e
        }
    }

    companion object {
        private const val MSG_REMOTE_CONNECTED = "remote-connected"
        private const val MSG_REMOTE_REQUESTED = "remote-requested"
        private const val MSG_REMOTE_RECEIVE_INIT = "remote-receive-init"
        private const val MSG_REMOTE_RECEIVE_FINISH = "remote-receive-finish"
        private const val MSG_REMOTE_SEND_ACK = "remote-send-ack"
        private const val MSG_REMOTE_DISCONNECT = "remote-disconnect"
        private const val MSG_LOCAL_ROOT_DIR_NAME = "root-dir-name"
        private const val MSG_LOCAL_APP_NAME = "app-name"
        private const val MSG_LOCAL_CANCEL = "cancel"
        private const val MSG_LOCAL_EXTRACT_FINISH = "extract-finish"
        private const val SCRIPT =
            """
                const pathToSock = process.argv[1];
                const pathToNodeModules = process.argv[2];
                const pathToLogs = process.argv[3];
                const ipv4 = process.argv[4];
                const port = process.argv[5];
                const pathToApps = process.argv[6];
                const pathToPackages = process.argv[7];

                const fs = require('fs');
                const path = require('path');
                const util = require('util');
                const logFile = fs.createWriteStream(pathToLogs, { flags: 'a', autoClose: true });

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
                let zip = null;
                let rootDirName = null;

                const client = net.createConnection(pathToSock, () => {
                    console.log('connected to IPC server');
                });
                client.on('data', (data) => {
                    const msg = JSON.parse(data.toString());
                    console.log(msg);
                    const { message } = msg;
                    if (message == "$MSG_LOCAL_CANCEL") {
                        clientEnd();
                    } else if(message == "$MSG_LOCAL_APP_NAME") {
                        const { appName } = msg;
                        if (appName !== rootDirName) {
                            zip.getEntries().forEach((entity) => {
                                entity.entryName = entity.entryName.replace(rootDirName, appName);
                            });
                        }

                        zip.extractAllTo(pathToApps, true);
                        client.write("$MSG_LOCAL_EXTRACT_FINISH\r\n");
                        console.log("$MSG_LOCAL_EXTRACT_FINISH");
                        clientEnd();
                    }
                });

                function clientEnd() {
                    client.end(() => {
                        console.log('disconnected from IPC server\n\n');
                        process.exit();
                    });
                }

                const remote = 'http://' + ipv4 + ':' + port;
                const remoteSocketClient = require('socket.io-client')(remote);

                remoteSocketClient.on('connect', () => {
                    console.log('connect to:' + remote);
                    client.write("$MSG_REMOTE_CONNECTED\r\n");
                    console.log("$MSG_REMOTE_CONNECTED");
                    remoteSocketClient.emit('request');
                    client.write("$MSG_REMOTE_REQUESTED\r\n");
                    console.log("$MSG_REMOTE_REQUESTED");
                });
                remoteSocketClient.on('send', (data, ack) => {
                    client.write("$MSG_REMOTE_RECEIVE_INIT\r\n");
                    console.log("$MSG_REMOTE_RECEIVE_INIT");
                    zip = new AdmZip(data);
                    client.write("$MSG_REMOTE_RECEIVE_FINISH\r\n");
                    console.log("$MSG_REMOTE_RECEIVE_FINISH");
                    const entity = zip.getEntries()[0];
                    rootDirName = entity.entryName.replace(/\/.*${'$'}/, "");
                    const msg = { message: "$MSG_LOCAL_ROOT_DIR_NAME", appName: rootDirName };
                    client.write(JSON.stringify(msg) + "\r\n");
                    console.log(msg);
                    if (ack) {
                        ack();
                        client.write("$MSG_REMOTE_SEND_ACK\r\n");
                        console.log("$MSG_REMOTE_SEND_ACK");
                    }
                    remoteSocketClient.close()
                });
                remoteSocketClient.on('disconnect', () => {
                    client.write("$MSG_REMOTE_DISCONNECT\r\n");
                    console.log('disconnect remote');
                });
            """
    }
}
