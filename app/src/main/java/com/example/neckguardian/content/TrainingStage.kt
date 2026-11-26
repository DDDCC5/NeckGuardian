package com.example.neckguardian.content

import android.os.SystemClock
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.neckguardian.domain.FlightPath
import com.example.neckguardian.domain.oneDecimal
import com.pico.spatial.core.container.SpatialViewContent
import com.pico.spatial.core.ecs.*
import com.pico.spatial.core.ecs.AccessibilityComponent
import com.pico.spatial.core.ecs.resource.ShapeResource
import com.pico.spatial.core.ecs.resource.PhysicsMaterialResource
import com.pico.spatial.core.math.*
import com.pico.spatial.ui.design.*
import com.pico.spatial.ui.foundation.content.setAccessibilityActions
import com.pico.spatial.ui.foundation.content.SpatialView
import com.pico.spatial.ui.foundation.gesture.TargetEntity
import com.pico.spatial.ui.foundation.gesture.detectSpatialDragGesture
import com.pico.spatial.ui.platform.containers.LocalSpatialNavigator
import kotlinx.coroutines.*
import org.json.JSONObject
import kotlin.math.*

/** Mixed Stage: no skybox, real room remains visible, placement uses real HMD pose. */
private class StageSceneHandles {
    var owner: Entity? = null
    var placed: Entity? = null
    var dragTarget: Entity? = null
    var dragEvents: Int = 0
    var panel: Entity? = null
    var content: SpatialViewContent? = null
}

