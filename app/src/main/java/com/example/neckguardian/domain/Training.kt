package com.example.neckguardian.domain

import kotlin.math.*

fun relativeDegrees(value: Double, baseline: Double): Double = ((value - baseline + 540.0) % 360.0) - 180.0
fun <T> recentThirty(records: List<T>): List<T> = records.takeLast(30)
fun healthBand(answers: List<Int>): Int { require(answers.size == 6 && answers.all { it in 0..3 }); return when(answers.sum()) { in 0..5 -> 0; in 6..11 -> 1; else -> 2 } }

class TowerSession(val limit: Double, val targetSeconds: Double, val graceSeconds: Double) {
    var elapsedSeconds = 0.0; private set
    var totalOverSeconds = 0.0; private set
    var episodeOverSeconds = 0.0; private set
    private var angleIntegral = 0.0
    var finished = false; private set
    var success = false; private set
    val averageAngle get() = if (elapsedSeconds == 0.0) 0.0 else angleIntegral / elapsedSeconds
    val completion get() = (elapsedSeconds / targetSeconds).coerceIn(0.0, 1.0)
    init { require(limit in 0.0..30.0 && targetSeconds > 0 && graceSeconds > 0) }
    fun step(downwardAngle: Double, seconds: Double) {
        if (finished || seconds <= 0 || !seconds.isFinite() || !downwardAngle.isFinite()) return
        val over = downwardAngle > limit
        val dt = min(seconds, min(targetSeconds - elapsedSeconds, if (over) graceSeconds - episodeOverSeconds else Double.MAX_VALUE)).coerceAtLeast(0.0)
        elapsedSeconds += dt
        angleIntegral += max(0.0, downwardAngle) * dt
        if (over) { totalOverSeconds += dt; episodeOverSeconds += dt } else episodeOverSeconds = 0.0
        if (episodeOverSeconds >= graceSeconds) finished = true
        else if (elapsedSeconds >= targetSeconds) { finished = true; success = true }
    }
    fun terminate() { finished = true }
}

class GlideSession {
    var elapsedSeconds = 0.0; private set
    var position = 0.0; private set
    private var heldSeconds = 0.0
    private var direction = 0
    private val collected = mutableSetOf<Int>()
    val stars get() = collected.size
    private val missedIds = mutableSetOf<Int>()
    val missed get() = missedIds.size
    val resolved get() = stars + missed
    val completion get() = if(resolved==0) 0.0 else stars.toDouble()/resolved
    fun miss(id:Int) = if(id in collected) false else missedIds.add(id)
    fun step(roll: Double, seconds: Double) {
        if (!roll.isFinite() || !seconds.isFinite() || seconds <= 0) return
        elapsedSeconds += seconds
        val next = if (abs(roll) < 6.0) 0 else if (roll > 0) 1 else -1
        if (next != direction) { heldSeconds = 0.0; direction = next }
        val before = heldSeconds
        heldSeconds += seconds
        val active = (heldSeconds - .5).coerceAtLeast(0.0) - (before - .5).coerceAtLeast(0.0)
        position = (position + next * active * .55 * (abs(roll) / 20).coerceAtMost(1.0)).coerceIn(-1.0, 1.0)
    }
    fun collect(id: Int) = if(id in missedIds) false else collected.add(id)
}

class MovementSession {
    var extensionMax = 0.0; private set
    var leftMax = 0.0; private set
    var rightMax = 0.0; private set
    fun sample(extension: Double, roll: Double) {
        if (!extension.isFinite() || !roll.isFinite()) return
        extensionMax = max(extensionMax, extension.coerceAtLeast(0.0))
        leftMax = max(leftMax, (-roll).coerceAtLeast(0.0))
        rightMax = max(rightMax, roll.coerceAtLeast(0.0))
    }
}

/** Android device-to-world matrix, headset X right / Y up / -Z viewing forward.
 * Positive output pitch is extension; positive roll is a right side bend.
 */
fun headsetAngles(matrix:DoubleArray):Pair<Double,Double> {
    require(matrix.size==9 && matrix.all {it.isFinite()})
    return Math.toDegrees(atan2(-matrix[8],hypot(matrix[6],matrix[7]))) to
        -Math.toDegrees(atan2(matrix[6],matrix[7]))
}

fun oneDecimal(value:Double):String = String.format(java.util.Locale.ROOT,"%.1f",value)
