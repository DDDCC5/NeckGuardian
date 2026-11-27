package com.example.neckguardian.platform

import android.content.Context
import com.example.neckguardian.domain.FlightEnvironmentSettings
import com.example.neckguardian.domain.TrainingEnvironment

class EnvironmentPreferences(context:Context) {
    private val prefs=context.getSharedPreferences("environment",Context.MODE_PRIVATE)
    fun read():FlightEnvironmentSettings {
        val environment=try {TrainingEnvironment.fromId(prefs.getString("flight_environment",null))} catch(_:ClassCastException) {TrainingEnvironment.REAL}
        val sound=try {prefs.getBoolean("flight_ambience",false)} catch(_:ClassCastException) {false}
        return FlightEnvironmentSettings(environment,sound)
    }
    fun write(value:FlightEnvironmentSettings) {
        prefs.edit().putString("flight_environment",value.environment.id).putBoolean("flight_ambience",value.ambienceEnabled).apply()
    }
}
