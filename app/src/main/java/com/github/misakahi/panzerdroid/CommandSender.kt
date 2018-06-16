package com.github.misakahi.panzerdroid

import android.util.Log
import com.google.protobuf.MessageLite
import io.grpc.StatusRuntimeException
import kotlin.concurrent.thread


enum class Command {
    DRIVE,
}


class CommandSender constructor(host: String, port: Int) {
    val TAG = "CommandSender"
    private val client = PanzerClient(host, port)

    private val commandMap = HashMap<Command, ProtoCompatible>()

    // threads
    var sendCommandThread: Thread? = null
    var isThreadActive = false

    fun activate(command: Command, data: ProtoCompatible) {
        Log.i(TAG, "activate " + command.toString())
        commandMap[command] = data
    }

    fun activateAndSend(command: Command, data: ProtoCompatible) {
        activate(command, data)
        send(command, data)
    }

    fun deactivate(command: Command) {
        Log.i(TAG, "deactivate " + command.toString())
        commandMap.remove(command)
    }

    fun send() {
        for (entry in commandMap.entries) {
            send(entry.key, entry.value)
        }
    }

    fun send(command: Command, data: ProtoCompatible) {
        when(command) {
            Command.DRIVE -> {
                sendActually(client.blockingStub::drive, data)
            }
        }
    }

    fun buildControlData(): ControlData {
        val data = ControlData()
        for (entry in commandMap.entries) {
            when (entry.key) {
                Command.DRIVE -> data.driveData = entry.value as DriveData?
            }
        }
        return data
    }

    private fun<T: MessageLite, U: MessageLite> sendActually(call: (T)->U , data: ProtoCompatible): U? {
        @Suppress("UNCHECKED_CAST")
        val request = data.toProto() as T
        Log.i(TAG, "Send " + request.toString())
        val response = try {
            call(request)
        } catch (e: StatusRuntimeException) {
            Log.w(TAG, "RPC failed " + e.status)
            null
        }
        Log.i(TAG, "Recv " + response.toString())
        return response
    }

    fun startThread(sendCommandIntervalMillis: Long) {
        isThreadActive = true

        // thread already exist. do nothing.
        if (sendCommandThread != null)
            return

        sendCommandThread = thread {
            while (isThreadActive) {
                if (commandMap.size > 0) {
                    val data = buildControlData()
                    sendActually(client.blockingStub::control, data)
                }
                Thread.sleep(sendCommandIntervalMillis)
            }
            Log.v(TAG, "sendCommandThread stopping")
        }
    }

    fun stopThreads() {
        isThreadActive = false
        sendCommandThread = null
    }

    fun pingPong(): Boolean {
        return client.pingPong()
    }
}