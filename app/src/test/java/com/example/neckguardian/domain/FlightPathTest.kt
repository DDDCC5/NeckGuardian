package com.example.neckguardian.domain

import org.junit.Assert.*
import org.junit.Test

class FlightPathTest {
    @Test fun tailFacesWearerAndNoseFacesForward() { assertEquals(0f, FlightPath.planeYaw, 0f) }
    @Test fun contactWithWingCollectsBeforeStarReachesPlaneCenter() {
        // Local coordinates relative to the plane: touching the broad left wing.
        assertTrue(FlightPath.touches(-0.045, 0.02, -0.045, 0.03))
    }
    @Test fun visibleNoseContactIsCollected() {
        assertTrue(FlightPath.touches(0.0, -0.12, 0.0, -0.09))
    }
    @Test fun oldFullSizeWingDoesNotCollectAfterHalfSizeChange() {
        assertFalse(FlightPath.touches(0.10, 0.02, 0.10, 0.05))
    }
    @Test fun missedStarOutsideWingDoesNotCollect() {
        assertFalse(FlightPath.touches(0.18, -0.2, 0.18, 0.2))
    }
    @Test fun sweptContactSurvivesLongFrameAndCollectsOnlyOnce() {
        val glide=GlideSession()
        assertTrue(FlightPath.touches(0.0, -0.4, 0.0, 0.3))
        assertTrue(glide.collect(10));assertFalse(glide.collect(10));assertEquals(1,glide.stars)
    }
}
