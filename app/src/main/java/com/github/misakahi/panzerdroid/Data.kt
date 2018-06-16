package com.github.misakahi.panzerdroid

import com.google.protobuf.MessageLite
import panzer.PanzerOuterClass

interface ProtoCompatible {
    fun toProto(): MessageLite
}

data class DriveData(val left_level: Double, val right_level: Double) : ProtoCompatible {

    override fun toProto(): PanzerOuterClass.DriveRequest {
        return PanzerOuterClass.DriveRequest.newBuilder()
                .setLeftLevel(left_level)
                .setRightLevel(right_level)
                .build()
    }
}

data class ControlData(var driveData: DriveData?) : ProtoCompatible {

    constructor() : this(null)

    override fun toProto(): MessageLite {
        val builder = PanzerOuterClass.ControlRequest.newBuilder()
        builder.driveRequest = driveData?.toProto() ?: PanzerOuterClass.DriveRequest.newBuilder().build()

        return builder.build()
    }
}
