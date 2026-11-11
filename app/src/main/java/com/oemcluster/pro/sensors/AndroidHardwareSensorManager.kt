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
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * Android Donanım Sensör Toplayıcısı ve Çok-Katmanlı GPS Hız Filtresi:
 *  - 400 Hz IMU (İvmeölçer + Jiroskop)
 *  - Yüksek Doğruluklu GNSS Doppler + Haversine Çapraz Doğrulama + Duruş Ölü Bölgesi (Stationary Deadband)
 *  - GnssStatus Uydu Sayısı ve Sinyal Kalitesi (CN0) Takibi
 *  - 48 kHz PCM Mikrofon (UNPROCESSED -> MIC otomatik geri dönüşlü)
 *  - Ortam Işığı Sensörü (Lux)
 */
class AndroidHardwareSensorManager(
    private val context: Context,
    private val scope: CoroutineScope
) : SensorEventListener, LocationListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val mainHandler = Handler(Looper.getMainLooper())

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
    private var audioCaptureJob: Job? = null

    // Çok-Katmanlı GPS Hız Filtresi Durumu (Speedometer Pro / Mad-Location-Manager Mimarisi)
    @Volatile var stationaryDeadbandKmh: Double = 2.2
    @Volatile var maxAllowedGpsErrorMeters: Float = 22.0f
    private var lastValidLocation: Location? = null
    private var warmupFixCount: Int = 0
    private val recentSpeedWindow = DoubleArray(3)
    private var recentSpeedIdx = 0
    private var recentSpeedCount = 0

    private val gnssStatusCallback = object : GnssStatus.Callback() {
        override fun onSatelliteStatusChanged(status: GnssStatus) {
            var inUse = 0
            val total = status.satelliteCount
            for (i in 0 until total) {
                if (status.usedInFix(i)) {
                    inUse++
                }
            }
            NativeTelemetryEngineBridge.liveSatellitesVisible = total
            NativeTelemetryEngineBridge.liveSatellitesInUse = inUse
        }
    }

    /**
     * 400 Hz IMU ve Işık sensörünü başlatır.
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
            recentSpeedCount = 0

            locationManager.registerGnssStatusCallback(gnssStatusCallback, mainHandler)

            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    50L, // 20 Hz'e kadar donanım GPS hızı
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
        } catch (_: Throwable) {
            // İzin veya sağlayıcı hatası durumunda sessizce geç
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
            locationManager.unregisterGnssStatusCallback(gnssStatusCallback)
        } catch (_: Throwable) {
        }
        stopAcousticMicrophone()
    }

    fun setThemeOverrideMode(mode: Int) {
        themeOverrideMode = mode
    }

    fun updateGpsFilterConfig(
        stationaryDeadbandKmh: Double,
        maxAccuracyMeters: Float,
        imuBlendingWeight: Double
    ) {
        this.stationaryDeadbandKmh = stationaryDeadbandKmh.coerceIn(0.5, 8.0)
        this.maxAllowedGpsErrorMeters = maxAccuracyMeters.coerceIn(5.0f, 60.0f)
        NativeTelemetryEngineBridge.nativeSetGpsFilterParameters(
            stationaryDeadbandMs = this.stationaryDeadbandKmh / 3.6,
            imuBlendingWeight = imuBlendingWeight.coerceIn(0.0, 1.0)
        )
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
                lastGyroX = event.values[0].toDouble()
                lastGyroY = event.values[1].toDouble()
                lastGyroZ = event.values[2].toDouble()

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
            Sensor.TYPE_LIGHT -> {
                val lux = event.values[0]
                NativeTelemetryEngineBridge.nativeUpdateAmbientLight(lux, themeOverrideMode)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    /**
     * Çok-Katmanlı Gerçek GPS Hız Filtresi:
     *  1. Yatay Doğruluk (Accuracy) Kapısı: > 22m (veya kullanıcı ayarı) olan zayıf/kapalı alan sapmalarını reddeder.
     *  2. İlk Isınma (Warmup) Kapısı: İlk konum paketini referans noktası olarak alır, sahte sıçrama üretmez.
     *  3. Hata Çemberi & Yer Değiştirme Kontrolü: Konum değişimi GPS hata yarıçapının içindeyse ve hız düşükse
     *     cihaz duruyor kabul edilir (0.0 km/h).
     *  4. Doppler + Haversine Tutarlılık Doğrulaması ve Duruş Ölü Bölgesi (Deadband < 2.2 km/h => 0.0 km/h).
     */
    override fun onLocationChanged(location: Location) {
        val accMeters = if (location.hasAccuracy()) location.accuracy else 99.0f
        NativeTelemetryEngineBridge.liveGpsAccuracyMeters = accMeters

        // Katman 1: Yatay doğruluk çok kötüyse (kapalı alan / zayıf sinyal) hızı sıfırla
        if (accMeters > maxAllowedGpsErrorMeters) {
            return
        }

        val nowElapsedNanos = SystemClock.elapsedRealtimeNanos()
        val locNanos = if (location.elapsedRealtimeNanos > 0L) location.elapsedRealtimeNanos else nowElapsedNanos
        // Eski/bayat konum paketini (3 saniyeden eski) yoksay
        if (nowElapsedNanos - locNanos > 3_000_000_000L) {
            return
        }

        NativeTelemetryEngineBridge.liveHasGpsFix = true
        NativeTelemetryEngineBridge.lastValidGpsLocationNanos = nowElapsedNanos

        val prevLoc = lastValidLocation
        lastValidLocation = location
        warmupFixCount++

        // Katman 2: İlk pakette sadece konum çapası kur, hız = 0
        if (prevLoc == null || warmupFixCount < 2) {
            ingestFilteredSpeedMs(nowElapsedNanos, 0.0, location)
            return
        }

        val dtSec = ((locNanos - prevLoc.elapsedRealtimeNanos) * 1e-9).coerceIn(0.05, 10.0)
        val distanceMeters = prevLoc.distanceTo(location).toDouble()
        val haversineSpeedMs = distanceMeters / dtSec

        // Donanım Doppler hızı varsa önceliklendir, yoksa Haversine hızını kullan
        var candidateSpeedMs = if (location.hasSpeed()) {
            location.speed.toDouble()
        } else {
            haversineSpeedMs
        }

        // Katman 3: Doppler hız doğruluğu (SpeedAccuracy) kontrolü
        val speedAccMs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && location.hasSpeedAccuracy()) {
            location.speedAccuracyMetersPerSecond.toDouble().coerceAtLeast(0.08)
        } else {
            0.25
        }

        // Katman 4: Duruş Hata Çemberi (Stationary Error Circle) kontrolü
        // Cihaz son paketten beri GPS hata dairesinden (örn. max(1.6m, accuracy * 0.32)) dışarı çıkmadıysa
        // ve hız düşükse (< 2.0 m/s = 7.2 km/h), bu tamamen dururken oluşan GPS gezinmesidir (GPS drift)!
        val minRealMovementMeters = max(1.6, accMeters * 0.32)
        if (distanceMeters < minRealMovementMeters && candidateSpeedMs < 2.0) {
            candidateSpeedMs = 0.0
        }

        // Haversine hızı ile Doppler hızı arasında uçurum varsa (örn. dururken anlık Doppler sıçraması)
        if (distanceMeters < 1.2 && candidateSpeedMs < 3.5 && haversineSpeedMs < 0.7) {
            candidateSpeedMs = 0.0
        }

        // Katman 5: Duruş Hız Ölü Bölgesi (Stationary Deadband, varsayılan 2.2 km/h)
        val deadbandMs = stationaryDeadbandKmh / 3.6
        if (candidateSpeedMs < deadbandMs) {
            candidateSpeedMs = 0.0
            recentSpeedCount = 0
        } else {
            // 3-örnekli hafif medyan/ortalama yumuşatma (ani tek-paket sıçramalarını kırpar)
            recentSpeedWindow[recentSpeedIdx] = candidateSpeedMs
            recentSpeedIdx = (recentSpeedIdx + 1) % recentSpeedWindow.size
            if (recentSpeedCount < recentSpeedWindow.size) recentSpeedCount++

            var sum = 0.0
            for (i in 0 until recentSpeedCount) sum += recentSpeedWindow[i]
            candidateSpeedMs = sum / recentSpeedCount
        }

        NativeTelemetryEngineBridge.liveRawGpsSpeedKmh = candidateSpeedMs * 3.6
        ingestFilteredSpeedMs(nowElapsedNanos, candidateSpeedMs, location, speedAccMs)
    }

    private fun ingestFilteredSpeedMs(
        timestampNanos: Long,
        filteredSpeedMs: Double,
        location: Location,
        speedAccMs: Double = 0.20
    ) {
        val bearingRad = if (location.hasBearing()) {
            Math.toRadians(location.bearing.toDouble())
        } else {
            0.0
        }
        val vNorthMs = filteredSpeedMs * cos(bearingRad)
        val vEastMs = filteredSpeedMs * sin(bearingRad)

        NativeTelemetryEngineBridge.nativeIngestGnssDoppler(
            timestampNanos = timestampNanos,
            vNorthMs = vNorthMs,
            vEastMs = vEastMs,
            vDownMs = 0.0,
            speedAccuracyMs = speedAccMs,
            gnssLagSec = 0.065
        )
    }

    @Deprecated("Deprecated in Java")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
}
