package com.example.neckguardian.domain

import org.junit.Assert.*
import org.junit.Test

class EnvironmentSettingsTest {
    @Test fun firstUseRemainsRealAndQuiet() {
        val settings=FlightEnvironmentSettings()
        assertEquals(TrainingEnvironment.REAL,settings.environmentFor("flight"))
        assertNull(settings.ambientPath("flight"))
    }
    @Test fun damagedOrUnknownPreferenceNeverOpensAnUnexpectedEnvironment() {
        for(value in listOf(null,"", "unknown", "../forest", "FOREST")) assertEquals(TrainingEnvironment.REAL,TrainingEnvironment.fromId(value))
        TrainingEnvironment.entries.forEach {assertEquals(it,TrainingEnvironment.fromId(it.id))}
    }
    @Test fun selectedVirtualFlightNeverLeaksIntoDeskOrMovementActivities() {
        val settings=FlightEnvironmentSettings(TrainingEnvironment.FOREST,true)
        assertEquals(TrainingEnvironment.FOREST,settings.environmentFor("flight"))
        for(kind in listOf("tower","extension","lateral","health","")) {
            assertEquals(TrainingEnvironment.REAL,settings.environmentFor(kind))
            assertNull(settings.ambientPath(kind))
        }
    }
    @Test fun enabledSoundUsesOnlyTheSelectedVirtualFlightAsset() {
        for(environment in TrainingEnvironment.entries) {
            val enabled=FlightEnvironmentSettings(environment,true)
            val expected=if(environment==TrainingEnvironment.REAL) null else "environments/${environment.id}.wav"
            assertEquals(expected,enabled.ambientPath("flight"))
            assertNull(enabled.copy(ambienceEnabled=false).ambientPath("flight"))
        }
    }
    @Test fun everyVirtualEnvironmentUsesItsApprovedModelWithoutPanoramaFallback() {
        assertNull(TrainingEnvironment.REAL.modelAsset)
        assertNull(TrainingEnvironment.REAL.lightingAsset)
        for(environment in listOf(TrainingEnvironment.LAKE,TrainingEnvironment.FOREST,TrainingEnvironment.STUDY)) {
            assertEquals("environments/${environment.id}-v6.glb",environment.modelAsset)
            assertEquals("environments/${environment.id}.png",environment.lightingAsset)
        }
    }
    @Test fun speechDucksAndThenRestoresTheEnvironmentWithoutMutingIt() {
        assertEquals(1f,ambientGain(false),0f)
        assertTrue(ambientGain(true)>0f)
        assertTrue(ambientGain(true)<ambientGain(false))
        assertEquals(1f,ambientGain(false),0f)
    }
}
