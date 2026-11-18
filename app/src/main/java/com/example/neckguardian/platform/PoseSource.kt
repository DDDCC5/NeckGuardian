package com.example.neckguardian.platform

import android.content.Context
import android.os.SystemClock
import android.util.Log
import androidx.compose.runtime.*
import com.example.neckguardian.domain.relativeDegrees
import com.pico.spatial.core.math.Quat
import com.pico.spatial.core.math.Vector3
import com.pico.spatial.tracking.hmd.HMDTrackingProvider
import kotlinx.coroutines.*

/** Official HMD tracking. Started only by the mixed training Stage. */
class PoseSource(@Suppress("UNUSED_PARAMETER") context:Context) {
    private val provider=HMDTrackingProvider()
    private var collection:Job?=null
    var pitch by mutableDoubleStateOf(0.0); private set
    var roll by mutableDoubleStateOf(0.0); private set
    var lastEventMs by mutableLongStateOf(0); private set
    var available by mutableStateOf(false); private set
    var position:Vector3=Vector3.ZERO; private set
    var rotation:Quat=Quat(); private set
    private var rawPitch=0.0
    private var rawRoll=0.0
    private var basePitch=0.0
    private var baseRoll=0.0
    val fresh get()=available && lastEventMs>0 && SystemClock.elapsedRealtime()-lastEventMs<1000
    fun start() {
        if(collection?.isActive==true) return
        lastEventMs=0;available=false
        collection=CoroutineScope(Dispatchers.Main.immediate).launch {
            provider.dataFlow.collect { data ->
                position=data.hmdPose.position;rotation=data.hmdPose.rotation
                val e=rotation.toEulerAngles()
                rawPitch=e.pitch.toDouble();rawRoll=-e.roll.toDouble()
                pitch=relativeDegrees(rawPitch,basePitch);roll=relativeDegrees(rawRoll,baseRoll)
                if(lastEventMs==0L) Log.i("NeckGuardian","First official HMD pose received: $position pitch=$rawPitch roll=$rawRoll")
                lastEventMs=SystemClock.elapsedRealtime();available=true
            }
        }
        val startResult=provider.start()
        Log.i("NeckGuardian","HMD tracking start result=$startResult support=${provider.supportState} state=${provider.state}")
    }
    fun stop() {provider.stop();collection?.cancel();collection=null;available=false;lastEventMs=0}
    fun calibrate():Boolean {
        if(!fresh) return false
        basePitch=rawPitch;baseRoll=rawRoll;pitch=0.0;roll=0.0
        Log.i("NeckGuardian","Calibrated official HMD pitch=$basePitch roll=$baseRoll")
        return true
    }
}
