package com.example.neckguardian.content

import android.util.Log
import com.pico.spatial.core.container.SpatialViewContent
import com.pico.spatial.core.ecs.Entity
import com.pico.spatial.core.ecs.HoverEffectComponent
import com.pico.spatial.core.ecs.InteractableComponent
import com.pico.spatial.core.ecs.CollisionComponent
import com.pico.spatial.core.ecs.resource.ShapeResource
import com.pico.spatial.core.ecs.resource.PhysicsMaterialResource
import com.pico.spatial.core.ecs.AccessibilityComponent
import com.pico.spatial.core.math.Vector3
import com.pico.spatial.ui.foundation.content.setAccessibilityActions

/** Interaction belongs to the loaded tower model; operations move its retained placement root. */
internal fun publishTowerMesh(content: SpatialViewContent, mesh: Entity, root: Entity, state: GuardianState) {
    mesh.setName("NeckGuardianTowerHit")
    mesh.components.set(InteractableComponent())
    mesh.components.set(HoverEffectComponent())
    val localBounds=mesh.getVisualBounds(mesh,recursive=true,enabledOnly=true)
    Log.i("NeckGuardian","Tower input bounds local=$localBounds ownerScale=${mesh.transform().scaleVector}")
    mesh.components.set(CollisionComponent(
        collisionShape = listOf(ShapeResource.createBox(localBounds.size).offsetByTranslation(localBounds.center)),
        physicsMaterial = PhysicsMaterialResource()
    ))
    mesh.components.set(
        AccessibilityComponent(label = "高塔", value = "").apply {
            isAccessibilityElement = true
        }
    )
    content.setAccessibilityActions(entity = mesh, semanticId = "training.world.tower") {
        move { position ->
            val destination = mesh.getParent()?.convertPositionTo(position, root.getParent()) ?: position
            val current = mesh.convertPositionTo(Vector3.ZERO, root.getParent())
            root.transform().setPosition(root.transform().position + destination - current)
        }
    }
    mesh.components[AccessibilityComponent::class.java]?.label =
        state.text("可拖拽的稳姿高塔", "Draggable posture tower")
}

internal fun publishInstrumentMesh(content: SpatialViewContent, mesh: Entity, state: GuardianState) {
    mesh.components.set(
        AccessibilityComponent(label = "训练场景", value = "").apply {
            isAccessibilityElement = true
        }
    )
    content.setAccessibilityActions(entity = mesh, semanticId = "training.world.scene") {}
    mesh.components[AccessibilityComponent::class.java]?.label =
        state.text("正前方训练场景", "Training scene straight ahead")
}
