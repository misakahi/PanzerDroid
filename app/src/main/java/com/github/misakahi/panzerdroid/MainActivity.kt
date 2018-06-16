package com.github.misakahi.panzerdroid

import android.content.Intent
import android.graphics.Color
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.preference.PreferenceManager
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.github.misakahi.panzerdroid.settings.SettingsActivity
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    // constants
    private val heartbeatIntervalMillis: Long = 1000

    // Views
    private val connectionTextView by lazy { findViewById<TextView>(R.id.connection_text_view) }

    private val handler= Handler()
    private var commandSender: CommandSender? = null
    private var heartbeatThread: Thread? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.setting_button).setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        findViewById<View>(R.id.text).setOnTouchListener{ _, motionEvent ->
            Log.i("main",""+motionEvent.action)
            when (motionEvent.action) {
                MotionEvent.ACTION_DOWN -> {
                    commandSender?.activateAndSend(Command.DRIVE, DriveData(1.0, 1.0))
                }
                MotionEvent.ACTION_MOVE -> {
                    commandSender?.activate(Command.DRIVE, DriveData(1.0, 1.0))
                }
                MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                    commandSender?.deactivate(Command.DRIVE)
                }
            }
            return@setOnTouchListener true
        }
    }

    override fun onResume() {
        super.onResume()
        connectServer()

        if (heartbeatThread== null)
            heartbeatThread = startHeartbeat()
    }

    /**
     * Connect control/camera server according to pref
     */
    private fun connectServer() {
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
        val host: String = sharedPref.getString(SettingsActivity.KEY_PREF_HOST, "")
        val port: Int = sharedPref.getString(SettingsActivity.KEY_PREF_CONTROL_PORT, "0").toInt()

        Log.v("MAIN", "Rebuild CommandSender($host, $port)")

        try {
            commandSender?.stopThreads()
            commandSender = CommandSender(host, port)
            commandSender?.startThread(10)
            Toast.makeText(this, "Connecting to $host:$port", Toast.LENGTH_SHORT).show()
        }
        catch (e: IllegalArgumentException) {
            Toast.makeText(this, "illegal host and port", Toast.LENGTH_SHORT).show()
        }
        catch (e: Exception) {
            Toast.makeText(this, "Oops! Something is wrong... Debug me!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startHeartbeat(): Thread {
        return thread {
            while (true) {
                val sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
                val host: String = sharedPref.getString(SettingsActivity.KEY_PREF_HOST, "")
                val port: String = sharedPref.getString(SettingsActivity.KEY_PREF_CONTROL_PORT, "0")
                val isConnected: Boolean = commandSender?.pingPong() ?: false
                val color = if (isConnected) Color.GREEN else Color.RED

                handler.post {
                    connectionTextView.text = "$host:$port"
                    connectionTextView.setTextColor(color)
                }
                Log.v("heartbeat", "$host:$port $isConnected")
                Thread.sleep(heartbeatIntervalMillis)
            }
        }
    }
}
