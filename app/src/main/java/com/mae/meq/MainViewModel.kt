package com.mae.meq

import android.media.audiofx.Equalizer
import androidx.lifecycle.ViewModel

data class Preset(val name: String, val levels: ShortArray)

class MainViewModel : ViewModel() {

    var equalizer: Equalizer? = null
    var isEnabled = false

    val presets = listOf(
        Preset("Default",    shortArrayOf(0, 0, 0, 0, 0)),
        Preset("Bass Boost", shortArrayOf(800, 400, 0, 0, 0)),
        Preset("Rock",       shortArrayOf(500, 200, 0, 300, 500)),
        Preset("Pop",        shortArrayOf(0, 200, 400, 200, 0)),
        Preset("Jazz",       shortArrayOf(300, 100, 0, 200, 400)),
        Preset("Classical",  shortArrayOf(600, 300, 0, 200, 600)),
        Preset("Vocal",      shortArrayOf(-300, 0, 400, 400, 200)),
        Preset("Flat",       shortArrayOf(0, 0, 0, 0, 0))
    )

    val bandCount: Int get() = equalizer?.numberOfBands?.toInt() ?: 0

    val levelRange: Pair<Short, Short>
        get() = equalizer?.bandLevelRange?.let {
            Pair(it[0], it[1])
        } ?: Pair((-1500).toShort(), 1500.toShort())

    fun init() {
        try {
            equalizer = Equalizer(0, 0).apply { enabled = false }
        } catch (e: Exception) {
            equalizer = null
        }
    }

    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
        equalizer?.enabled = enabled
    }

    fun setBandLevel(band: Int, levelMb: Short) {
        equalizer?.setBandLevel(band.toShort(), levelMb)
    }

    fun getBandLevel(band: Int): Short =
        equalizer?.getBandLevel(band.toShort()) ?: 0

    fun getCenterFreqHz(band: Int): Int =
        (equalizer?.getCenterFreq(band.toShort()) ?: 0) / 1000

    fun applyPreset(preset: Preset): ShortArray {
        val eq = equalizer ?: return ShortArray(5)
        val count = eq.numberOfBands.toInt()
        return ShortArray(count) { i ->
            val level = if (i < preset.levels.size) preset.levels[i] else 0
            eq.setBandLevel(i.toShort(), level)
            level
        }
    }

    fun reset(): ShortArray = applyPreset(presets[0])

    override fun onCleared() {
        super.onCleared()
        equalizer?.release()
    }
}
