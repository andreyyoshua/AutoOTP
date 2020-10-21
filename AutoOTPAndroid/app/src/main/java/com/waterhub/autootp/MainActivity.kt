package com.waterhub.autootp

import android.Manifest
import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_main.*
import tech.gusavila92.websocketclient.WebSocketClient
import java.net.URI
import java.net.URISyntaxException


class MainActivity : AppCompatActivity() {

    private var datas = ArrayList<String>()
    private var webSocketClient: WebSocketClient? = null

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
            recyclerView.layoutManager = LinearLayoutManager(this)
            recyclerView.adapter = MainAdapter()
            addData(200)
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
            val SENT = "SMS_SENT"
            val sentPI = PendingIntent.getBroadcast(this, 0, Intent(SENT), 0)

            val DELIVERY = "SMS_DELIVERY"
            val deliveryPI = PendingIntent.getBroadcast(this, 0, Intent(DELIVERY), 0)

            registerReceiver(object : BroadcastReceiver() {
                override fun onReceive(arg0: Context?, arg1: Intent?) {
                    when (resultCode) {
                        Activity.RESULT_OK -> {
                            Toast.makeText(
                                baseContext,
                                "SMS sent",
                                Toast.LENGTH_SHORT
                            ).show()
                            addData(200, "Success send Message $msg to $phoneNo")
                        }
                        SmsManager.RESULT_ERROR_GENERIC_FAILURE -> {
                            Toast.makeText(
                                baseContext,
                                "Generic failure",
                                Toast.LENGTH_SHORT
                            ).show()
                            addData(500, "Generic failure send Message $msg to $phoneNo")
                        }
                        SmsManager.RESULT_ERROR_NO_SERVICE -> {
                            Toast.makeText(
                                baseContext,
                                "No service",
                                Toast.LENGTH_SHORT
                            ).show()

                            addData(500, "No service send Message $msg to $phoneNo")
                        }
                        SmsManager.RESULT_ERROR_NULL_PDU -> {
                            Toast.makeText(
                                baseContext,
                                "Null PDU",
                                Toast.LENGTH_SHORT
                            ).show()

                            addData(500, "Null PDU send Message $msg to $phoneNo")
                        }
                        SmsManager.RESULT_ERROR_RADIO_OFF -> {
                            Toast.makeText(
                                baseContext,
                                "Radio off",
                                Toast.LENGTH_SHORT
                            ).show()
                            addData(500, "Radio off send Message $msg to $phoneNo")
                        }
                        else -> {
                            addData(500, "Unknown error failed send Message $msg to $phoneNo")
                        }
                    }
                }
            }, IntentFilter(SENT))

            val smsManager: SmsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(phoneNo, null, msg, sentPI, null)
            recyclerView.adapter?.notifyDataSetChanged()

        } catch (ex: Exception) {
            Toast.makeText(
                applicationContext, ex.message.toString(),
                Toast.LENGTH_LONG
            ).show()
            addData(500, "Failed to send Message $msg to $phoneNo")
            ex.printStackTrace()
        }
    }

    private fun addData(code: Int, msg: String? = null) {
        webSocketClient?.send("{\"code\":$code, \"message\":\"$msg\"}")
        msg?.let { datas.add(it) }
        totalTextView.text = datas.size.toString()
        recyclerView.adapter?.notifyDataSetChanged()
    }

    private fun createWebSocketClient() {
        val uri: URI?
        try {
            // Connect to local host
            uri = URI.create("ws://andreyyoshua.com:3000/")
        } catch (e: URISyntaxException) {
            e.printStackTrace();
            return;
        }

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
        webSocketClient?.setConnectTimeout(10000);
        webSocketClient?.setReadTimeout(60000);
        webSocketClient?.enableAutomaticReconnection(1000);
        webSocketClient?.connect();
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