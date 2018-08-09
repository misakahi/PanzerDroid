package com.github.misakahi.panzerdroid

import android.util.Log
import com.google.protobuf.MessageLite
import panzer.PanzerOuterClass

interface ProtoCompatible {
    fun toProto(): MessageLite
}

data class DriveData(var leftLevel: Double? = null, var rightLevel: Double? = null) : ProtoCompatible {

    override fun toProto(): PanzerOuterClass.DriveRequest {
        val builder = PanzerOuterClass.DriveRequest.newBuilder()
        leftLevel?.let { builder.setLeftLevel(it) }
        rightLevel?.let { builder.setRightLevel(it) }
        return builder.build()
    }

    companion object {

        var SPINTURN_ANGLE_DEG = 10

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
            val signum = Math.signum(sin)   // either of -1, 0, +1

            Log.v("DriveData", "$angle $strength $level $theta $sin $signum")

            return if (angle in 0..10 || angle in 350..360) {
                // neutral steering in clockwise
                DriveData(level, -level)
            }
            else if (angle in 170..190) {
                // neutral steering in counterclockwise
                DriveData(-level, level)
            }
            else if (angle in 0..90 || angle in 270..360) {
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

data class MoveTurretData(var rotation: Double? = null, var updown: Double? = null) : ProtoCompatible {

    override fun toProto(): PanzerOuterClass.MoveTurretRequest {
        val builder = PanzerOuterClass.MoveTurretRequest.newBuilder()
        rotation?.let { builder.setRotation(it) }
        updown?.let { builder.setUpdown(it) }
        return builder.build()
    }

    companion object {
        /**
         * Create MoveTurretData from angle
         *
         * Currently ignore strength
         */
        fun fromAngleStrength(angle: Int, strength: Int): MoveTurretData {
            if (angle == 0 && strength == 0)
                return MoveTurretData()

            val speedFactor = 0.25  // restrict max speed
            val level = strength / 100.0 * speedFactor
            val theta = angle / 180.0 * Math.PI   // deg -> rad
            val cos = Math.cos(theta)
            val sin = Math.sin(theta)

            Log.v("MoveTurretData", "$angle $strength $theta $cos $sin")

            return MoveTurretData(rotation = level * cos, updown = level * sin)
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
