package com.example.neckguardian.domain
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*
class PoseMathTest {
    @Test fun headsetAxesDistinguishExtensionDownwardAndSides() {
        fun matrix(pitch:Double=0.0,roll:Double=0.0):DoubleArray {
            val p=Math.toRadians(pitch);val r=Math.toRadians(roll)
            // Upright headset R0 * Rx(pitch) * Rz(roll).
            return doubleArrayOf(cos(r),-sin(r),0.0,-sin(p)*sin(r),-sin(p)*cos(r),-cos(p),cos(p)*sin(r),cos(p)*cos(r),-sin(p))
        }
        val baseline=headsetAngles(matrix())
        assertEquals(20.0,relativeDegrees(headsetAngles(matrix(pitch=20.0)).first,baseline.first),.001)
        assertEquals(-25.0,relativeDegrees(headsetAngles(matrix(pitch=-25.0)).first,baseline.first),.001)
        assertEquals(18.0,headsetAngles(matrix(roll=-18.0)).second,.001)
        assertEquals(-18.0,headsetAngles(matrix(roll=18.0)).second,.001)
        val combined=headsetAngles(matrix(pitch=30.0,roll=-20.0))
        assertEquals(30.0,combined.first,.001)
        assertEquals(20.0,combined.second,.001)
    }
}
