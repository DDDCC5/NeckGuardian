package com.example.neckguardian.content

import android.content.Context
import android.os.SystemClock
import androidx.compose.runtime.*
import com.example.neckguardian.domain.*
import com.example.neckguardian.platform.*
import kotlin.math.*
import kotlin.random.Random

object ConsentSession { var accepted by mutableStateOf(false) }
data class FlyingStar(val id:Int,val x:Double,val born:Double)
class GuardianState(context:Context) {
    val pose=PoseSource(context)
    val audio=Guidance(context)
    val history=HistoryStore(context)
    private val environmentPreferences=EnvironmentPreferences(context)
    var environmentSettings by mutableStateOf(environmentPreferences.read()); private set
    var stageEnvironment by mutableStateOf(TrainingEnvironment.REAL); private set
    private var stageAmbientPath:String?=null
    fun chooseEnvironment(value:TrainingEnvironment) {
        if(inStage) return
        environmentSettings=environmentSettings.copy(environment=value)
        environmentPreferences.write(environmentSettings)
    }
    fun chooseAmbience(enabled:Boolean) {
        if(inStage) return
        environmentSettings=environmentSettings.copy(ambienceEnabled=enabled)
        environmentPreferences.write(environmentSettings)
    }
    fun prepareEnvironment(kind:String) {
        stageEnvironment=environmentSettings.environmentFor(kind)
        stageAmbientPath=environmentSettings.ambientPath(kind)
    }
    fun startAmbience() {audio.startAmbience(stageAmbientPath)}
    var inStage by mutableStateOf(false)
    var stageKind by mutableStateOf("tower")
    var modelReady by mutableStateOf(false)
    var recenterRevision by mutableIntStateOf(0)
    var page by mutableStateOf("splash")
    var limit by mutableIntStateOf(15)
    var minutes by mutableIntStateOf(10)
    var grace by mutableIntStateOf(5)
    var answers by mutableStateOf(List(6) { -1 })
    var question by mutableIntStateOf(0)
    var tower:TowerSession?=null
    var glide:GlideSession?=null
    var movement=MovementSession()
    var active by mutableStateOf(false)
    var countdown by mutableIntStateOf(0)
    var tick by mutableLongStateOf(0)
    var result by mutableStateOf<SessionRecord?>(null)
    var message by mutableStateOf("")
    var sceneError by mutableStateOf(false)
    var stars by mutableStateOf<List<FlyingStar>>(emptyList())
    var showQuickStop by mutableStateOf(false)
    var topper by mutableDoubleStateOf(0.0)
    var elapsed by mutableDoubleStateOf(0.0)
    var zh=false
    private var nextStar=0.0
    private var starId=0
    private var lastPitch=0.0
    private var warned=false
    fun explain(kind:String) {if(kind in listOf("tower","flight","extension","lateral")) audio.speak("intro_$kind",zh)}
    fun nav(target:String) {
        audio.stopAmbience();audio.stopVoice();audio.effect("button_click");message="";sceneError=false;page=target
        if(target in listOf("towerSetup","flightSetup","extensionSetup","lateralSetup")) explain(target.removeSuffix("Setup"))
    }
    fun text(cn:String,en:String)=if(zh) cn else en
    fun start(kind:String):Boolean {
        if(!pose.calibrate()) {message=text("当前无法获取头部姿态，请检查头显追踪后重试。","Head tracking is unavailable. Check your headset tracking and try again.");return false}
        tower=if(kind=="tower") TowerSession(limit.toDouble(),minutes*60.0,grace.toDouble()) else null
        glide=if(kind=="flight") GlideSession() else null
        movement=MovementSession();elapsed=0.0;stars=emptyList();nextStar=1.0;starId=0;showQuickStop=false;topper=0.0;warned=false
        page=kind;countdown=3;audio.speak("calibrate",zh);return true
    }
    fun finish(reason:String="ended") {
        if(!active && countdown==0) return
        active=false;countdown=0
        val kind=page
        val metrics=when(kind) {
            "tower" -> tower!!.let { mapOf("average" to it.averageAngle,"completion" to it.completion,"over" to it.totalOverSeconds,"duration" to it.elapsedSeconds) }
            "flight" -> glide!!.let {mapOf("duration" to elapsed,"stars" to it.stars.toDouble(),"missed" to it.missed.toDouble(),"resolved" to it.resolved.toDouble(),"completion" to it.completion)}
            "extension" -> mapOf("extension" to movement.extensionMax,"duration" to elapsed)
            else -> mapOf("left" to movement.leftMax,"right" to movement.rightMax,"duration" to elapsed)
        }
        result=SessionRecord(System.currentTimeMillis(),kind,metrics,reason);history.add(result!!)
        audio.speak(when(reason) {"success" -> "finish_success";"tracking_lost" -> "tracking_lost";else -> "finish_ended"},zh)
        audio.effect(if(reason=="success") "success" else "session_end")
        if(kind=="tower" && reason=="failed") {topper=SystemClock.elapsedRealtime()/1000.0}
        else page="results"
    }
    fun step(dt:Double) {
        tick++
        if(topper>0) { if(SystemClock.elapsedRealtime()/1000.0-topper>4) {topper=0.0;page="results"};return }
        if(!active) return
        if(!pose.fresh) {message=text("追踪中断，本次训练已结束。","Tracking was lost. This session has ended.");finish("tracking_lost");return}
        elapsed+=dt
        when(page) {
            "tower" -> tower?.let {
                it.step(max(0.0,-pose.pitch),dt)
                if(it.episodeOverSeconds>0 && !warned) {audio.speak("over_limit",zh);audio.effect("over_limit");warned=true}
                if(it.episodeOverSeconds==0.0) warned=false
                if(it.finished) {if(!it.success) audio.effect("blocks_topple");finish(if(it.success) "success" else "failed")}
            }
            "flight" -> glide?.let { g ->
                val previousPlaneX=FlightPath.planeX(g.position)
                g.step(pose.roll,dt)
                if(elapsed>=nextStar) {stars=stars+FlyingStar(++starId,Random.nextDouble(-.22,.22),elapsed);nextStar=elapsed+Random.nextDouble(1.5,3.5)}
                stars=stars.filter { s ->
                    val age=elapsed-s.born
                    val hit=FlightPath.touches(s.x-previousPlaneX,FlightPath.starZ((age-dt).coerceAtLeast(0.0))-FlightPath.planeZ,s.x-FlightPath.planeX(g.position),FlightPath.starZ(age)-FlightPath.planeZ)
                    if(hit && g.collect(s.id)) {audio.effect("star_collect");false}
                    else if(FlightPath.hasPassedCollectionZone(FlightPath.starZ(age)) || age>=FlightPath.starLifetime) {g.miss(s.id);false}
                    else true
                }
                if(dt>0 && (pose.pitch-lastPitch)/dt>45 && pose.pitch>8) showQuickStop=true
            }
            "extension","lateral" -> movement.sample(pose.pitch,pose.roll)
        }
        lastPitch=pose.pitch
    }
    fun saveHealth() {
        result=SessionRecord(System.currentTimeMillis(),"health",mapOf("band" to healthBand(answers).toDouble(),"score" to answers.sum().toDouble()),"reference")
        history.add(result!!);page="healthResult"
    }
}