@Composable
fun TrainingStage(s:GuardianState) {
    val context=LocalContext.current
    val navigator=LocalSpatialNavigator.current
    val scope=rememberCoroutineScope()
    val lifecycle=LocalLifecycleOwner.current
    val kind=s.stageKind
    val handles=remember { StageSceneHandles() }
    val modelReady=s.modelReady
    var anchored by remember {mutableStateOf(false)}
    var failed by remember {mutableStateOf(false)}
    var revision by remember {mutableIntStateOf(-1)}
    var returning by remember {mutableStateOf(false)}
    fun leaveTraining() {
        if (returning) { return }
        returning = true
        scope.launch {
            navigator.restoreWindowContainer("NeckGuardianWindow")
            s.inStage=false
            navigator.closeStage()
        }
    }
    DisposableEffect(lifecycle) {
        s.modelReady=false;s.sceneError=false;s.pose.start()
        val observer=LifecycleEventObserver { _,e ->
            if(e==Lifecycle.Event.ON_STOP && !returning) {
                if(s.active || s.countdown>0) s.finish("background")
                s.pose.stop()
            }
            if(e==Lifecycle.Event.ON_START) s.pose.start()
        }
        lifecycle.lifecycle.addObserver(observer)
        onDispose {
            lifecycle.lifecycle.removeObserver(observer)
            if(s.active || s.countdown>0) s.finish("background")
            s.inStage=false;s.pose.stop()
            handles.owner?.destroy();handles.owner=null;s.modelReady=false
        }
    }
    LaunchedEffect(s.pose.available) {
        if(s.pose.available) {
            repeat(3) {
                val hidden=navigator.minimizeWindowContainer("NeckGuardianWindow")
                Log.i("NeckGuardian", "Stage-ready minimize result=$hidden")
                if(hidden) return@LaunchedEffect
                delay(250)
            }
        }
    }
    LaunchedEffect(Unit) {
        var last=SystemClock.elapsedRealtime()
        while(true) {delay(50);val now=SystemClock.elapsedRealtime();s.step((now-last)/1000.0);last=now}
    }
    LaunchedEffect(s.countdown>0) {
        if(s.countdown>0) {
            delay(1800)
            for(n in listOf(3,2,1)) {s.countdown=n;s.audio.speak("countdown_$n",s.zh);delay(1000)}
            s.countdown=0
            if(s.pose.fresh) {
                s.active=true;s.audio.speak(when(kind){"extension"->"extend";"lateral"->"lateral";else->"begin"},s.zh)
            } else {s.message=s.text("未获得有效头显追踪，请重试。","No valid headset tracking. Please retry.")}
        }
    }
    LaunchedEffect(s.page) {if(s.page=="results") leaveTraining()}
    SpatialView(
        modifier=Modifier.fillMaxSize().onSizeChanged {Log.i("NeckGuardian", "Stage host size=$it")}.pointerInput(kind) {
            Log.i("NeckGuardian", "Stage pointer input active kind=$kind")
            detectSpatialDragGesture(
                context=context,
                targetedToEntity=TargetEntity.any { kind=="tower" && it.getName()=="NeckGuardianTowerHit" },
                onDragStart={ _,event ->
                    handles.dragEvents=0
                    Log.i("NeckGuardian", "Tower drag start kind=${event.interactionKind} hit=${event.targetEntity?.getName()}")
                },
                onDragEnd={ event ->Log.i("NeckGuardian", "Tower drag end kind=${event.interactionKind} position=${handles.placed?.transform()?.position}")},
                onDragCancel={ event ->Log.i("NeckGuardian", "Tower drag cancelled kind=${event.interactionKind}")},
            ) { event ->
                val target=handles.placed
                val content=handles.content
                if(target!=null && content!=null) {
                    val d=event.dragAmount
                    val a=content.convertPosition(Vector3.ZERO,ViewCoordinateSpace.Local,content.localSpatialCoordinateSpace)
                    val b=content.convertPosition(Vector3(d.x,d.y,d.z),ViewCoordinateSpace.Local,content.localSpatialCoordinateSpace)
                    target.transform().setPosition(target.transform().position+(b-a))
                    handles.dragEvents++
                    if(handles.dragEvents==1 || handles.dragEvents%20==0) Log.i("NeckGuardian", "Tower drag deltaPx=$d deltaMeters=${b-a} position=${target.transform().position}")
                }
            }
        },
        initial={content,attachments ->
            handles.content=content
            val placementOwner=Entity();val placedSceneRoot=Entity()
            placementOwner.addChild(placedSceneRoot);handles.owner=placementOwner
            try {
                val motion=context.assets.open("models/tower_motion.json").bufferedReader().use{JSONObject(it.readText())}
                val driver=GuardianSceneController(s,kind,placedSceneRoot,motion,world=true)
                val file=when(kind){"tower"->"posture_tower";"flight"->"paper_plane";"extension"->"angle_meter";else->"rudder_gauge"}
                val model=Entity.loadSuspend("asset://models/${if(kind in listOf("tower","flight")) "polished/" else ""}$file.glb");placedSceneRoot.addChild(model)
                if(kind=="extension") model.transform().setScaleVector(Vector3(.8f))
                when(kind) {
                    "tower" -> {
                        model.findEntity("table_top")?.enabled=false
                        (1..6).forEach {i->driver.blocks.add(requireNotNull(model.findEntity("tower_block_${i.toString().padStart(2,'0')}")))}

                    }
                    "flight" -> {driver.plane=model;repeat(5){val star=Entity.loadSuspend("asset://models/star.glb");star.enabled=false;placedSceneRoot.addChild(star);driver.starModels.add(star)}}
                    "extension" -> {
                        for(a in listOf(0,5,10,15,20,25,30,35,40,45,50,55,60)) {
                            val segment = requireNotNull(model.findEntity("meter_segment_${a.toString().padStart(2,'0')}"))
                            segment.components.set(OpacityControllerComponent(1f))
                            driver.segments.add(segment)
                        }
                    }
                    "lateral" -> driver.needle=requireNotNull(model.findEntity("needle_pivot"))
                }
                if(kind=="tower") attachments.entity("training.stability")?.let { halo ->
                    halo.components.set(AccessibilityComponent(label="高塔稳定提示", value="").apply { isAccessibilityElement=false })
                    content.setAccessibilityActions(entity=halo,semanticId="training.world.halo") {}
                    halo.components[AccessibilityComponent::class.java]?.label=s.text("高塔稳定提示","Tower stability decoration")
                    placedSceneRoot.addChild(halo)
                    halo.transform().setPosition(Vector3(0f,.001f,0f))
                    halo.transform().setEulerAngles(EulerAngles(pitch=-90f))
                    halo.transform().setScaleVector(Vector3(.8f))
                }
                if(kind=="flight") attachments.entity("training.wake")?.let { wake ->
                    wake.components.set(AccessibilityComponent(label="纸飞机航迹", value="").apply { isAccessibilityElement=false })
                    content.setAccessibilityActions(entity=wake,semanticId="training.world.wake") {}
                    wake.components[AccessibilityComponent::class.java]?.label=s.text("纸飞机航迹","Aircraft wake decoration")
                    placedSceneRoot.addChild(wake)
                    driver.wake=wake
                    wake.transform().setPosition(Vector3(0f,-.012f,FlightPath.planeZ.toFloat()+.10f))
                    wake.transform().setEulerAngles(EulerAngles(pitch=-90f))
                }
                placementOwner.components.set(SceneDriverComponent(driver))
                val controls = attachments.entity("training.controls")
                if(controls != null) {
                    controls.components.set(
                        AccessibilityComponent(label = "训练控制", value = "").apply {
                            isAccessibilityElement = true
                        }
                    )
                    content.setAccessibilityActions(entity = controls, semanticId = "training.world.controls") {}
                    controls.components[AccessibilityComponent::class.java]?.label = s.text("训练控制", "Training controls")
                    placementOwner.addChild(controls)
                }
                controls?.transform()?.setScaleVector(Vector3(1.5f))
                handles.panel = controls
                val visibleName=when(kind){"tower"->"tower_block_03";"flight"->"wing_left";"extension"->"meter_backing";else->"dial_arc"}
                val visibleMesh=requireNotNull(model.findEntity(visibleName))
                if(kind=="tower") {publishTowerMesh(content,model,placedSceneRoot,s);handles.dragTarget=model}
                else publishInstrumentMesh(content,visibleMesh,s)
                content.addEntity(placementOwner)
                Log.i("NeckGuardian", "Visible owner kind=$kind mesh=${visibleMesh.components[ModelComponent::class.java]!=null} bounds=${visibleMesh.getVisualBounds(placementOwner)}")
                handles.placed=placedSceneRoot;s.modelReady=true
                Log.i("NeckGuardian","Mixed Stage loaded kind=$kind nativeScale=1 tabletopHidden=${kind=="tower"}")
            } catch(e:Exception) {
                if(handles.owner == placementOwner){placementOwner.destroy();handles.owner=null}
                if(e is CancellationException) throw e
                failed=true;s.sceneError=true
                s.message=s.text("场景加载失败，请重新进入训练空间。","Scene loading failed. Please enter the training space again.")
                Log.e("NeckGuardian","Mixed scene failed",e)
                leaveTraining()
            }
        },
        update={_,_ ->
            val root=handles.placed;val origin=handles.owner
            if(root!=null && origin!=null && s.pose.fresh && (!anchored || revision!=s.recenterRevision)) {
                val forward=s.pose.rotation.rotateVector(Vector3(0f,0f,-1f))
                val yaw=Quat(Vector3(0f,1f,0f),atan2(-forward.x,-forward.z))
                val vertical=when(kind){"tower"->-0.15f;"extension"->-0.18f;"lateral"->-0.06f;else->-0.05f}
                val distance=if(kind=="flight") -.8f else -1f
                val target=s.pose.position+yaw.rotateVector(Vector3(0f,vertical,distance))
                root.transform().setPosition(origin.convertPositionFrom(target,null))
                root.transform().setQuaternion(origin.convertRotationFrom(yaw,null))
                val controls=s.pose.position+yaw.rotateVector(Vector3(0f,0f,-1.2f))
                handles.panel?.transform()?.setPosition(origin.convertPositionFrom(controls,null))
                handles.panel?.transform()?.setQuaternion(origin.convertRotationFrom(yaw,null))
                anchored=true;revision=s.recenterRevision
                Log.i("NeckGuardian","Anchored $kind ahead of HMD at $target")
            }
        },
        attachments={
            if(kind=="tower") AttachmentPanel(id="training.stability") { StabilityHalo(s) }
            if(kind=="flight") AttachmentPanel(id="training.wake") { FlightWake() }
            AttachmentPanel(id="training.controls") {
                PicoTheme {
                  GuardianTypography { TrainingDesignPanel(s,kind,failed,::leaveTraining) }
                }
            }
        }
    )
}

