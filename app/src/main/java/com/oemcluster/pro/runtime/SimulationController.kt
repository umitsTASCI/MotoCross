package com.oemcluster.pro.runtime

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Masaüstü/emülatör test sürüşü. Aynı telemetri hattını besler; yani
 * gösterge, shift-light, vites ve teker analizcisi gerçek motosikletten
 * geliyormuş gibi davranır.
 */
class SimulationController {

    var active by mutableStateOf(false)
    var speedKmh by mutableFloatStateOf(0f)
    var gear by mutableIntStateOf(1)
    var clutchIn by mutableStateOf(false)
    var leanDeg by mutableFloatStateOf(0f)
    var wheelieDeg by mutableFloatStateOf(0f)

    // Teker süresi/mesafesi ve simülasyon devir izi (frame döngüsü tarafından güncellenir)
    var currentWheelieSec by mutableFloatStateOf(0f)
    var currentWheelieMeters by mutableFloatStateOf(0f)
    var wheelieHoldSec by mutableFloatStateOf(0f)
    var maxWheelieDeg: Double = 0.0
    var longestWheelieSec: Double = 0.0
    var longestWheelieMeters: Double = 0.0
    var lastRpm: Double = 0.0
    var filteredRpmRatePerSec: Double = 0.0
    var displayedRpm: Double = 0.0
    var rpmVelocity: Double = 0.0
    var peakHoldRpm: Double = 0.0
    var peakHoldTimerSec: Double = 0.0

    fun start() { active = true }

    fun stop() {
        active = false
        speedKmh = 0f
        gear = 1
        clutchIn = false
        leanDeg = 0f
        wheelieDeg = 0f
        currentWheelieSec = 0f
        currentWheelieMeters = 0f
        wheelieHoldSec = 0f
        maxWheelieDeg = 0.0
        longestWheelieSec = 0.0
        longestWheelieMeters = 0.0
        lastRpm = 0.0
        filteredRpmRatePerSec = 0.0
        displayedRpm = 0.0
        rpmVelocity = 0.0
        peakHoldRpm = 0.0
        peakHoldTimerSec = 0.0
    }

    fun setSpeed(value: Float) {
        active = true
        speedKmh = value.coerceIn(0f, 340f)
        if (value < 1f) gear = if (clutchIn) gear else 0
    }

    fun selectGear(value: Int) {
        active = true
        gear = value.coerceIn(0, 8)
        if (gear > 0) clutchIn = false
        if (gear == 0) clutchIn = true
        if (speedKmh < 8f && gear > 0) speedKmh = 18f + gear * 8f
    }

    fun setLean(value: Float) {
        active = true
        leanDeg = value.coerceIn(-58f, 58f)
        if (speedKmh < 30f && value != 0f) speedKmh = 62f
    }

    fun setWheelie(value: Float) {
        active = true
        wheelieDeg = value.coerceIn(0f, 62f)
        if (speedKmh < 35f && value > 0f) speedKmh = 68f
    }

    fun launchScenario() {
        active = true
        speedKmh = 0f
        gear = 1
        clutchIn = false
        leanDeg = 0f
        wheelieDeg = 0f
    }

    fun canyonScenario() {
        active = true
        speedKmh = 84f
        gear = 3
        clutchIn = false
        leanDeg = 42f
        wheelieDeg = 0f
    }

    fun wheelieScenario() {
        active = true
        speedKmh = 96f
        gear = 2
        clutchIn = false
        leanDeg = 0f
        wheelieDeg = 34f
    }

    fun highSpeedScenario() {
        active = true
        speedKmh = 292f
        gear = 6
        clutchIn = false
        leanDeg = 0f
        wheelieDeg = 0f
    }
}
