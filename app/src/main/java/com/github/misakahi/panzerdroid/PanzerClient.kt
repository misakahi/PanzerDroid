package com.github.misakahi.panzerdroid

import android.util.Log
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.StatusRuntimeException
import panzer.PanzerGrpc
import panzer.PanzerOuterClass

class PanzerClient
    internal constructor(channel: ManagedChannel) {

    val blockingStub = PanzerGrpc.newBlockingStub(channel)
    private val TAG = "PanzerClient"

    constructor(host: String, port: Int) : this(ManagedChannelBuilder.forAddress(host, port)
            .usePlaintext(true)
            .build())

    fun drive(left_level: Double, right_level: Double) {
        val request = PanzerOuterClass.DriveRequest.newBuilder()
                .setLLevel(left_level)
                .setRLevel(right_level)
                .build()

        val response = try {
            blockingStub.drive(request)
        } catch (e: StatusRuntimeException) {
            Log.w(TAG, "RPC failed " + e.status)
        }
        Log.i(TAG, response.toString())
    }
}

