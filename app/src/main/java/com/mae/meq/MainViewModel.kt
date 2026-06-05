package com.mae.meq

import android.media.audiofx.Equalizer
import androidx.lifecycle.ViewModel

data class Preset(val name: String, val levels: ShortArray)

class MainViewModel : ViewModel() {

    var equalizer: Equalizer? = null
    var eqEnabled = false

    private var storedBandCount = 0
    private var storedRange: Pair<Short, Short> = Pair((-1500).toShort(), 1500.toShort())
    private val savedLevels = ShortArray(5) { 0 }
    private val savedFreqs = IntArray(5) { 0 }

    val bandCount: Int get() = storedBandCount
    val levelRange: Pair<Short, Short> get() = storedRange

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

    fun init() {
        try {
            // Solo leer parámetros del dispositivo, luego liberar para no tocar el audio
            val tempEq = Equalizer(0, 0)
            storedBandCount = tempEq.numberOfBands.toInt()
            storedRange = Pair(tempEq.bandLevelRange[0], tempEq.bandLevelRange[1])
            for (i in 0 until storedBandCount) {
                if (i < savedFreqs.size) savedFreqs[i] = tempEq.getCenterFreq(i.toShort()) / 1000
            }
            tempEq.release()
        } catch (e: Exception) {
            storedBandCount = 0
        }
    }

    fun setEnabled(enabled: Boolean) {
        eqEnabled = enabled
        if (enabled) {
            try {
                equalizer = Equalizer(0, 0).apply {
                    val count = numberOfBands.toInt()
                    for (i in 0 until minOf(count, savedLevels.size)) {
                        setBandLevel(i.toShort(), savedLevels[i])
                    }
                    this.enabled = true
                }
            } catch (e: Exception) {
                equalizer = null
                eqEnabled = false
            }
        } else {
            equalizer?.release()
            equalizer = null
        }
    }

    fun setBandLevel(band: Int, levelMb: Short) {
        if (band < savedLevels.size) savedLevels[band] = levelMb
        equalizer?.setBandLevel(band.toShort(), levelMb)
    }

    fun getBandLevel(band: Int): Short =
        savedLevels.getOrElse(band) { 0 }

    fun getCenterFreqHz(band: Int): Int =
        savedFreqs.getOrElse(band) { 0 }

    fun applyPreset(preset: Preset): ShortArray {
        val count = storedBandCount
        return ShortArray(count) { i ->
            val level = if (i < preset.levels.size) preset.levels[i] else 0
            if (i < savedLevels.size) savedLevels[i] = level
            equalizer?.setBandLevel(i.toShort(), level)
            level
        }
    }

    fun reset(): ShortArray = applyPreset(presets[0])

    override fun onCleared() {
        super.onCleared()
        equalizer?.release()
    }
}
