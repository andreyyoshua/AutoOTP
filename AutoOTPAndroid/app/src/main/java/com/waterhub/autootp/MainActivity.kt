package com.waterhub.autootp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.telephony.SmsManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.net.URISyntaxException
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import com.github.nkzawa.emitter.Emitter;
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.row.*
import tech.gusavila92.websocketclient.WebSocketClient
import java.net.URI


class MainActivity : AppCompatActivity() {

    private var datas = ArrayList<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            // Permission is not granted
            // Ask for permision
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.SEND_SMS), 1);
        } else {
            createWebSocketClient()
            recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, true)
            recyclerView.adapter = MainAdapter()
        }

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if ((grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED)
        ) {
            // Permission is granted. Continue the action or workflow
            // in your app.

            createWebSocketClient()
            recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, true)
            recyclerView.adapter = MainAdapter()
        } else {
            // Explain to the user that the feature is unavailable because
            // the features requires a permission that the user has denied.
            // At the same time, respect the user's decision. Don't link to
            // system settings in an effort to convince the user to change
            // their decision.
        }
    }

    private fun sendSMS(phoneNo: String?, msg: String?) {
        try {
            val smsManager: SmsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(phoneNo, null, msg, null, null)
            Toast.makeText(
                applicationContext, "Message Sent",
                Toast.LENGTH_LONG
            ).show()
            datas.add("Sending Message $msg to $phoneNo")
            recyclerView.adapter?.notifyDataSetChanged()
        } catch (ex: Exception) {
            Toast.makeText(
                applicationContext, ex.message.toString(),
                Toast.LENGTH_LONG
            ).show()
            ex.printStackTrace()
        }
    }

    private fun createWebSocketClient() {
        val uri: URI?
        try {
            // Connect to local host
            uri = URI.create("ws://172.20.10.7:3000/")
        } catch (e: URISyntaxException) {
            e.printStackTrace();
            return;
        }

        var webSocketClient: WebSocketClient? = null
        webSocketClient = object : WebSocketClient(uri) {

            override fun onOpen() {
                Log.i("WebSocket", "Session is starting");
                runOnUiThread {
                    progress_bar.visibility = View.INVISIBLE
                    connected.visibility = View.VISIBLE
                }
            }

            override fun onTextReceived(message: String?) {
                runOnUiThread {
                    val data = Gson().fromJson<MainData>(message, MainData::class.java)
                    sendSMS(data.dest, data.msg)
                }
                Log.i("WebSocket", "Message received $message");
            }

            override fun onBinaryReceived(data: ByteArray?) {
                Log.i("WebSocket", "Message received $data");
            }

            override fun onPingReceived(data: ByteArray?) {

            }

            override fun onPongReceived(data: ByteArray?) {
            }

            override fun onException(e: java.lang.Exception?) {
                Log.e("Error", "${e?.stackTraceToString()}")
                runOnUiThread {
                    progress_bar.visibility = View.VISIBLE
                    connected.visibility = View.INVISIBLE
                }
            }

            override fun onCloseReceived() {
                Log.i("WebSocket", "Closed ");
                runOnUiThread {
                    progress_bar.visibility = View.VISIBLE
                    connected.visibility = View.INVISIBLE
                }
            }
        };
        webSocketClient.setConnectTimeout(10000);
        webSocketClient.setReadTimeout(60000);
        webSocketClient.enableAutomaticReconnection(5000);
        webSocketClient.connect();
    }

    data class MainData(val msg: String, val dest: String)

    inner class MainAdapter: RecyclerView.Adapter<MainAdapter.MainViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MainViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.row, parent, false)
            return MainViewHolder(view)
        }

        override fun onBindViewHolder(holder: MainViewHolder, position: Int) {
            holder.textView.text = datas[position]
        }

        override fun getItemCount(): Int {
            return datas.size
        }

        inner class MainViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val textView = itemView.findViewById<TextView>(R.id.textView)
        }
    }
}