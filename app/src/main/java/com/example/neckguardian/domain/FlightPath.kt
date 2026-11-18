package com.example.neckguardian.domain

import kotlin.math.*

/** Shared by rendering and collection. Coordinates are meters, forward is -Z. */
object FlightPath {
    const val planeScale = 0.5
    const val planeZ = 0.10
    const val planeYaw = 0f
    const val starLifetime = 5.0
    private const val starRadius = 0.024
    private val outline = listOf(0.0 to -0.15, -0.11 to 0.085, 0.0 to 0.105, 0.11 to 0.085).map { (x,z) -> x*planeScale to z*planeScale }
    fun hasPassedCollectionZone(z:Double) = z > planeZ + outline.maxOf {it.second} + starRadius
    fun planeX(position: Double) = position * 0.24
    fun starZ(age: Double) = -0.35 + age * 0.15

    /** Swept star circle against the authored paper outline; catches wing/nose contact and long frames. */
    fun touches(previousX: Double, previousZ: Double, x: Double, z: Double): Boolean {
        if(listOf(previousX,previousZ,x,z).any {!it.isFinite()}) return false
        if(inside(x,z) || inside(previousX,previousZ)) return true
        return outline.indices.any {i ->
            val a=outline[i];val b=outline[(i+1)%outline.size]
            segmentsTouch(previousX,previousZ,x,z,a.first,a.second,b.first,b.second) ||
                distance2(previousX,previousZ,a.first,a.second,b.first,b.second)<=starRadius*starRadius ||
                distance2(x,z,a.first,a.second,b.first,b.second)<=starRadius*starRadius ||
                distance2(a.first,a.second,previousX,previousZ,x,z)<=starRadius*starRadius ||
                distance2(b.first,b.second,previousX,previousZ,x,z)<=starRadius*starRadius
        }
    }
    private fun inside(x:Double,z:Double):Boolean {
        var sign=0
        for(i in outline.indices) {
            val a=outline[i];val b=outline[(i+1)%outline.size]
            val cross=(b.first-a.first)*(z-a.second)-(b.second-a.second)*(x-a.first)
            if(abs(cross)<1e-10) continue
            val s=if(cross>0) 1 else -1
            if(sign!=0 && sign!=s) return false
            sign=s
        }
        return true
    }
    private fun distance2(px:Double,pz:Double,ax:Double,az:Double,bx:Double,bz:Double):Double {
        val dx=bx-ax;val dz=bz-az;val length=dx*dx+dz*dz
        val t=if(length==0.0) 0.0 else (((px-ax)*dx+(pz-az)*dz)/length).coerceIn(0.0,1.0)
        return (px-ax-t*dx).pow(2)+(pz-az-t*dz).pow(2)
    }
    private fun segmentsTouch(ax:Double,az:Double,bx:Double,bz:Double,cx:Double,cz:Double,dx:Double,dz:Double):Boolean {
        val rx=bx-ax;val rz=bz-az;val sx=dx-cx;val sz=dz-cz
        val denominator=rx*sz-rz*sx
        if(abs(denominator)<1e-10) return false
        val t=((cx-ax)*sz-(cz-az)*sx)/denominator
        val u=((cx-ax)*rz-(cz-az)*rx)/denominator
        return t in 0.0..1.0 && u in 0.0..1.0
    }
}
