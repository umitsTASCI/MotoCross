package com.oemcluster.pro.sensors

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.GnssStatus
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Android Donanım Sensör Toplayıcısı (400 Hz IMU + GNSS Doppler + 48 kHz Mikrofon + Işık Sensörü)
 * Tüm verileri sıfır gecikmeyle C++20 NDK motoruna (NativeTelemetryEngineBridge) aktarır.
 */
class AndroidHardwareSensorManager(
    private val context: Context,
    private val scope: CoroutineScope
) : SensorEventListener, LocationListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscope: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val lightSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

    private var lastAccelX = 0.0
    private var lastAccelY = 9.5
    private var lastAccelZ = 2.5

    private var lastGyroX = 0.0
    private var lastGyroY = 0.0
    private var lastGyroZ = 0.0

    private var themeOverrideMode: Int = 0 // 0: Auto Sensor, 1: Always Day, 2: Always Night
    private var lastAmbientLux: Float = 1800f
    private var audioCaptureJob: Job? = null

    // GNSS kalite ve Çok-Katmanlı Hız Filtresi
    @Volatile var stationaryDeadbandKmh: Double = 1.6
    @Volatile var maxAllowedGpsErrorMeters: Float = 35.0f
    private var lastValidLocation: Location? = null
    private var warmupFixCount = 0
    private var lastGnssSpeedMs = -1.0
    private var lastGnssTimestampNanos = 0L
    private var lastGnssBearingDeg = 0.0
    private var lastGnssHorizontalAccuracyM = 99.0
    private var gnssOutlierCount = 0
    private var visibleSatellites = 0
    private var usedSatellites = 0
    private var gnssStatusCallback: GnssStatus.Callback? = null
    private var lastGpsFixElapsedMs = 0L
    private var lastLocationProvider: String? = null

    fun updateGpsFilterConfig(stationaryDeadbandKmh: Double, maxAccuracyMeters: Float) {
        this.stationaryDeadbandKmh = stationaryDeadbandKmh.coerceIn(0.5, 8.0)
        this.maxAllowedGpsErrorMeters = maxAccuracyMeters.coerceIn(8.0f, 60.0f)
    }

    // 400 Hz IMU akışında ivmeölçer ana saat; jiroskop yalnızca son değeri günceller.
    // Böylece aynı örnek iki kez entegre edilmez (eski sürümdeki çift besleme hatası).
    private var lastImuFeedNanos = 0L

    /**
     * 400 Hz (2500 mikrosaniye örnekleme periyodu) IMU ve Işık sensörünü başlatır.
     */
    fun startImuAndLightSensors() {
        accelerometer?.let {
            sensorManager.registerListener(this, it, 2500) // 400 Hz (2500 us)
        }
        gyroscope?.let {
            sensorManager.registerListener(this, it, 2500) // 400 Hz (2500 us)
        }
        lightSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    /**
     * Yüksek frekanslı GNSS (GPS_PROVIDER + FUSED_PROVIDER + GnssStatus) alıcısını başlatır.
     */
    @SuppressLint("MissingPermission")
    fun startGnssUpdates() {
        try {
            warmupFixCount = 0
            lastValidLocation = null
            lastGnssSpeedMs = -1.0
            lastGnssTimestampNanos = 0L
            gnssOutlierCount = 0
            lastGpsFixElapsedMs = 0L
            lastLocationProvider = null

            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                // 20 Hz'e kadar donanım GPS + Doppler hızı (desteklemeyen çipler 1-10 Hz'e düşer)
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    50L,
                    0f,
                    this,
                    Looper.getMainLooper()
                )
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                locationManager.isProviderEnabled(LocationManager.FUSED_PROVIDER)
            ) {
                locationManager.requestLocationUpdates(
                    LocationManager.FUSED_PROVIDER,
                    100L,
                    0f,
                    this,
                    Looper.getMainLooper()
                )
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && gnssStatusCallback == null) {
                val callback = object : GnssStatus.Callback() {
                    override fun onSatelliteStatusChanged(status: GnssStatus) {
                        var visible = 0
                        var used = 0
                        for (i in 0 until status.satelliteCount) {
                            if (status.usedInFix(i)) {
                                used++
                                visible++
                            } else if (status.getCn0DbHz(i) > 18f) {
                                visible++
                            }
                        }
                        visibleSatellites = visible
                        usedSatellites = used
                        NativeTelemetryEngineBridge.nativeUpdateGnssStatus(
                            usedSatellites,
                            lastGnssHorizontalAccuracyM
                        )
                    }
                }
                gnssStatusCallback = callback
                locationManager.registerGnssStatusCallback(callback, Handler(Looper.getMainLooper()))
            }
        } catch (_: SecurityException) {
            // İzin verilmediyse sessizce geç
        } catch (_: Throwable) {
            // Bazı üretici ROM'larında GnssStatus veya FUSED sağlayıcısı patlayabilir; GPS akışı devam eder.
        }
    }

    /**
     * 48 kHz Mikrofon Akustik FFT/HPS motor sesi dinleyicisini başlatır.
     */
    @SuppressLint("MissingPermission")
    fun startAcousticMicrophone48kHz() {
        if (audioCaptureJob?.isActive == true) return

        audioCaptureJob = scope.launch(Dispatchers.IO) {
            val sampleRate = 48000
            val minBufSize = AudioRecord.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_FLOAT
            ).coerceAtLeast(4096 * 4)

            val recorder = createBestAudioRecord(sampleRate, minBufSize) ?: return@launch

            val floatBuffer = FloatArray(1024)
            try {
                NativeTelemetryEngineBridge.nativeSetAcousticEnabled(true)
                recorder.startRecording()
                while (isActive) {
                    val readCount = recorder.read(
                        floatBuffer,
                        0,
                        floatBuffer.size,
                        AudioRecord.READ_BLOCKING
                    )
                    if (readCount > 0) {
                        NativeTelemetryEngineBridge.nativeIngestMicrophonePcm48kHz(
                            floatBuffer,
                            readCount
                        )
                    }
                }
            } finally {
                NativeTelemetryEngineBridge.nativeSetAcousticEnabled(false)
                try {
                    recorder.stop()
                } catch (_: Throwable) {
                }
                recorder.release()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun createBestAudioRecord(sampleRate: Int, minBufSize: Int): AudioRecord? {
        val sources = intArrayOf(
            MediaRecorder.AudioSource.UNPROCESSED,
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            MediaRecorder.AudioSource.MIC
        )
        for (source in sources) {
            try {
                val rec = AudioRecord(
                    source,
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_FLOAT,
                    minBufSize
                )
                if (rec.state == AudioRecord.STATE_INITIALIZED) {
                    return rec
                }
                rec.release()
            } catch (_: Throwable) {
            }
        }
        return null
    }

    fun stopAcousticMicrophone() {
        audioCaptureJob?.cancel()
        audioCaptureJob = null
    }

    fun stopAllSensors() {
        sensorManager.unregisterListener(this)
        try {
            locationManager.removeUpdates(this)
            gnssStatusCallback?.let { locationManager.unregisterGnssStatusCallback(it) }
        } catch (_: SecurityException) {
        } catch (_: Throwable) {
        }
        gnssStatusCallback = null
        stopAcousticMicrophone()
    }

    fun setThemeOverrideMode(mode: Int) {
        themeOverrideMode = mode
        try {
            NativeTelemetryEngineBridge.nativeUpdateAmbientLight(lastAmbientLux, mode)
        } catch (_: UnsatisfiedLinkError) {
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                lastAccelX = event.values[0].toDouble()
                lastAccelY = event.values[1].toDouble()
                lastAccelZ = event.values[2].toDouble()

                NativeTelemetryEngineBridge.nativeIngestImu400Hz(
                    timestampNanos = event.timestamp,
                    axPhone = lastAccelX,
                    ayPhone = lastAccelY,
                    azPhone = lastAccelZ,
                    gxPhone = lastGyroX,
                    gyPhone = lastGyroY,
                    gzPhone = lastGyroZ
                )
            }
            Sensor.TYPE_GYROSCOPE -> {
                // Jiroskop yalnızca önbelleği günceller; native örneklemeyi ivmeölçer
                // zaman damgası sürer. Böylece aynı örnek iki kez entegre edilmez.
                lastGyroX = event.values[0].toDouble()
                lastGyroY = event.values[1].toDouble()
                lastGyroZ = event.values[2].toDouble()
            }
            Sensor.TYPE_LIGHT -> {
                val lux = event.values[0]
                lastAmbientLux = lux
                NativeTelemetryEngineBridge.nativeUpdateAmbientLight(lux, themeOverrideMode)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    /**
     * Çok-Katmanlı Gerçek GPS Hız Filtresi + OOSM Gecikme Telafisi:
     *  1. Yaş Kapısı: 3 sn'den eski veya zamanı geri giden paketler yoksayılır.
     *  2. Yatay Doğruluk Kapısı: [maxAllowedGpsErrorMeters] üzeri zayıf fix reddedilir.
     *  3. İlk Isınma Çapası + Haversine Yedeği: İlk pakette sıçrama önlenir; Doppler yoksa distanceTo/dt kullanılır.
     *  4. Outlier ve Duruş Hata Çemberi: Fiziksel dışı sıçramalar ve park halindeki GPS drift'i sıfırlanır.
     *  5. Duruş Ölü Bölgesi ([stationaryDeadbandKmh]): Düşük hız salınımları 0.0 km/h olarak sabitlenir.
     */
    override fun onLocationChanged(location: Location) {
        val nowNs = SystemClock.elapsedRealtimeNanos()
        val nowMs = SystemClock.elapsedRealtime()
        val provider = location.provider ?: ""

        // GPS_PROVIDER taze iken FUSED_PROVIDER paketlerini karıştırma; aksi halde
        // iki farklı konum kaynağı aynı prevLoc çapasına girip sahte Haversine sıçraması üretir.
        if (provider == LocationManager.GPS_PROVIDER) {
            lastGpsFixElapsedMs = nowMs
        } else if (lastGpsFixElapsedMs > 0L && (nowMs - lastGpsFixElapsedMs) < 2500L) {
            return
        }

        val locNanos = if (location.elapsedRealtimeNanos > 0L) location.elapsedRealtimeNanos else nowNs
        val fixAgeSec = ((nowNs - locNanos) / 1_000_000_000.0).coerceIn(0.0, 10.0)
        if (fixAgeSec > 3.0 || locNanos <= lastGnssTimestampNanos) return

        val horizontalAccuracyM = if (location.hasAccuracy()) {
            location.accuracy.toDouble()
        } else {
            99.0
        }
        lastGnssHorizontalAccuracyM = horizontalAccuracyM
        if (horizontalAccuracyM > maxAllowedGpsErrorMeters.toDouble()) {
            NativeTelemetryEngineBridge.nativeUpdateGnssStatus(
                usedSatellites,
                horizontalAccuracyM
            )
            return
        }

        // Sağlayıcı değiştiyse (ör. GPS ↔ Fused geçişi) konum çapasını temizle.
        val providerSwitched = lastLocationProvider != null && lastLocationProvider != provider
        val prevLoc = if (providerSwitched) null else lastValidLocation
        lastLocationProvider = provider
        lastValidLocation = location
        warmupFixCount = if (providerSwitched) 1 else warmupFixCount + 1

        if (prevLoc == null || warmupFixCount < 2) {
            // İlk pakette yalnızca konum çapası kur; sahte başlangıç sıçramasını engelle.
            lastGnssTimestampNanos = locNanos
            NativeTelemetryEngineBridge.nativeUpdateGnssStatus(usedSatellites, horizontalAccuracyM)
            return
        }

        val prevNanos = if (prevLoc.elapsedRealtimeNanos > 0L) prevLoc.elapsedRealtimeNanos else locNanos
        val dtSec = ((locNanos - prevNanos) / 1_000_000_000.0).coerceIn(0.02, 5.0)
        val distanceMeters = prevLoc.distanceTo(location).toDouble()
        val haversineSpeedMs = distanceMeters / dtSec

        var speedMs = if (location.hasSpeed()) {
            location.speed.toDouble()
        } else {
            haversineSpeedMs
        }
        if (!speedMs.isFinite() || speedMs < 0.0 || speedMs > 105.0) return

        val speedAccMs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            location.hasSpeedAccuracy()
        ) {
            location.speedAccuracyMetersPerSecond.toDouble().coerceIn(0.05, 20.0)
        } else if (location.hasSpeed()) {
            0.35
        } else {
            0.65
        }

        // Outlier kapısı: fiziksel olarak mümkün olmayan ani GPS sıçramalarını reddet.
        val plausibleDelta = 1.5 + 14.0 * dtSec + 3.0 * speedAccMs
        if (lastGnssSpeedMs >= 0.0 && abs(speedMs - lastGnssSpeedMs) > plausibleDelta) {
            gnssOutlierCount++
            if (gnssOutlierCount < 4) return
        } else {
            gnssOutlierCount = 0
        }

        // Duruş hata çemberi: park halindeyken konum hata çemberi içinde gezinmeyi sıfırla.
        val minRealMovementMeters = max(1.6, horizontalAccuracyM * 0.32)
        if (distanceMeters < minRealMovementMeters && speedMs < 2.0) {
            speedMs = 0.0
        }
        if (distanceMeters < 1.2 && speedMs < 3.5 && haversineSpeedMs < 0.7) {
            speedMs = 0.0
        }

        // Duruş ölü bandı (kullanıcı ayarına göre varsayılan 1.6 km/h).
        val deadbandMs = stationaryDeadbandKmh / 3.6
        if (speedMs < deadbandMs) {
            speedMs = 0.0
        }

        val bearingValid = location.hasBearing() && speedMs > 0.6
        if (bearingValid) {
            lastGnssBearingDeg = location.bearing.toDouble()
        }
        val bearingRad = Math.toRadians(lastGnssBearingDeg)
        val vNorthMs = speedMs * cos(bearingRad)
        val vEastMs = speedMs * sin(bearingRad)

        NativeTelemetryEngineBridge.nativeIngestGnssDoppler(
            timestampNanos = locNanos,
            vNorthMs = vNorthMs,
            vEastMs = vEastMs,
            vDownMs = 0.0,
            speedAccuracyMs = speedAccMs,
            gnssLagSec = fixAgeSec.coerceIn(0.0, 1.5)
        )
        NativeTelemetryEngineBridge.nativeUpdateGnssStatus(
            usedSatellites,
            horizontalAccuracyM
        )

        lastGnssSpeedMs = speedMs
        lastGnssTimestampNanos = locNanos
    }

    @Deprecated("Deprecated in Java")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
}

