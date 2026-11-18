package com.example.neckguardian.domain

import kotlin.math.*

data class BlockPose(val x:Double,val y:Double,val z:Double,val qx:Double,val qy:Double,val qz:Double,val qw:Double)

/** Replays the approved GLB samples with original LINEAR translation and quaternion interpolation. */
class TowerPlayback(private val frames:List<BlockPose>) {
    init {require(frames.isNotEmpty())}
    fun sample(seconds:Double):BlockPose {
        val at=(seconds*30).coerceIn(0.0,frames.lastIndex.toDouble())
        val i=floor(at).toInt()
        return blend(frames[i],frames[min(i+1,frames.lastIndex)],at-i)
    }
    /** Keep the last live pose at t=0; join the exact authored release pose before original collapse. */
    fun falling(seconds:Double,from:BlockPose):BlockPose = if(seconds<.4) {
        val t=(seconds/.4).coerceIn(0.0,1.0)
        blend(from,sample(.6),t*t*(3-2*t))
    } else sample(.6+seconds-.4)

    private fun blend(a:BlockPose,b:BlockPose,t:Double):BlockPose {
        val dot=a.qx*b.qx+a.qy*b.qy+a.qz*b.qz+a.qw*b.qw
        val sign=if(dot<0) -1.0 else 1.0
        val d=abs(dot).coerceIn(0.0,1.0)
        val theta=acos(d)
        val wa=if(d>.9995) 1-t else sin((1-t)*theta)/sin(theta)
        val wb=(if(d>.9995) t else sin(t*theta)/sin(theta))*sign
        val x=wa*a.qx+wb*b.qx;val y=wa*a.qy+wb*b.qy
        val z=wa*a.qz+wb*b.qz;val w=wa*a.qw+wb*b.qw
        val n=sqrt(x*x+y*y+z*z+w*w)
        return BlockPose(a.x+(b.x-a.x)*t,a.y+(b.y-a.y)*t,a.z+(b.z-a.z)*t,x/n,y/n,z/n,w/n)
    }
}
