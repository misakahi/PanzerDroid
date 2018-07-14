package com.github.misakahi.panzerdroid

import android.util.Log
import com.google.protobuf.MessageLite
import panzer.PanzerOuterClass

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
        fun fromAngleStrength(angle: Int, strength: Int): DriveData {
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

data class MoveTurretData(val rotation: Double = 0.0, val updown: Double = 0.0) : ProtoCompatible {

    override fun toProto(): PanzerOuterClass.MoveTurretRequest {
        return PanzerOuterClass.MoveTurretRequest.newBuilder()
                .setRotation(rotation)
                .setUpdown(updown)
                .build()
    }

    companion object {
        /**
         * Create MoveTurretData from angle
         *
         * Currently ignore strength
         */
        fun fromAngleStrength(angle: Int, strength: Int): MoveTurretData {
            val theta = angle / 180.0 * Math.PI   // deg -> rad
            val cos = Math.cos(theta)
            val sin = Math.sin(theta)

            return MoveTurretData(rotation = cos, updown = sin)
        }
    }
}

data class ControlData(var driveData: DriveData?, var moveTurretData: MoveTurretData?) : ProtoCompatible {

    constructor() : this(null, null)

    override fun toProto(): MessageLite {
        val builder = PanzerOuterClass.ControlRequest.newBuilder()
        builder.driveRequest = driveData?.toProto() ?: PanzerOuterClass.DriveRequest.newBuilder().build()
        builder.moveTurretRequest = moveTurretData?.toProto() ?: PanzerOuterClass.MoveTurretRequest.newBuilder().build()

        return builder.build()
    }
}
