package com.oemcluster.pro.runtime

import com.oemcluster.pro.data.AppPreferencesRepository
import com.oemcluster.pro.data.ClusterPreferences
import com.oemcluster.pro.domain.UnifiedTelemetryState
import kotlin.math.abs
import kotlin.math.max

/**
 * Mesafe biçimlendirme: hem km hem de mil (MPH modunda) gösterim.
 * Tek doğrulanmış kaynak kilometre deposudur; mil yalnızca görünümdür.
 */
object DistanceFormatter {
    const val KM_TO_MILE = 0.621371192

    fun value(km: Double, useMph: Boolean): Double = if (useMph) km * KM_TO_MILE else km

    fun unit(useMph: Boolean): String = if (useMph) "mi" else "km"

    fun numeric(km: Double, useMph: Boolean, decimals: Int = 1): String =
        String.format(java.util.Locale.US, "%.${decimals}f", value(km, useMph))

    fun format(km: Double, useMph: Boolean, decimals: Int = 1): String {
        val v = value(km, useMph)
        return if (useMph) String.format(java.util.Locale.US, "%.${decimals}f mi", v)
        else String.format(java.util.Locale.US, "%.${decimals}f km", v)
    }
}

/**
 * Ana ekranın seans nesnesine aktivite yaşam döngüsü (onPause/onStop) tarafından
 * erişim sağlar; böylece uygulama arka plana alındığında sayaçlar diske yazılır.
 */
object RideSessionStateHolder {
    @Volatile
    var current: RideSessionState? = null
}

/**
 * Sürüş seansı sayaçları: yol bilgisayarı, tepe değerler, teker rekorları
 * ve drag ölçümleri. Değerler her karede burada birikir; diske yazma
 * seyreltilerek yapılır, böylece 120 Hz döngü engellenmez.
 */
class RideSessionState {

    var tripAKm: Double = 0.0
    var tripBKm: Double = 0.0

    /** Kalıcı toplam kilometre (km). Uygulama her açıldığında bu değer korunur. */
    var odometerKm: Double = 1420.5

    /** Bu açılışa ait seyahat sayacı; uygulama her açıldığında sıfırdan başlar. */
    var sessionKm: Double = 0.0

    var peakSpeedKmh: Double = 0.0
    var peakRpm: Double = 0.0
    var peakLeanLeftDeg: Double = 0.0
    var peakLeanRightDeg: Double = 0.0
    var peakWheelieDeg: Double = 0.0
    var longestWheelieSec: Double = 0.0
    var longestWheelieMeters: Double = 0.0

    var rideTimeSeconds: Double = 0.0
    var avgSpeedKmh: Double = 0.0

    var best0to100Sec: Double = 0.0
    var best0to200Sec: Double = 0.0
    var bestQuarterMileSec: Double = 0.0

    // Canlı drag göstergesi
    var dragActive: Boolean = false
    var dragElapsedSec: Double = 0.0
    var dragDistanceM: Double = 0.0
    var dragSpeedKmh: Double = 0.0
    var last0to100Sec: Double = 0.0
    var lastQuarterMileSec: Double = 0.0

    /** Bir rekor kırıldığında true olur; ana döngü diske yazar. */
    var bestChanged: Boolean = false
        private set

    private var avgDistanceKm: Double = 0.0
    private var avgTimeSec: Double = 0.0

    private var launchRunning: Boolean = false
    private var launchElapsed: Double = 0.0
    private var launchDistanceM: Double = 0.0
    private var launchT100: Double = -1.0
    private var launchT200: Double = -1.0
    private var launchT400: Double = -1.0

    fun loadFrom(prefs: ClusterPreferences) {
        tripAKm = prefs.tripAKm
        tripBKm = prefs.tripBKm
        odometerKm = prefs.odometerKm
        // Seans sayacı her uygulama açılışında sıfırdan başlar; toplam km korunur.
        sessionKm = 0.0
        peakSpeedKmh = prefs.peakSpeedKmh
        peakRpm = prefs.peakRpm
        peakLeanLeftDeg = prefs.peakLeanLeftDeg
        peakLeanRightDeg = prefs.peakLeanRightDeg
        peakWheelieDeg = prefs.peakWheelieDeg
        longestWheelieSec = prefs.longestWheelieSec
        longestWheelieMeters = prefs.longestWheelieMeters
        rideTimeSeconds = prefs.rideTimeSeconds
        best0to100Sec = prefs.best0to100Sec
        best0to200Sec = prefs.best0to200Sec
        bestQuarterMileSec = prefs.bestQuarterMileSec
        avgSpeedKmh = 0.0
        avgDistanceKm = 0.0
        avgTimeSec = 0.0
    }

    /** 120 Hz telemetri karesiyle sayaçları ilerletir. */
    fun update(dtSec: Double, t: UnifiedTelemetryState) {
        val dt = dtSec.coerceIn(0.0, 0.1)
        val moved = t.trueSpeedMs > 0.6
        val dKm = if (moved) t.trueSpeedMs * dt / 1000.0 else 0.0

        tripAKm += dKm
        tripBKm += dKm
        odometerKm += dKm
        sessionKm += dKm
        if (moved) {
            rideTimeSeconds += dt
            avgDistanceKm += dKm
            avgTimeSec += dt
            if (avgTimeSec > 0.5) {
                avgSpeedKmh = avgDistanceKm / (avgTimeSec / 3600.0)
            }
        }

        peakSpeedKmh = max(peakSpeedKmh, t.trueSpeedKmh)
        peakRpm = max(peakRpm, t.actualEngineRpm)
        if (t.leanAngleDeg < 0.0) peakLeanLeftDeg = max(peakLeanLeftDeg, abs(t.leanAngleDeg))
        if (t.leanAngleDeg > 0.0) peakLeanRightDeg = max(peakLeanRightDeg, t.leanAngleDeg)
        peakWheelieDeg = max(peakWheelieDeg, t.maxWheelieAngleDeg)
        longestWheelieSec = max(longestWheelieSec, t.longestWheelieDurationSec)
        longestWheelieMeters = max(longestWheelieMeters, t.longestWheelieDistanceMeters)

        updateDrag(dt, t)
    }

