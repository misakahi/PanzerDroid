package com.github.misakahi.panzerdroid

import android.content.Intent
import android.graphics.Color
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.preference.PreferenceManager
import android.util.Log
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import com.github.misakahi.panzerdroid.settings.SettingsActivity
import com.github.niqdev.mjpeg.DisplayMode
import com.github.niqdev.mjpeg.Mjpeg
import com.github.niqdev.mjpeg.MjpegSurfaceView
import io.github.controlwear.virtual.joystick.android.JoystickView

class MainActivity : AppCompatActivity() {

    // constants
    private val heartbeatIntervalMillis: Long = 1000

    // Views
    private val mjpegView by lazy { findViewById<MjpegSurfaceView>(R.id.mjpeg_view) }
    private val serverConnectionTextView by lazy { findViewById<TextView>(R.id.server_connection_text_view) }
    private val cameraConnectionTextView by lazy { findViewById<TextView>(R.id.camera_connection_text_view) }
    private val joystickViewLeft by lazy { findViewById<JoystickView>(R.id.joystickViewLeft) }

    private val handler= Handler()
    private var commandSender: CommandSender? = null
    private var pingPong: RepeatThread? = null

    // constants
    private val pingPongIntervalMillis: Long = 1000
    private val sendCommandIntervalMillis: Long = 50

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Setting button - show setting view
        findViewById<ImageButton>(R.id.setting_button).setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        // Reconnect button - try reconnect server
        findViewById<ImageButton>(R.id.reconnect_button).setOnClickListener {
            reconnect()
        }

        // Joy stick for driving
        joystickViewLeft.setOnMoveListener { angle, strength -> run {
            Log.v(this.javaClass.name, "$angle $strength")
            commandSender?.send(Command.DRIVE, DriveData.fromAngleStrength(angle, strength))
        } }
    }

    override fun onResume() {
        super.onResume()
        reconnect()
    }

    private fun reconnect() {
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
        val host: String = sharedPref.getString(SettingsActivity.KEY_PREF_HOST, getString(R.string.pref_default_host))
        val controlPort: Int = sharedPref.getString(SettingsActivity.KEY_PREF_CONTROL_PORT, getString(R.string.pref_default_control_port)).toInt()
        val cameraPort: Int = sharedPref.getString(SettingsActivity.KEY_PREF_CAMERA_PORT, getString(R.string.pref_default_camera_port)).toInt()

        connectServer(host, controlPort)
        connectCamera(host, cameraPort, 5)
        startPingPong(host, controlPort)

        // Show connection status
        with(serverConnectionTextView) {
            text = String.format(getString(R.string.view_server_connection_status), host, controlPort)
            setTextColor(Color.RED)
        }
        with(cameraConnectionTextView) {
            text = String.format(getString(R.string.view_camera_connection_status), host, cameraPort)
            setTextColor(Color.RED)
        }
    }

    /**
     * Connect control/camera server according to pref
     */
    private fun connectServer(host: String, port: Int) {

        Log.v("MAIN", "Rebuild CommandSender($host, $port)")

        try {
            commandSender?.stop()
            commandSender = CommandSender(host, port)
            commandSender?.start(sendCommandIntervalMillis)
            Toast.makeText(this, "Connecting to $host:$port", Toast.LENGTH_SHORT).show()
        }
        catch (e: IllegalArgumentException) {
            Toast.makeText(this, "illegal host and port", Toast.LENGTH_SHORT).show()
        }
        catch (e: Exception) {
            Toast.makeText(this, "Oops! Something is wrong... Debug me!", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Start ping pong to check connection availability
     */
    private fun startPingPong(host: String, port: Int) {
        pingPong?.stop()  // stop if already exist
        pingPong = object : RepeatThread() {
            override fun running() {
                val isConnected: Boolean = commandSender?.pingPong() ?: false
                val color = if (isConnected) Color.GREEN else Color.RED

                handler.post {
                    serverConnectionTextView.setTextColor(color)
                }
                Log.v("pingpong", "$host:$port $isConnected")
            }
        }
        pingPong?.start(pingPongIntervalMillis)
    }

    private fun connectCamera(host: String, port: Int, timeout: Int) {
        val streamUrl = "http://$host:$port/?action=stream"

        // TODO how to disconnect?
        Mjpeg.newInstance()
                .open(streamUrl, timeout)
                .subscribe({
                    mjpegView.setSource(it)
                    mjpegView.setDisplayMode(DisplayMode.BEST_FIT)
                    mjpegView.showFps(true)
                    cameraConnectionTextView.setTextColor(Color.GREEN)
                },
                {
                    Log.e("loadIpCam", it.toString())
                    Toast.makeText(this, "Error: Unable to connect camera.", Toast.LENGTH_LONG).show()
                    cameraConnectionTextView.setTextColor(Color.RED)
                })
    }

}
