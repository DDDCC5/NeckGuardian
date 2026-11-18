package com.example.neckguardian.domain

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

class TowerPlaybackTest {
    private fun pose(x:Double)=BlockPose(x,.1,0.0,0.0,0.0,0.0,1.0)
    private val track=TowerPlayback((0..120).map {pose(it.toDouble())})
    @Test fun failureStartsAtLastDisplayedPoseWithoutJumpingUpright() {
        assertEquals(pose(8.5),track.falling(0.0,pose(8.5)))
    }
    @Test fun originalLinearTranslationIsInterpolatedBetweenFrames() {
        assertEquals(1.5,track.sample(.05).x,1e-9)
    }
    @Test fun handoffJoinsAuthoredReleaseThenReplaysOriginalSuffix() {
        assertEquals(18.0,track.falling(.4,pose(8.5)).x,1e-9)
        assertEquals(33.0,track.falling(.9,pose(8.5)).x,1e-9)
        assertEquals(120.0,track.falling(20.0,pose(8.5)).x,1e-9)
    }
    @Test fun quaternionInterpolationPreservesRigidWholeBlockRotation() {
        val rotated=BlockPose(0.0,.1,0.0,0.0,0.0,1.0,0.0)
        val playback=TowerPlayback(listOf(pose(0.0),rotated))
        val q=playback.sample(1.0/60)
        assertEquals(sqrt(.5),q.qz,1e-9);assertEquals(sqrt(.5),q.qw,1e-9)
        assertEquals(1.0,q.qx*q.qx+q.qy*q.qy+q.qz*q.qz+q.qw*q.qw,1e-9)
    }
}
