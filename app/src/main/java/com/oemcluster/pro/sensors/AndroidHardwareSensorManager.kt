package com.oemcluster.pro.sensors

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.cos
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
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    40L, // 25 Hz'e kadar destek
                    0f,
                    this
                )
            }
        } catch (_: SecurityException) {
            // İzin verilmediyse sessizce geç
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
        } catch (_: SecurityException) {
        }
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

    override fun onLocationChanged(location: Location) {
        if (!location.hasSpeed()) return

        val speedMs = location.speed.toDouble()
        val bearingRad = Math.toRadians(location.bearing.toDouble())
        val vNorthMs = speedMs * cos(bearingRad)
        val vEastMs = speedMs * sin(bearingRad)
        val vDownMs = 0.0

        val speedAccMs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && location.hasSpeedAccuracy()) {
            location.speedAccuracyMetersPerSecond.toDouble().coerceAtLeast(0.08)
        } else {
            0.20
        }

        val timestampNanos = location.elapsedRealtimeNanos
        NativeTelemetryEngineBridge.nativeIngestGnssDoppler(
            timestampNanos = timestampNanos,
            vNorthMs = vNorthMs,
            vEastMs = vEastMs,
            vDownMs = vDownMs,
            speedAccuracyMs = speedAccMs,
            gnssLagSec = 0.085
        )
    }

    @Deprecated("Deprecated in Java")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
}

