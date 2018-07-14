package com.github.misakahi.panzerdroid

import android.util.Log
import com.google.protobuf.MessageLite
import io.grpc.StatusRuntimeException
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread


enum class Command {
    DRIVE,
    MOVE_TURRET,
}


class CommandSender constructor(host: String, port: Int) {
    private val client = PanzerClient(host, port)
    private val commandMap = ConcurrentHashMap<Command, ProtoCompatible>()
    private val sendCommandThread = object : RepeatThread() {
        override fun running() {
            if (commandMap.size > 0) {
                val data = buildControlData()
                sendActually(client.blockingStub::control, data)
                deactivateAll()
            }
        }
    }

    fun activate(command: Command, data: ProtoCompatible) {
        Log.i(this.javaClass.name, "activate " + command.toString())
        commandMap[command] = data
    }

    fun activateAndSend(command: Command, data: ProtoCompatible) {
        activate(command, data)
        send(command, data)
    }

    fun deactivate(command: Command) {
        Log.i(this.javaClass.name, "deactivate " + command.toString())
        commandMap.remove(command)
    }

    fun deactivateAll() {
        commandMap.keys.forEach { deactivate(it) }
    }

    fun send() {
        for (entry in commandMap.entries) {
            send(entry.key, entry.value)

            // no longer needed after sending
            deactivate(entry.key)
        }
    }

    fun send(command: Command, data: ProtoCompatible) {
        when (command) {
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
        Log.i(this.javaClass.name, "Send " + request.toString())
        val response = try {
            call(request)
        } catch (e: StatusRuntimeException) {
            Log.w(this.javaClass.name, "RPC failed " + e.status)
            null
        }
        Log.i(this.javaClass.name, "Recv " + response.toString())
        return response
    }

    fun start(sendCommandIntervalMillis: Long) {
        Log.v(this.javaClass.name, "Starting... interval=$sendCommandIntervalMillis")
        sendCommandThread.start(sendCommandIntervalMillis)
    }

    fun stop() {
        Log.v(this.javaClass.name, "Stopping...")
        sendCommandThread.stop()
    }

    fun pingPong(): Boolean {
        return client.pingPong()
    }
}