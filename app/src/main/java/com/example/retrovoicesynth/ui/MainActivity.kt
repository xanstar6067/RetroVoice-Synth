package com.example.retrovoicesynth.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Filter
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.retrovoicesynth.R
import com.example.retrovoicesynth.presets.VoicePreset
import com.example.retrovoicesynth.synth.AudioPlayer
import com.example.retrovoicesynth.synth.RetroVoiceSynth
import com.example.retrovoicesynth.synth.SynthControls
import com.example.retrovoicesynth.ui.theme.ThemePreferences
import com.google.android.material.button.MaterialButton
import com.google.android.material.slider.Slider
import com.google.android.material.textfield.TextInputEditText
import java.util.Locale
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {
    private val synth = RetroVoiceSynth()
    private val audioPlayer = AudioPlayer()
    private val mainHandler = Handler(Looper.getMainLooper())

    private lateinit var inputText: TextInputEditText
    private lateinit var presetDropdown: AutoCompleteTextView
    private lateinit var themeButton: MaterialButton
    private lateinit var speakButton: MaterialButton
    private lateinit var stopButton: MaterialButton
    private lateinit var statusText: TextView
    private lateinit var speedSlider: Slider
    private lateinit var pitchSlider: Slider
    private lateinit var robotnessSlider: Slider
    private lateinit var speedValue: TextView
    private lateinit var pitchValue: TextView
    private lateinit var robotnessValue: TextView
    private lateinit var themePreferences: ThemePreferences

    @Volatile
    private var synthesisRunId = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        themePreferences = ThemePreferences(this)
        themePreferences.applySavedMode()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bindViews()
        configurePresetMenu()
        configureSliders()
        configureActions()
    }

    override fun onDestroy() {
        synthesisRunId += 1
        audioPlayer.stop()
        super.onDestroy()
    }

    private fun bindViews() {
        inputText = findViewById(R.id.inputText)
        presetDropdown = findViewById(R.id.presetDropdown)
        themeButton = findViewById(R.id.themeButton)
        speakButton = findViewById(R.id.speakButton)
        stopButton = findViewById(R.id.stopButton)
        statusText = findViewById(R.id.statusText)
        speedSlider = findViewById(R.id.speedSlider)
        pitchSlider = findViewById(R.id.pitchSlider)
        robotnessSlider = findViewById(R.id.robotnessSlider)
        speedValue = findViewById(R.id.speedValue)
        pitchValue = findViewById(R.id.pitchValue)
        robotnessValue = findViewById(R.id.robotnessValue)
    }

    private fun configurePresetMenu() {
        val names = VoicePreset.all.map { it.displayName }
        presetDropdown.setAdapter(
            PresetAdapter(names)
        )
        presetDropdown.setText(VoicePreset.all.first().displayName, false)
    }

    private inner class PresetAdapter(
        private val names: List<String>
    ) : ArrayAdapter<String>(this, R.layout.item_preset_dropdown, names) {
        private val noFilter = object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                return FilterResults().apply {
                    values = names
                    count = names.size
                }
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) = Unit
        }

        override fun getFilter(): Filter = noFilter
    }

    private fun configureSliders() {
        bindSlider(speedSlider, speedValue, "x%.2f")
        bindSlider(pitchSlider, pitchValue, "x%.2f")
        bindSlider(robotnessSlider, robotnessValue, "%.0f%%") { value -> value * 100f }
    }

    private fun configureActions() {
        updateThemeButton()
        themeButton.setOnClickListener {
            themePreferences.toggleMode()
            updateThemeButton()
        }
        speakButton.setOnClickListener { speak() }
        stopButton.setOnClickListener {
            synthesisRunId += 1
            audioPlayer.stop()
            setIdle()
        }
    }

    private fun speak() {
        val text = inputText.text?.toString().orEmpty().ifBlank {
            getString(R.string.default_phrase)
        }
        val preset = VoicePreset.byName(presetDropdown.text?.toString())
        val controls = SynthControls(
            speed = speedSlider.value,
            pitch = pitchSlider.value,
            robotness = robotnessSlider.value
        )
        val runId = ++synthesisRunId

        audioPlayer.stop()
        setBusy(getString(R.string.status_render))

        thread(name = "vox-80-synth") {
            val samples = synth.synthesize(text, preset, controls)
            mainHandler.post {
                if (runId != synthesisRunId) return@post
                setBusy(getString(R.string.status_speak))
                audioPlayer.play(samples, preset.sampleRate) {
                    mainHandler.post {
                        if (runId == synthesisRunId) {
                            setIdle()
                        }
                    }
                }
            }
        }
    }

    private fun bindSlider(
        slider: Slider,
        label: TextView,
        format: String,
        transform: (Float) -> Float = { it }
    ) {
        val updateLabel = {
            label.text = String.format(Locale.US, format, transform(slider.value))
        }
        updateLabel()
        slider.addOnChangeListener { _, _, _ -> updateLabel() }
    }

    private fun updateThemeButton() {
        val isDarkMode = themePreferences.isDarkMode()
        themeButton.setIconResource(
            if (isDarkMode) R.drawable.ic_theme_light else R.drawable.ic_theme_dark
        )
        themeButton.contentDescription = getString(
            if (isDarkMode) R.string.theme_toggle_light else R.string.theme_toggle_dark
        )
    }

    private fun setBusy(text: String) {
        statusText.text = text
        speakButton.isEnabled = false
        stopButton.isEnabled = true
    }

    private fun setIdle() {
        statusText.text = getString(R.string.status_ready)
        speakButton.isEnabled = true
        stopButton.isEnabled = true
    }
}
