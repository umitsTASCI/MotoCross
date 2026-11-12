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
    private var audioCaptureJob: Job? = null

    // GNSS kalite ve outlier takibi
    private var lastGnssSpeedMs = -1.0
    private var lastGnssTimestampNanos = 0L
    private var lastGnssBearingDeg = 0.0
    private var lastGnssHorizontalAccuracyM = 99.0
    private var gnssOutlierCount = 0
    private var visibleSatellites = 0
    private var usedSatellites = 0
    private var gnssStatusCallback: GnssStatus.Callback? = null

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
     * GNSS (Dahili GPS veya Harici Bluetooth 10Hz-25Hz GNSS) Doppler alıcısını başlatır.
     */
    @SuppressLint("MissingPermission")
    fun startGnssUpdates() {
        try {
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) return

            // 10 Hz hedef, minimum 100 ms: 25 Hz istemek pil tüketimini artırır ve
            // çoğu telefonda zaten desteklenmez. Doppler hızı için 5-10 Hz yeterlidir.
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                100L,
                0f,
                this,
                Looper.getMainLooper()
            )

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
            // Bazı üretici ROM'larında GnssStatus çağrısı patlayabilir; GPS akışı devam eder.
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

            val recorder = try {
                AudioRecord(
                    MediaRecorder.AudioSource.UNPROCESSED,
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_FLOAT,
                    minBufSize
                )
            } catch (_: Throwable) {
                null
            } ?: return@launch

            if (recorder.state != AudioRecord.STATE_INITIALIZED) {
                recorder.release()
                return@launch
            }

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
                NativeTelemetryEngineBridge.nativeUpdateAmbientLight(lux, themeOverrideMode)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    override fun onLocationChanged(location: Location) {
        // 1) Yaş kontrolü: bayat fix'ler hızı geçmişe doğru çeker.
        val nowNs = SystemClock.elapsedRealtimeNanos()
        val fixAgeSec = ((nowNs - location.elapsedRealtimeNanos) / 1_000_000_000.0)
            .coerceIn(0.0, 10.0)
        if (location.elapsedRealtimeNanos <= 0L || fixAgeSec > 3.0) return

        // 2) Yatay doğruluk kapısı: 60 m üzeri fix bisiklet hızı için anlamsızdır.
        val horizontalAccuracyM = if (location.hasAccuracy()) {
            location.accuracy.toDouble()
        } else {
            99.0
        }
        lastGnssHorizontalAccuracyM = horizontalAccuracyM
        if (horizontalAccuracyM > 60.0) {
            NativeTelemetryEngineBridge.nativeUpdateGnssStatus(
                usedSatellites,
                horizontalAccuracyM
            )
            return
        }

        // 3) Doppler hızı yoksa manyetik burun/konum türeviyle uydurma yapma.
        if (!location.hasSpeed()) return

        var speedMs = location.speed.toDouble()
        if (!speedMs.isFinite() || speedMs < 0.0 || speedMs > 100.0) return

        val speedAccMs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            location.hasSpeedAccuracy()
        ) {
            location.speedAccuracyMetersPerSecond.toDouble().coerceIn(0.05, 20.0)
        } else {
            // Eski API'lerde Doppler doğruluğu yok; muhafazakâr varsayım.
            0.35
        }

        // 4) Outlier kapısı: fiziksel olarak mümkün olmayan sıçramaları reddet.
        val dtSec = if (lastGnssTimestampNanos > 0L) {
            ((location.elapsedRealtimeNanos - lastGnssTimestampNanos) / 1_000_000_000.0)
                .coerceIn(0.02, 5.0)
        } else {
            0.2
        }
        val plausibleDelta = 1.5 + 14.0 * dtSec + 3.0 * speedAccMs
        if (lastGnssSpeedMs >= 0.0 && abs(speedMs - lastGnssSpeedMs) > plausibleDelta) {
            gnssOutlierCount++
            // Tek tük GPS sıçraması reddedilir; 4 ardışık tutarlı örnek gerçeği yener.
            if (gnssOutlierCount < 4) return
        } else {
            gnssOutlierCount = 0
        }

        // 5) Duruş ölü bandı: GPS sıfır civarında salınır, ekranda 0-1 km/h titremesin.
        if (speedMs < 0.45 && speedAccMs < 2.5) {
            speedMs = 0.0
        }

        // 6) Burun açısı: düşük hızda ve bearing'siz fix'lerde son geçerli yönü koru.
        val bearingValid = location.hasBearing() && speedMs > 0.6
        if (bearingValid) {
            lastGnssBearingDeg = location.bearing.toDouble()
        }
        val bearingRad = Math.toRadians(lastGnssBearingDeg)
        val vNorthMs = speedMs * cos(bearingRad)
        val vEastMs = speedMs * sin(bearingRad)

        NativeTelemetryEngineBridge.nativeIngestGnssDoppler(
            timestampNanos = location.elapsedRealtimeNanos,
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
        lastGnssTimestampNanos = location.elapsedRealtimeNanos
    }

    @Deprecated("Deprecated in Java")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
}

