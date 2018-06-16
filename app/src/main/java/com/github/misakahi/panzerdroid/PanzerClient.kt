package com.github.misakahi.panzerdroid

import android.util.Log
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.StatusRuntimeException
import panzer.PanzerGrpc
import panzer.PanzerOuterClass

class PanzerClient internal constructor(channel: ManagedChannel) {

    val blockingStub: PanzerGrpc.PanzerBlockingStub = PanzerGrpc.newBlockingStub(channel)

    constructor(host: String, port: Int) : this(ManagedChannelBuilder.forAddress(host, port)
            .usePlaintext(true)
            .build())

    fun drive(left_level: Double, right_level: Double) {
        val request = PanzerOuterClass.DriveRequest.newBuilder()
                .setLeftLevel(left_level)
                .setRightLevel(right_level)
                .build()

        val response = try {
            blockingStub.drive(request)
        } catch (e: StatusRuntimeException) {
            Log.w(this.javaClass.name, "RPC failed " + e.status)
        }
        Log.i(this.javaClass.name, response.toString())
    }

    /**
     * Ping-Pong (heartbeat)
     *
     * @return true if ping-pong succeeds otherwise false
     */
    fun pingPong(): Boolean {
        val request = PanzerOuterClass.Ping.newBuilder().build()
        return try {
            blockingStub.sendPing(request)
            true
        } catch (e: StatusRuntimeException) {
            Log.w(this.javaClass.name, "RPC failed " + e.status)
            false
        }
    }
}

