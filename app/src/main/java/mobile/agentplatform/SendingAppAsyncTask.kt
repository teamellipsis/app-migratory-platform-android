package mobile.agentplatform

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.LocalServerSocket
import android.net.LocalSocket
import android.net.LocalSocketAddress
import android.os.AsyncTask
import android.support.v7.app.AlertDialog
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.lang.Exception
import java.util.*

class SendingAppAsyncTask(private val context: Context, private val appPath: File) :
    AsyncTask<String, String, Pair<Boolean, String?>>() {
    private var alertDialog: AlertDialog? = null
    private var socketDir: File? = null
    private var pathToSock: String? = null
    private var serverSocket: LocalServerSocket? = null
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

            result = Pair(false, getString(R.string.cancel_send_sharing_fragment))

            // Handle server connection
            nodeClient = serverSocket?.accept()
            publishProgress(getString(R.string.node_connected_sharing_fragment))
            nodeClient?.sendBufferSize = 1024
            nodeClient?.receiveBufferSize = 1024

            val inputReader = InputStreamReader(nodeClient?.inputStream)
            val buffReader = BufferedReader(inputReader)
            var msg: String? = null
            while ({ msg = buffReader.readLine(); msg }() != null) {
                if (msg?.contains("message")!!) {
                    if (JSONObject(msg).getString("message") == MSG_SERVER_ERROR) {
                        result = Pair(true, getString(R.string.error_send_server_send_sharing_fragment))
                        break
                    } else if (JSONObject(msg).getString("message") == MSG_SEND) {
                        result = Pair(false, getString(R.string.successfully_sent_sharing_fragment))
                        break
                    }
                    handleClientMessage(msg!!)
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
        if (progress[0] == MSG_CONNECTIONS) {
            val msg = JSONObject(progress[1])
            val connections = msg.getJSONArray("connections")
            selectNetworkInterface(connections)
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
        if (result.b == getString(R.string.successfully_sent_sharing_fragment)) {
            val intent = Intent(context, QrViewActivity::class.java).apply {
                putExtra("SEND_FINISH", true)
            }
            context.startActivity(intent)
        }
    }

    private fun createAlertDialog(): AlertDialog? {
        alertDialog = this.let {
            val builder = AlertDialog.Builder(context)
            builder.apply {
                setCancelable(false)
            }
            builder.create()
        }
        return alertDialog
    }

    private fun getString(id: Int): String? {
        return context.resources.getString(id)
    }

    private fun createSockDir(): Boolean {
        return try {
            socketDir = File(context.filesDir.absoluteFile, "platform/sock")
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
        val appConfig = AppConfig(context)
        val pathToNodeModules = appConfig.get(AppConstant.KEY_NODE_MODULES_DIR)
        val pathToApps = appConfig.get(AppConstant.KEY_APPS_DIR)
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
            appPath.absolutePath,
            appPath.name,
            pathToApps
        )
    }

    private fun handleClientMessage(message: String) {
        val msg = JSONObject(message)
        when (msg.getString("message")) {
            MSG_CONNECTIONS -> publishProgress(MSG_CONNECTIONS, message)
            MSG_SERVER_OK -> port = msg.getJSONObject("server").getInt("port")
        }
    }

    private fun selectNetworkInterface(connections: JSONArray) {
        val conns = mutableListOf<String>()
        for (i in 0 until connections.length()) {
            val conn = connections.getJSONObject(i)
            conns.add("${conn.getString("name")} (${conn.getString("ip")})\n${conn.getString("mac")}")
        }
        val alertDialog: AlertDialog? = this.let {
            val builder = AlertDialog.Builder(context)
            builder.apply {
                setTitle("Select network interface")
                setCancelable(false)
                setItems(conns.toTypedArray(),
                    DialogInterface.OnClickListener { dialog, which ->
                        val intent = Intent(context, QrViewActivity::class.java).apply {
                            putExtra("IPV4", connections.getJSONObject(which).getString("ip"))
                            putExtra("PORT", port.toString())
                        }
                        context.startActivity(intent)
                    })
            }
            builder.create()
        }
        alertDialog?.show()
    }

    companion object {
        private const val MSG_CONNECTIONS = "connections"
        private const val MSG_SEND = "send"
        private const val MSG_SERVER_ERROR = "server-error"
        private const val MSG_SERVER_OK = "server-listening"
        private const val MSG_CANCEL = "cancel"
        private const val SCRIPT =
            """
                const pathToSock = process.argv[1];
                const pathToNodeModules = process.argv[2];
                const pathToLogs = process.argv[3];
                const pathToApp = process.argv[4];
                const appName = process.argv[5];
                const pathToApps = process.argv[6];

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
                const server = require('http').createServer((req, res) => {
                    res.writeHead(403, { 'Content-Type': 'text/plain' });
                    res.end('Forbidden');
                });
                const io = require('socket.io')(server);
                const os = require('os');

                let zip = new AdmZip();
                zip.addLocalFolder(pathToApp, appName);

                io.on('connection', socket => {
                    console.log('connection');
                    socket.on('request', (args) => {
                        console.log('request');
                        socket.emit('send', zip.toBuffer(), () => {
                            // ACK function
                            console.log('send-app');
                            const msg = { message: "$MSG_SEND", appName };
                            client.write(JSON.stringify(msg) + "\r\n");
                            console.log(msg);
                        });
                    });
                });

                server.on('error', (error) => {
                    const msg = { message: "$MSG_SERVER_ERROR", error };
                    client.write(JSON.stringify(msg) + "\r\n");
                    console.log(msg);
                    throw error;
                });
                server.listen(0, (err) => {
                    const msg = { message: "$MSG_SERVER_OK", server: server.address() };
                    client.write(JSON.stringify(msg) + "\r\n");
                    console.log(msg);
                    if (err) throw err;
                    console.log('Socket ready on http://localhost:' + server.address().port + ', pid:' + process.pid);
                });

                const connections = [];
                let networkInterfaces = os.networkInterfaces();

                Object.keys(networkInterfaces).forEach((ifaceName, index, array) => {
                    let connection = {};
                    let alias = 0;
                    networkInterfaces[ifaceName].forEach((iface, index, array) => {

                        if (iface.family !== 'IPv4' || iface.internal) {
                            return;
                        }

                        if (alias === 0) {
                            connection.ip = iface.address;
                            connection.name = ifaceName;
                            connection.mac = iface.mac;
                        }
                        alias++;
                    });

                    if (!(Object.keys(connection).length === 0 && connection.constructor === Object)) {
                        connections.push(connection);
                    }
                });

                const client = net.createConnection(pathToSock, () => {
                    console.log('connected to IPC server');
                    const msg = { message: "$MSG_CONNECTIONS", connections };
                    client.write(JSON.stringify(msg) + "\r\n");
                    console.log(msg);
                });
                client.on('data', (data) => {
                    const msg = JSON.parse(data.toString());
                    console.log(msg);
                    const { message } = msg;
                    if (message == "$MSG_CANCEL") {
                        clientEnd();
                    }
                });

                function clientEnd() {
                    client.end(() => {
                        console.log('disconnected from IPC server\n\n');
                        process.exit();
                    });
                }
            """
    }
}
