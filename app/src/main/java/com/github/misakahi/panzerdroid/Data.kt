package com.github.misakahi.panzerdroid

import android.util.Log
import com.google.protobuf.MessageLite
import panzer.PanzerOuterClass
import kotlin.math.sign

interface ProtoCompatible {
    fun toProto(): MessageLite
}

data class DriveData(val leftLevel: Double = 0.0, val rightLevel: Double = 0.0) : ProtoCompatible {

    override fun toProto(): PanzerOuterClass.DriveRequest {
        return PanzerOuterClass.DriveRequest.newBuilder()
                .setLeftLevel(leftLevel)
                .setRightLevel(rightLevel)
                .build()
    }

    companion object {

        /**
         * Create DriveDate from angle and strength on a joy pad
         *
         * @param angle 0 to 359
         * @param strength 0 to 100, radius ratio
         * @return DriveDate
         */
        public fun fromAngleStrength(angle: Int, strength: Int): DriveData {
            val level = strength / 100.0
            val theta = angle / 180.0 * Math.PI   // deg -> rad
            val sin = Math.sin(theta)
            val signum = Math.signum(sin)

            Log.v("DriveData", "$angle $strength $level $theta $sin $signum")

            // TODO spin turn
            return if (angle in 0..90 || angle in 270..360) {
                DriveData(level*signum, level*sin)
            } else if (angle in 90..270) {
                DriveData(level*sin, level* signum)
            } else {
                Log.v("DriveData", "invalid angle $angle")
                DriveData()
            }
        }
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
