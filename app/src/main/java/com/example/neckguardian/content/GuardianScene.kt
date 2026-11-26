package com.example.neckguardian.content

import com.example.neckguardian.domain.FlightPath
import com.example.neckguardian.domain.BlockPose
import com.example.neckguardian.domain.TowerPlayback
import android.os.SystemClock
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import com.pico.spatial.core.container.SpatialViewContent
import com.pico.spatial.core.ecs.Entity
import kotlinx.coroutines.CancellationException
import com.pico.spatial.core.ecs.OpacityControllerComponent
import com.pico.spatial.core.ecs.Scene
import com.pico.spatial.core.ecs.Component
import com.pico.spatial.core.ecs.TransformComponent
import com.pico.spatial.core.ecs.AccessibilityComponent
import com.pico.spatial.core.ecs.ViewCoordinateSpace
import com.pico.spatial.core.ecs.SceneUpdateContext
import com.pico.spatial.core.ecs.EntityQueryCondition
import com.pico.spatial.core.ecs.System as SpatialSystem
import com.pico.spatial.core.math.*
import com.pico.spatial.ui.design.Text
import com.pico.spatial.ui.foundation.content.SpatialView
import com.pico.spatial.ui.foundation.content.setAccessibilityActions
import com.pico.spatial.ui.foundation.layout.depth
import androidx.compose.ui.unit.dp
import org.json.JSONObject
import kotlin.math.*

class SceneDriverComponent(val controller:GuardianSceneController):Component()
class GuardianSceneSystem:SpatialSystem() {
    override fun update(context:SceneUpdateContext) {
        context.scene.queryEntity(EntityQueryCondition.hasComponent(SceneDriverComponent::class.java)).forEach {
            it.components[SceneDriverComponent::class.java]?.controller?.update(context.scene)
        }
    }
}
internal fun Entity.transform() = requireNotNull(components[TransformComponent::class.java])

