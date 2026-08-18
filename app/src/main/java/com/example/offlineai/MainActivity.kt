package com.example.offlineai

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import org.vosk.android.StorageService

class MainActivity : AppCompatActivity(), RecognitionListener {

    private var speechService: SpeechService? = null
    private var model: Model? = null
    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        statusText = TextView(this).apply {
            text = "Offline AI Assistant Loading..."
            textSize = 20f
            setPadding(40, 40, 40, 40)
        }
        setContentView(statusText)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1)
        } else {
            initModel()
        }
    }

    private fun initModel() {
        StorageService.unpack(this, "model-en-us", "models",
            { loadedModel: Model ->
                model = loadedModel
                startListening()
            },
            { exception: Exception ->
                statusText.text = "Model Load Failed: ${exception.message}"
            }
        )
    }

    private fun startListening() {
        try {
            val recognizer = Recognizer(model, 16000.0f)
            speechService = SpeechService(recognizer, 16000.0f)
            speechService?.startListening(this)
            statusText.text = "AI Listening... Speak commands like 'hello', 'open settings', 'wifi'"
        } catch (e: Exception) {
            statusText.text = "Error starting listening: ${e.message}"
        }
    }

    override fun onResult(hypothesis: String?) {
        if (hypothesis == null) return
        val text = hypothesis.lowercase()
        statusText.text = "Heard: $text"

        // Offline Command Actions
        when {
            text.contains("hello") -> {
                Toast.makeText(this, "Hello! Offline AI Active", Toast.LENGTH_SHORT).show()
            }
            text.contains("open settings") || text.contains("settings") -> {
                startActivity(Intent(Settings.ACTION_SETTINGS))
            }
            text.contains("wifi") -> {
                startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
            }
        }
    }

    override fun onPartialResult(hypothesis: String?) {}
    override fun onFinalResult(hypothesis: String?) {}
    override fun onError(exception: Exception?) { statusText.text = "Error: ${exception?.message}" }
    override fun onTimeout() {}

    override fun onDestroy() {
        super.onDestroy()
        speechService?.stop()
        speechService?.shutdown()
    }
}
