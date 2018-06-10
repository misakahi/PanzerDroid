package com.github.misakahi.panzerdroid

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Button

class MainActivity : AppCompatActivity() {

    private val button: Button by lazy { findViewById<Button>(R.id.button) }
    private val host = "192.168.1.17"
    private val port = 50051
    // private val rpcStub by lazy { PanzerClient("192.168.1.17", 50051) }
    private val sender = CommandSender(host, port)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        button.setOnClickListener {
            // this.rpcStub.drive(-1.0, 1.0)
        }

        findViewById<View>(R.id.text).setOnTouchListener{ view, motionEvent ->
            Log.i("main",""+motionEvent.action)
            when (motionEvent.action) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                    sender.activate(Command.DRIVE, DriveData(1.0, 1.0))
                }
                MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                    sender.deactivate(Command.DRIVE)
                }
            }
            return@setOnTouchListener true
        }

        sender.startWatch(50)
    }
}