    private fun updateDrag(dt: Double, t: UnifiedTelemetryState) {
        val speed = t.trueSpeedKmh
        dragSpeedKmh = speed

        // Kalkış: duruştan harekete geçiş. 0-100 / 0-200 / çeyrek mil buradan ölçülür.
        if (!launchRunning) {
            if (speed > 0.6 && speed < 8.0) {
                launchRunning = true
                launchElapsed = 0.0
                launchDistanceM = 0.0
                launchT100 = -1.0
                launchT200 = -1.0
                launchT400 = -1.0
                dragActive = true
            }
        } else {
            launchElapsed += dt
            launchDistanceM += t.trueSpeedMs * dt
            dragElapsedSec = launchElapsed
            dragDistanceM = launchDistanceM

            if (launchT100 < 0.0 && speed >= 100.0) {
                launchT100 = launchElapsed
                last0to100Sec = launchT100
                bestChanged = best0to100Sec <= 0.0 || launchT100 < best0to100Sec
                if (bestChanged) best0to100Sec = launchT100
            }
            if (launchT200 < 0.0 && speed >= 200.0) {
                launchT200 = launchElapsed
                if (best0to200Sec <= 0.0 || launchT200 < best0to200Sec) {
                    best0to200Sec = launchT200
                    bestChanged = true
                }
            }
            if (launchT400 < 0.0 && launchDistanceM >= 402.336) {
                launchT400 = launchElapsed
                lastQuarterMileSec = launchT400
                if (bestQuarterMileSec <= 0.0 || launchT400 < bestQuarterMileSec) {
                    bestQuarterMileSec = launchT400
                    bestChanged = true
                }
            }
            // Durunca veya 60 sn sonra ölçümü kapat.
            if ((speed < 0.4 && launchElapsed > 3.0) || launchElapsed > 60.0) {
                launchRunning = false
                dragActive = false
            }
        }
    }

    fun resetTripA() {
        tripAKm = 0.0
        avgDistanceKm = 0.0
        avgTimeSec = 0.0
        avgSpeedKmh = 0.0
        rideTimeSeconds = 0.0
        pushToPrefs()
    }

    fun resetTripB() {
        tripBKm = 0.0
        pushToPrefs()
    }

    /** Kalıcı kilometre sayacını sıfırlar (garaj sıfırlamasından bağımsız). */
    fun resetOdometer() {
        odometerKm = 0.0
        pushToPrefs()
    }

    fun resetPeaks() {
        peakSpeedKmh = 0.0
        peakRpm = 0.0
        peakLeanLeftDeg = 0.0
        peakLeanRightDeg = 0.0
        peakWheelieDeg = 0.0
        longestWheelieSec = 0.0
        longestWheelieMeters = 0.0
        pushToPrefs()
    }

    fun resetDragRecords() {
        best0to100Sec = 0.0
        best0to200Sec = 0.0
        bestQuarterMileSec = 0.0
        last0to100Sec = 0.0
        lastQuarterMileSec = 0.0
        pushToPrefs()
    }

    fun resetAllRideData(keepOdometer: Boolean = true) {
        tripAKm = 0.0
        tripBKm = 0.0
        sessionKm = 0.0
        if (!keepOdometer) odometerKm = 0.0
        peakSpeedKmh = 0.0
        peakRpm = 0.0
        peakLeanLeftDeg = 0.0
        peakLeanRightDeg = 0.0
        peakWheelieDeg = 0.0
        longestWheelieSec = 0.0
        longestWheelieMeters = 0.0
        rideTimeSeconds = 0.0
        avgSpeedKmh = 0.0
        avgDistanceKm = 0.0
        avgTimeSec = 0.0
        best0to100Sec = 0.0
        best0to200Sec = 0.0
        bestQuarterMileSec = 0.0
        last0to100Sec = 0.0
        lastQuarterMileSec = 0.0
        pushToPrefs()
    }

    fun consumeBestChanged(): Boolean {
        if (!bestChanged) return false
        bestChanged = false
        return true
    }

    fun pushToPrefs() {
        AppPreferencesRepository.updateInMemory { p ->
            p.copy(
                tripAKm = tripAKm,
                tripBKm = tripBKm,
                odometerKm = odometerKm,
                peakSpeedKmh = peakSpeedKmh,
                peakRpm = peakRpm,
                peakLeanLeftDeg = peakLeanLeftDeg,
                peakLeanRightDeg = peakLeanRightDeg,
                peakWheelieDeg = peakWheelieDeg,
                longestWheelieSec = longestWheelieSec,
                longestWheelieMeters = longestWheelieMeters,
                rideTimeSeconds = rideTimeSeconds,
                best0to100Sec = best0to100Sec,
                best0to200Sec = best0to200Sec,
                bestQuarterMileSec = bestQuarterMileSec
            )
        }
    }
}
