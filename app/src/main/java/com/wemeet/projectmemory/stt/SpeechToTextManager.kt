package com.wemeet.projectmemory.stt

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale

/**
 * Thin wrapper over the framework's online `SpeechRecognizer` (Phase 2).
 * Deliberately does not fall back to an offline language pack — the app's
 * design bets entirely on the LLM to correct STT mistakes using project
 * context/glossary, not on STT accuracy itself.
 */
class SpeechToTextManager(private val context: Context) {

    interface Listener {
        fun onReadyForSpeech() {}
        fun onPartialResult(text: String) {}
        fun onResult(text: String)
        fun onError(message: String)
    }

    private var recognizer: SpeechRecognizer? = null

    fun isNetworkAvailable(): Boolean {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun startListening(listener: Listener) {
        if (!isNetworkAvailable()) {
            listener.onError("네트워크에 연결되어 있지 않아 음성 인식을 사용할 수 없습니다.")
            return
        }
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            listener.onError("이 기기에서는 음성 인식을 사용할 수 없습니다.")
            return
        }

        val speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer = speechRecognizer

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toString())
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }

        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) = listener.onReadyForSpeech()
            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() = Unit

            override fun onError(error: Int) {
                listener.onError(errorMessage(error))
            }

            override fun onResults(results: Bundle) {
                val text = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (text.isNullOrBlank()) {
                    listener.onError("인식된 텍스트가 없습니다.")
                } else {
                    listener.onResult(text)
                }
            }

            override fun onPartialResults(partialResults: Bundle) {
                partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                    ?.let(listener::onPartialResult)
            }

            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })

        speechRecognizer.startListening(intent)
    }

    fun stopListening() {
        recognizer?.stopListening()
    }

    fun destroy() {
        recognizer?.destroy()
        recognizer = null
    }

    private fun errorMessage(error: Int): String = when (error) {
        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
            "네트워크 오류로 음성 인식에 실패했습니다."
        SpeechRecognizer.ERROR_NO_MATCH -> "음성을 인식하지 못했습니다."
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "음성 입력이 없어 종료되었습니다."
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "마이크 권한이 필요합니다."
        SpeechRecognizer.ERROR_AUDIO -> "오디오 녹음 중 오류가 발생했습니다."
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "음성 인식기가 사용 중입니다."
        else -> "음성 인식 중 알 수 없는 오류가 발생했습니다. ($error)"
    }
}
