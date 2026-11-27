package com.example.neckguardian.content

import com.example.neckguardian.domain.TrainingEnvironment
import com.pico.spatial.core.ecs.*
import com.pico.spatial.core.ecs.resource.*
import com.pico.spatial.core.math.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Owns the modeled environment's resources until its Stage root has been destroyed. */
internal class TrainingEnvironmentScene(val entity:Entity,private val resources:List<Resource>):AutoCloseable {
    override fun close() { resources.asReversed().forEach {it.close()} }
}

internal suspend fun createTrainingEnvironment(environment:TrainingEnvironment,owner:Entity):TrainingEnvironmentScene? {
    val asset=environment.modelAsset?:return null
    return loadEnvironmentScene(asset,requireNotNull(environment.lightingAsset),owner)
}

/** Meter-scale authored scene. Unlike a distant panorama, never follow head translation. */
private suspend fun loadEnvironmentScene(asset:String,lightingAsset:String,owner:Entity):TrainingEnvironmentScene {
    val resources=mutableListOf<Resource>()
    var scene:Entity?=null
    try {
        scene=Entity.loadSuspend("asset://$asset")
        lateinit var ibl:TextureResource
        withContext(Dispatchers.IO) {
            ibl=TextureResource.load(lightingAsset,LoadType.FROM_ASSETS,ImageBasedLightingGenerationOption().apply {textureEncoding=TextureEncoding.SRGB})
            resources.add(ibl)
        }
        scene.components.set(StageEnvironmentLightingComponent(ImageBasedLightSource.Single(ibl),8f,Quat()))
        // One warm key light restores modeled surfaces; IBL remains the ambient baseline.
        // Local -Z is the ray direction: authored sun=(-20,35,-25), toward the origin.
        val daylight=Entity()
        daylight.components.set(DirectionalLightComponent(Color4(1f,.93f,.80f,1f),2000f,false))
        daylight.transform().setEulerAngles(EulerAngles(pitch=-48f,yaw=-141.34f))
        scene.addChild(daylight)
        owner.addChild(scene)
        android.util.Log.i("NeckGuardian","Environment geometry loaded: asset=$asset, bounds=${scene.getVisualBounds(owner)}, ibl=${ibl.valid}")
        return TrainingEnvironmentScene(scene,resources)
    } catch(failure:Throwable) {
        scene?.destroy()
        resources.asReversed().forEach {it.close()}
        throw failure
    }
}