class GuardianSceneController(val s:GuardianState,val kind:String,val root:Entity,val motion:JSONObject,val world:Boolean=false) {
    val blocks=mutableListOf<Entity>()
    var demonstrationModel:Entity?=null
    var plane:Entity?=null
    var wake:Entity?=null
    var needle:Entity?=null
    val segments=mutableListOf<Entity>()
    val starModels=mutableListOf<Entity>()
    private val playback=if(kind=="tower") (1..6).map {i ->
        val track=motion.getJSONObject("tower_block_${i.toString().padStart(2,'0')}")
        val p=track.getJSONArray("translation");val q=track.getJSONArray("rotation")
        TowerPlayback((0 until p.length()/3).map {f ->BlockPose(p.getDouble(f*3),p.getDouble(f*3+1),p.getDouble(f*3+2),q.getDouble(f*4),q.getDouble(f*4+1),q.getDouble(f*4+2),q.getDouble(f*4+3))})
    } else emptyList()
    private val displayed=(0..5).map {i ->BlockPose(0.0,.0175+i*.0351,0.0,0.0,0.0,0.0,1.0)}.toMutableList()
    fun update(scene:Scene) {
        if(!world) {updatePreview();return}
        when(kind) {
            "tower" -> blocks.forEachIndexed { i,b ->
                val pose=if(s.topper>0) {
                    playback[i].falling(SystemClock.elapsedRealtime()/1000.0-s.topper,displayed[i])
                } else {
                    val angle=(if(s.active) max(0.0,-s.pose.pitch) else 0.0).coerceAtMost(60.0)
                    val a=Math.toRadians(angle);val h=.0175+i*.0351
                    BlockPose(h*sin(a),h*cos(a)+.035*sin(a),0.0,0.0,0.0,-sin(a/2),cos(a/2)).also {displayed[i]=it}
                }
                b.transform().setPosition(Vector3(pose.x.toFloat(),pose.y.toFloat(),pose.z.toFloat()))
                b.transform().setQuaternion(Quat(pose.qx.toFloat(),pose.qy.toFloat(),pose.qz.toFloat(),pose.qw.toFloat()))
            }
            "flight" -> {
                val x=FlightPath.planeX(s.glide?.position?:0.0)
                val z=FlightPath.planeZ.toFloat()
                plane?.transform()?.setScaleVector(Vector3(FlightPath.planeScale.toFloat()))
                plane?.transform()?.setPosition(Vector3(x.toFloat(),0f,z))
                wake?.transform()?.setPosition(Vector3(x.toFloat(),-.012f,z+.10f))
                plane?.transform()?.setEulerAngles(EulerAngles(pitch=15f,yaw=FlightPath.planeYaw,roll=(-s.pose.roll*.35).coerceIn(-12.0,12.0).toFloat()))
                starModels.forEachIndexed { i,e ->
                    val st=s.stars.getOrNull(i)
                    e.enabled=st!=null && s.active
                    if(st!=null) {e.transform().setPosition(Vector3(st.x.toFloat(),0f,FlightPath.starZ(s.elapsed-st.born).toFloat()));e.transform().setEulerAngles(EulerAngles(yaw=(s.elapsed*30).toFloat()))}
                }
            }
            "extension" -> segments.forEachIndexed { i,e ->
                // Each authored segment is independently driven. Inactive segments stay visible at a reduced scale.
                val lit=s.pose.pitch>=i*5
                e.components[OpacityControllerComponent::class.java]?.opacity=if(lit) 1f else .18f
            }
            "lateral" -> needle?.transform()?.setEulerAngles(EulerAngles(roll=(-s.pose.roll).toFloat()))
        }
    }
    /** Decorative setup-only timelines. They never read or write pose/session/scoring data. */
    private fun updatePreview() {
        val t=SystemClock.elapsedRealtime()/1000.0
        when(kind) {
            "tower" -> {
                val a=Math.toRadians(14.0*sin(t*.9))
                blocks.forEachIndexed {i,b ->
                    val h=.0175+i*.0351
                    b.transform().setPosition(Vector3((h*sin(a)).toFloat(),(h*cos(a)+.035*abs(sin(a))).toFloat(),0f))
                    b.transform().setQuaternion(Quat(0f,0f,-sin(a/2).toFloat(),cos(a/2).toFloat()))
                }
            }
            "flight" -> {
                val x=(.12*sin(t*.7)).toFloat()
                plane?.transform()?.setPosition(Vector3(x,0f,.1f))
                plane?.transform()?.setEulerAngles(EulerAngles(pitch=15f,yaw=0f,roll=(6*sin(t*.7)).toFloat()))
                starModels.forEachIndexed {i,e ->
                    val age=(t+i*1.1)%4.5
                    e.enabled=i<3 && age<1.7
                    if(e.enabled) {
                        e.transform().setPosition(Vector3(x,0f,(-.12+age*.06).toFloat()))
                        e.transform().setEulerAngles(EulerAngles(yaw=(t*35).toFloat()))
                        e.transform().setScaleVector(Vector3(if(age<1.4) 1f else ((1.7-age)/.3).toFloat().coerceAtLeast(.01f)))
                    }
                }
            }
            "extension" -> {
                demonstrationModel?.transform()?.setPosition(Vector3(0f,(.015*sin(t*1.1)).toFloat(),0f))
                val angle=25+20*sin(t*.8)
                segments.forEachIndexed {i,e ->e.components[OpacityControllerComponent::class.java]?.opacity=if(angle>=i*5) 1f else .18f}
            }
            "lateral" -> needle?.transform()?.setEulerAngles(EulerAngles(roll=(28*sin(t*.85)).toFloat()))
        }
    }

}

private fun accessibleScene(content: SpatialViewContent): Entity {
    val entity = Entity()
    entity.components.set(
        AccessibilityComponent(label = "空间场景", value = "").apply {
            isAccessibilityElement = true
        }
    )
    content.setAccessibilityActions(entity = entity, semanticId = "training.scene") {}
    return entity
}

