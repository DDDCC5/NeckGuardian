package com.example.neckguardian.domain

import org.junit.Assert.*
import org.junit.Test

class TrainingTest {
    @Test fun recoveryResetsEpisodeButKeepsTotal() {
        val tower = TowerSession(limit = 10.0, targetSeconds = 600.0, graceSeconds = 3.0)
        tower.step(20.0, 2.0)
        tower.step(0.0, 1.0)
        tower.step(20.0, 2.0)
        assertFalse(tower.finished)
        assertEquals(4.0, tower.totalOverSeconds, 0.0001)
        assertEquals(2.0, tower.episodeOverSeconds, 0.0001)
        assertEquals(16.0, tower.averageAngle, 0.0001)
        tower.step(20.0, 1.1)
        assertTrue(tower.finished)
        assertFalse(tower.success)
        assertEquals(0.01, tower.completion, 0.0001)
    }
    @Test fun equalityIsAllowedAndTimerStopsExactlyAtTarget() {
        val tower = TowerSession(10.0, 2.0, 1.0)
        tower.step(10.0, 5.0)
        assertTrue(tower.success)
        assertEquals(2.0, tower.elapsedSeconds, 0.0)
        assertEquals(0.0, tower.totalOverSeconds, 0.0)
    }
    @Test fun sustainedLeanFiltersBriefGesturesAndCollectionIsUnique() {
        val glide = GlideSession()
        glide.step(20.0, .3)
        assertEquals(0.0, glide.position, 0.0)
        glide.step(0.0, .1)
        glide.step(-20.0, .4)
        assertEquals(0.0, glide.position, 0.0)
        glide.step(-20.0, .2)
        assertTrue(glide.position < 0.0)
        assertTrue(glide.collect(1))
        assertFalse(glide.collect(1))
        assertEquals(1, glide.stars)
    }
    @Test fun angleWrapAndIndependentMaxima() {
        assertEquals(2.0, relativeDegrees(-179.0, 179.0), .0001)
        val range = MovementSession()
        range.sample(23.0, -17.0)
        range.sample(8.0, 29.0)
        assertEquals(23.0, range.extensionMax, 0.0)
        assertEquals(17.0, range.leftMax, 0.0)
        assertEquals(29.0, range.rightMax, 0.0)
    }
    @Test fun historyKeepsNewestThirtyAndRiskIsOnlyHeuristic() {
        assertEquals((11..40).toList(), recentThirty((1..40).toList()))
        assertEquals(0, healthBand(List(6) { 0 }))
        assertEquals(1, healthBand(List(6) { 1 }))
        assertEquals(2, healthBand(List(6) { 3 }))
    }
}
