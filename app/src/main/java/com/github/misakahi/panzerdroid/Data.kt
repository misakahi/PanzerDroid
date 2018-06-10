package com.github.misakahi.panzerdroid

import com.google.protobuf.MessageLite
import panzer.PanzerOuterClass

interface ProtoCompatible {
    fun toProto(): MessageLite?
}

data class DriveData(val left_level: Double, val right_level: Double) : ProtoCompatible {

    override fun toProto(): PanzerOuterClass.DriveRequest? {
        return PanzerOuterClass.DriveRequest.newBuilder()
                .setLLevel(left_level)
                .setRLevel(right_level)
                .build()
    }
}
