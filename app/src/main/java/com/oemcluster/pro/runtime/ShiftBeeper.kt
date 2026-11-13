package com.oemcluster.pro.runtime

import android.media.AudioManager
import android.media.ToneGenerator
import com.oemcluster.pro.domain.ShiftLightState

/**
 * Shift Stage 2'ye (renk terslemesi / 8 Hz flaşır) her giriş ve Stage 2'den
 * kesiciye geçişte kısa bir "bip" çalan kask interkomu uyarı sesi.
 *
 * Tekrar tetiği: aynı evreye yeniden girildiğinde (NORMAL→STAGE_2) bir kez
 * çalar; içeride kalındığı sürece bip bip atmaz.
 */
object ShiftBeeper {

    private var tone: ToneGenerator? = null
    private var currentVolume: Int = 80

    fun setVolume(volumePercent: Int) {
        val clamped = volumePercent.coerceIn(0, 100)
        if (clamped != currentVolume) {
            currentVolume = clamped
            release()
        }
    }

    private fun ensureTone(): ToneGenerator? {
        if (currentVolume <= 0) return null
        if (tone == null) {
            tone = runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, currentVolume) }.getOrNull()
        }
        return tone
    }

    /** Evre geçişini kontrol edip gerekirse bip çalar. latestState tarafça saklanır. */
    fun onShiftState(state: ShiftLightState, previous: ShiftLightState) {
        val escalated = state != previous
        if (!escalated) return
        val wantsBeep = state == ShiftLightState.STAGE_2_SHIFT_NOW ||
            state == ShiftLightState.OVER_REV_LIMITER
        if (wantsBeep) beepOnce()
    }

    /** Ayarlar ekranındaki "Bip sesini dene" düğmesi — her koşulda bir kez çalar. */
    fun test() {
        beepOnce()
    }

    private fun beepOnce() {
        if (currentVolume <= 0) return
        runCatching {
            ensureTone()?.startTone(ToneGenerator.TONE_PROP_BEEP, 110)
        }
    }

    fun release() {
        runCatching { tone?.release() }
        tone = null
    }
}
