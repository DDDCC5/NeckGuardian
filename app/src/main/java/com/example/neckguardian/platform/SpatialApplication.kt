package com.example.neckguardian.platform

import android.app.Application
import com.pico.spatial.ui.foundation.dsl.launch
import com.example.neckguardian.mainApp

class SpatialApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        com.pico.spatial.core.ecs.System.register(com.example.neckguardian.content.GuardianSceneSystem::class.java)
        val state = com.example.neckguardian.content.GuardianState(this)
        launch { mainApp(this, state) }
    }
}
