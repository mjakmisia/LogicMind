package com.example.logicmind.common

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.example.logicmind.R

/**
 * Menedżer dźwięków dla gier.
 * Używa [SoundPool] do szybkiego odtwarzania krótkich efektów.
 */
object SoundManager {
    private var soundPool: SoundPool? = null
    private val soundMap = mutableMapOf<Int, Int>() // R.raw.id → soundId

    /** Inicjalizuje SoundPool (tylko raz) */
    private fun ensureInitialized(context: Context) {
        if (soundPool != null) return

        val appContext = context.applicationContext
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(10)
            .setAudioAttributes(audioAttributes)
            .build()

        listOf(
            R.raw.explosion,
            R.raw.key_sound_a, R.raw.key_sound_b, R.raw.key_sound_c, R.raw.key_sound_c_higher,
            R.raw.key_sound_d, R.raw.key_sound_e, R.raw.key_sound_f, R.raw.key_sound_g
        ).forEach { loadSound(appContext, it) }
    }

    /** Ładuje dźwięk do SoundPool (lub zwraca już załadowany) */
    private fun loadSound(context: Context, resId: Int): Int {
        return soundMap.getOrPut(resId) {
            soundPool?.load(context, resId, 1) ?: 0
        }
    }

    /** Inicjalizuje menedżer */
    fun init(context: Context) {
        ensureInitialized(context)
    }

    /** Odtwarza dźwięk */
    fun play(context: Context, resId: Int, volume: Float = 1f) {
        ensureInitialized(context)
        val soundId = loadSound(context.applicationContext, resId)
        if (soundId != 0) {
            soundPool?.play(soundId, volume, volume, 1, 0, 1f)
        }
    }

    /** Zwalnia zasoby */
    fun release() {
        soundPool?.release()
        soundPool = null
        soundMap.clear()
    }
}