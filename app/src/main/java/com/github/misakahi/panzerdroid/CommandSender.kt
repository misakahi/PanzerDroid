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
    val client = PanzerClient(host, port)

    val commandMap = HashMap<Command, ProtoCompatible>()

    fun activate(command: Command, data: ProtoCompatible) {
        Log.i(TAG, "activate " + command.toString())
        commandMap[command] = data
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

    fun<T: MessageLite, U: MessageLite> sendActually(call: (T)->U , data: ProtoCompatible): U? {
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

    fun startThread(intervalMillis: Long) {
        thread {
            var count: Long = 0
            while (true) {
                if (commandMap.size > 0) {
                    val data = buildControlData()
                    sendActually(client.blockingStub::control, data)
                }
                Thread.sleep(intervalMillis)
                count += intervalMillis
            }
        }
    }
}