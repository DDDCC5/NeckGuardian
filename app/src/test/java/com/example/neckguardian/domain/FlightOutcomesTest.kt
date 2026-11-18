package com.example.neckguardian.domain

import org.junit.Assert.*
import org.junit.Test

class FlightOutcomesTest {
    @Test fun missesAreCountedOnceAndCannotLaterBeCollected() {
        val session=GlideSession()
        assertTrue(session.miss(4));assertFalse(session.miss(4));assertFalse(session.collect(4))
        assertEquals(1,session.missed);assertEquals(0,session.stars)
    }
    @Test fun collectedStarsCannotAlsoBecomeMisses() {
        val session=GlideSession()
        assertTrue(session.collect(2));assertFalse(session.miss(2))
        assertEquals(0,session.missed);assertEquals(1,session.stars)
    }
    @Test fun completionUsesOnlyResolvedOutcomes() {
        val session=GlideSession()
        assertEquals(0.0,session.completion,0.0)
        session.collect(1);session.miss(2);session.miss(3)
        assertEquals(1.0/3,session.completion,1e-9)
        // No outcome was registered for an in-flight star, so it does not affect the denominator.
        session.step(0.0,1.0)
        assertEquals(1.0/3,session.completion,1e-9)
    }
    @Test fun starIsMissedOnlyAfterItsLastPossibleContact() {
        assertFalse(FlightPath.hasPassedCollectionZone(FlightPath.planeZ))
        assertFalse(FlightPath.hasPassedCollectionZone(FlightPath.planeZ+.07))
        assertTrue(FlightPath.hasPassedCollectionZone(FlightPath.planeZ+.09))
    }
}
