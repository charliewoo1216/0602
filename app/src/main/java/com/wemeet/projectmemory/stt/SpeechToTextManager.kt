package com.wemeet.projectmemory.stt

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import java.util.Locale

/**
 * Thin wrapper over the framework's online `SpeechRecognizer` (Phase 2).
 * Deliberately does not fall back to an offline language pack — the app's
 * design bets entirely on the LLM to correct STT mistakes using project
 * context/glossary, not on STT accuracy itself.
 *
 * `SpeechRecognizer` does NOT automatically pick up a connected Bluetooth
 * headset mic — Android keeps using the phone's built-in mic unless the app
 * explicitly opens a SCO (Synchronous Connection-Oriented) audio link. So
 * when a Bluetooth SCO headset is present, we open SCO right before
 * starting recognition and close it right after, instead of holding it open
 * for the overlay's whole lifetime (extra latency/battery otherwise).
 */
class SpeechToTextManager(private val context: Context) {

    interface Listener {
        fun onReadyForSpeech() {}
        fun onPartialResult(text: String) {}
        fun onResult(text: String)
        fun onError(message: String)
    }

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val mainHandler = Handler(Looper.getMainLooper())

    private var recognizer: SpeechRecognizer? = null
    private var scoReceiver: BroadcastReceiver? = null
    private var scoTimeoutRunnable: Runnable? = null
    private var usingBluetoothSco = false

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

        if (hasBluetoothScoHeadset()) {
            connectBluetoothScoThen { beginRecognition(listener) }
        } else {
            beginRecognition(listener)
        }
    }

    private fun hasBluetoothScoHeadset(): Boolean {
        if (!hasBluetoothConnectPermission()) return false
        if (!audioManager.isBluetoothScoAvailableOffCall) return false
        return audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS).any { device ->
            device.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && device.type == AudioDeviceInfo.TYPE_BLE_HEADSET)
        }
    }

    /**
     * BLUETOOTH_CONNECT is a runtime permission from API 31+; below that it's
     * install-time. Rather than forcing the user through another permission
     * prompt just to record a memo, we simply skip SCO and fall back to the
     * phone's built-in mic when it isn't granted.
     */
    private fun hasBluetoothConnectPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED

    private fun connectBluetoothScoThen(onReady: () -> Unit) {
        var settled = false
        fun settle() {
            if (settled) return
            settled = true
            scoTimeoutRunnable?.let(mainHandler::removeCallbacks)
            scoTimeoutRunnable = null
            onReady()
        }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val state = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, AudioManager.SCO_AUDIO_STATE_ERROR)
                if (state == AudioManager.SCO_AUDIO_STATE_CONNECTED) {
                    usingBluetoothSco = true
                    settle()
                } else if (state == AudioManager.SCO_AUDIO_STATE_ERROR || state == AudioManager.SCO_AUDIO_STATE_DISCONNECTED) {
                    settle()
                }
            }
        }
        scoReceiver = receiver
        context.registerReceiver(receiver, IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED))

        @Suppress("DEPRECATION")
        audioManager.startBluetoothSco()
        @Suppress("DEPRECATION")
        audioManager.isBluetoothScoOn = true

        // SCO can fail to connect silently (headset out of range, stale
        // pairing, etc.) — don't block recording forever waiting for it.
        val timeout = Runnable { settle() }
        scoTimeoutRunnable = timeout
        mainHandler.postDelayed(timeout, BLUETOOTH_SCO_TIMEOUT_MS)
    }

    private fun beginRecognition(listener: Listener) {
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
                releaseBluetoothSco()
                listener.onError(errorMessage(error))
            }

            override fun onResults(results: Bundle) {
                releaseBluetoothSco()
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
        releaseBluetoothSco()
    }

    private fun releaseBluetoothSco() {
        scoTimeoutRunnable?.let(mainHandler::removeCallbacks)
        scoTimeoutRunnable = null
        scoReceiver?.let { runCatching { context.unregisterReceiver(it) } }
        scoReceiver = null
        if (usingBluetoothSco) {
            usingBluetoothSco = false
            @Suppress("DEPRECATION")
            audioManager.stopBluetoothSco()
            @Suppress("DEPRECATION")
            audioManager.isBluetoothScoOn = false
        }
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

    companion object {
        private const val BLUETOOTH_SCO_TIMEOUT_MS = 2000L
    }
}