@Composable
fun ScenePanel(s:GuardianState,kind:String,modifier:Modifier) {
    val context=androidx.compose.ui.platform.LocalContext.current
    val depthPixels=with(androidx.compose.ui.platform.LocalDensity.current) {500.dp.toPx()}
    var loaded by remember(kind) {mutableStateOf(false)}
    var error by remember(kind) {mutableStateOf<String?>(null)}
    var owner by remember(kind) {mutableStateOf<Entity?>(null)}
    var controller by remember(kind) {mutableStateOf<GuardianSceneController?>(null)}
    var measured by remember {mutableStateOf(androidx.compose.ui.unit.IntSize.Zero)}
    var fitted by remember(kind) {mutableStateOf(androidx.compose.ui.unit.IntSize.Zero)}
    DisposableEffect(kind) {
        onDispose {owner?.destroy();owner=null}
    }
    Column(modifier) {
        if(error!=null) {Text(s.text("空间模型加载失败，请返回后重试。","The spatial model could not load. Return and try again."));return@Column}
        if(!loaded) Text(s.text("正在准备空间场景…","Preparing spatial scene…"))
        SpatialView(
            modifier=Modifier.fillMaxWidth().weight(1f).depth(500.dp).onSizeChanged {measured=it},
            initial = { content, _ ->
                val placementOwner = accessibleScene(content)
                owner=placementOwner
                try {
                    val fittedSceneRoot = Entity()
                    placementOwner.addChild(fittedSceneRoot)
                    val motion=context.assets.open("models/tower_motion.json").bufferedReader().use {JSONObject(it.readText())}
                    val driver=GuardianSceneController(s,kind,fittedSceneRoot,motion)
                    val file=when(kind) {"tower"->"posture_tower";"flight"->"paper_plane";"extension"->"angle_meter";else->"rudder_gauge"}
                    val model=Entity.loadSuspend("asset://models/${if(kind in listOf("tower","flight")) "polished/" else ""}$file.glb")
                    fittedSceneRoot.addChild(model)
                    driver.demonstrationModel=model
                    if(kind=="tower") model.findEntity("table_top")?.enabled=false
                    when(kind) {
                        "tower"-> (1..6).forEach {i -> driver.blocks.add(requireNotNull(model.findEntity("tower_block_${i.toString().padStart(2,'0')}")))}
                        "flight"->{driver.plane=model;repeat(5) {val star=Entity.loadSuspend("asset://models/star.glb");star.enabled=false;fittedSceneRoot.addChild(star);driver.starModels.add(star)}}
                        "extension"-> (0..60 step 5).forEach {a ->driver.segments.add(requireNotNull(model.findEntity("meter_segment_${a.toString().padStart(2,'0')}" )).apply { components.set(OpacityControllerComponent(1f)) })}
                        "lateral" -> driver.needle=requireNotNull(model.findEntity("needle_pivot"))
                    }
                    val label=when(kind) {"tower"->s.text("稳姿高塔空间场景","Posture Tower spatial scene");"flight"->s.text("纸飞机与星星空间场景","Paper plane and stars spatial scene");"extension"->s.text("后仰角度状态条","Extension angle meter");else->s.text("左右侧伸仪表","Side bend gauge")}
                    placementOwner.components[AccessibilityComponent::class.java]?.apply { this.label = label; value = s.text("仅作健康参考", "For wellbeing reference only") }
                    placementOwner.components.set(SceneDriverComponent(driver))
                    content.addEntity(placementOwner);owner=placementOwner;controller=driver;loaded=true
                    Log.i("NeckGuardian","GLB loaded kind=$kind blocks=${driver.blocks.size}")
                } catch(e:Exception) {
                    if(owner === placementOwner) { placementOwner.destroy();owner=null }
                    if(e is CancellationException) throw e
                    error=e.message;s.sceneError=true;Log.e("NeckGuardian","Scene load $kind",e)
                }
            },
            update={content,_ ->
                val driver=controller
                if(driver!=null && measured.width>0 && measured.height>0 && fitted!=measured) {
                    val start=content.convertPosition(Vector3.ZERO,ViewCoordinateSpace.Local,content.localSpatialCoordinateSpace)
                    val end=content.convertPosition(Vector3(measured.width.toFloat(),measured.height.toFloat(),depthPixels),ViewCoordinateSpace.Local,content.localSpatialCoordinateSpace)
                    val center=Vector3((start.x+end.x)/2,(start.y+end.y)/2,(start.z+end.z)/2)
                    val half=Vector3(abs(end.x-start.x)*.44f,abs(end.y-start.y)*.44f,abs(end.z-start.z)*.44f)
                    if(half.x>0 && half.y>0 && half.z>0) {
                        val root=driver.root;root.transform().setScaleVector(Vector3(1f));root.transform().setPosition(Vector3.ZERO)
                        val b=root.getVisualBounds(owner,recursive=true,enabledOnly=true)
                        // Accepted full motion envelopes include tabletop, approaching stars and leaning blocks.
                        val envelope=when(kind) {"tower"->Vector3(.28f,.30f,.18f);"flight"->Vector3(.7f,.22f,.38f);"extension"->Vector3(max(.1f,b.halfExtent.x*2),max(.36f,b.halfExtent.y*2),.08f);else->Vector3(max(.3f,b.halfExtent.x*2),max(.2f,b.halfExtent.y*2),.08f)}
                        val scale=min(half.x*2/envelope.x,min(half.y*2/envelope.y,half.z*2/envelope.z))
                        root.transform().setScaleVector(Vector3(scale))
                        // Native 2D content lies on the back of SpatialView. Keep model thickness, but avoid center-depth parallax.
                        root.transform().setPosition(Vector3(center.x-b.center.x*scale,center.y-b.center.y*scale,start.z+.003f+b.halfExtent.z*scale-b.center.z*scale))
                        val finalBounds=root.getVisualBounds(owner,recursive=true,enabledOnly=true)
                        Log.i("NeckGuardian","Scene bounds kind=$kind safe=$half scale=$scale final=$finalBounds")
                        fitted=measured
                    }
                }
            }
        )
    }
}
