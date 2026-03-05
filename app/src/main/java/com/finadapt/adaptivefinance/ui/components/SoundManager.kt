package com.finadapt.adaptivefinance.ui.components

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.finadapt.adaptivefinance.R

class SoundEngine(context: Context) {
    private val soundPool: SoundPool

    private val soundMap = mutableMapOf<String, Int>()

    // 🟢 FIXED: Changed from Map<String, Int> to Map<String, List<Int>>
    // This allows tracking multiple simultaneous plays of the same sound
    private val activeStreams = mutableMapOf<String, MutableList<Int>>()

    init {
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(5)
            .setAudioAttributes(attributes)
            .build()

        // Your existing sound loads...
        soundMap["success"] = soundPool.load(context, R.raw.success_chime, 1)
        soundMap["warning"] = soundPool.load(context, R.raw.warning_jolt, 1)
        soundMap["click"] = soundPool.load(context, R.raw.slide_click, 1)
        soundMap["lose"] = soundPool.load(context, R.raw.lose, 1)
        soundMap["level_up"] = soundPool.load(context, R.raw.level_up, 1)
        soundMap["tick1"] = soundPool.load(context, R.raw.bell_tick_tock_timer, 1)
        soundMap["tick2"] = soundPool.load(context, R.raw.clock_tick, 1)
        soundMap["tick3"] = soundPool.load(context, R.raw.clock_tick1, 1)
    }

    fun play(soundName: String) {
        soundMap[soundName]?.let { soundId ->
            // 🟢 FIXED: Add to list instead of overwriting
            val streamId = soundPool.play(soundId, 1f, 1f, 0, 0, 1f)
            activeStreams.getOrPut(soundName) { mutableListOf() }.add(streamId)
        }
    }

    // 🟢 FIXED: Stop all instances of a sound
    fun stop(soundName: String) {
        activeStreams[soundName]?.let { streamIds ->
            streamIds.forEach { streamId ->
                soundPool.stop(streamId)
            }
            streamIds.clear()
        }
    }

    // 🟢 FIXED: A helper to kill all tension sounds instantly
    fun stopAllTicking() {
        stop("tick1")
        stop("tick2")
        stop("tick3")
    }

    fun release() {
        soundPool.release()
    }
}