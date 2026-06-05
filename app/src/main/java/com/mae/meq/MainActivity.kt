package com.mae.meq

import android.media.AudioManager
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.mae.meq.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val vm: MainViewModel by viewModels()

    private val seekBars: List<SeekBar> by lazy {
        listOf(binding.band0, binding.band1, binding.band2, binding.band3, binding.band4)
    }
    private val levelLabels: List<TextView> by lazy {
        listOf(binding.level0, binding.level1, binding.level2, binding.level3, binding.level4)
    }
    private val freqLabels: List<TextView> by lazy {
        listOf(binding.freq0, binding.freq1, binding.freq2, binding.freq3, binding.freq4)
    }
    private val bandColumns: List<View> by lazy {
        listOf(binding.col0, binding.col1, binding.col2, binding.col3, binding.col4)
    }

    private var skipPresetCallback = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        vm.init()

        if (vm.equalizer == null) {
            Toast.makeText(this, "Ecualizador no disponible en este dispositivo", Toast.LENGTH_LONG).show()
        }

        setupVolume()
        setupBands()
        setupPresets()
        setupButtons()
    }

    private fun setupVolume() {
        val am = getSystemService(AUDIO_SERVICE) as AudioManager
        val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val curVol = am.getStreamVolume(AudioManager.STREAM_MUSIC)

        binding.seekVolume.max = maxVol
        binding.seekVolume.progress = curVol
        binding.labelVolume.text = "${curVol * 100 / maxVol}%"

        binding.seekVolume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                if (!fromUser) return
                am.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0)
                binding.labelVolume.text = "${progress * 100 / maxVol}%"
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {}
        })
    }

    private fun setupBands() {
        val count = vm.bandCount
        val (minMb, maxMb) = vm.levelRange
        val range = (maxMb - minMb).toInt()

        for (i in 0 until 5) {
            if (i >= count) {
                bandColumns[i].visibility = View.GONE
                continue
            }

            val freqHz = vm.getCenterFreqHz(i)
            freqLabels[i].text = if (freqHz >= 1000) "${freqHz / 1000}k" else "$freqHz"

            val currentLevel = vm.getBandLevel(i)
            seekBars[i].max = range
            seekBars[i].progress = (currentLevel - minMb).toInt()
            levelLabels[i].text = formatDb(currentLevel)

            val bandIndex = i
            seekBars[i].setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                    val levelMb = (progress + minMb).toShort()
                    if (fromUser) vm.setBandLevel(bandIndex, levelMb)
                    levelLabels[bandIndex].text = formatDb(levelMb)
                }
                override fun onStartTrackingTouch(sb: SeekBar) {}
                override fun onStopTrackingTouch(sb: SeekBar) {}
            })
        }
    }

    private fun setupPresets() {
        val names = vm.presets.map { it.name }
        binding.spinnerPreset.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_item, names
        ).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        binding.spinnerPreset.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                if (skipPresetCallback) { skipPresetCallback = false; return }
                val levels = vm.applyPreset(vm.presets[pos])
                applyLevelsToSliders(levels)
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun setupButtons() {
        binding.btnOnOff.setOnClickListener {
            val newState = !vm.eqEnabled
            vm.setEnabled(newState)
            binding.btnOnOff.text = if (newState) getString(R.string.btn_on) else getString(R.string.btn_off)
        }

        binding.btnReset.setOnClickListener {
            skipPresetCallback = true
            binding.spinnerPreset.setSelection(0)
            applyLevelsToSliders(vm.reset())
        }
    }

    private fun applyLevelsToSliders(levels: ShortArray) {
        val (minMb, _) = vm.levelRange
        levels.forEachIndexed { i, level ->
            if (i < seekBars.size) {
                seekBars[i].progress = (level - minMb).toInt()
                levelLabels[i].text = formatDb(level)
            }
        }
    }

    private fun formatDb(levelMb: Short): String {
        val db = levelMb / 100
        return "${if (db > 0) "+" else ""}${db}dB"
    }
}
