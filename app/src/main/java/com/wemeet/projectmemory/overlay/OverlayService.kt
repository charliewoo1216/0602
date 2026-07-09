package com.wemeet.projectmemory.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.wemeet.projectmemory.MainActivity
import com.wemeet.projectmemory.ProjectMemoryApplication
import com.wemeet.projectmemory.R
import com.wemeet.projectmemory.stt.SpeechToTextManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Foreground service that keeps a draggable, always-on-top record button
 * alive across every app (Phase 2). Tapping it toggles STT; a finished
 * recognition result is handed straight to DocumentManager's pipeline
 * (Phase 4/5) so a memo gets applied without the user ever opening the app.
 */
class OverlayService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private lateinit var windowManager: WindowManager
    private lateinit var sttManager: SpeechToTextManager
    private var floatingView: TextView? = null
    private lateinit var layoutParams: WindowManager.LayoutParams

    private var projectId: String = ""
    private var isRecording = false

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        sttManager = SpeechToTextManager(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.getStringExtra(EXTRA_PROJECT_ID)?.let { projectId = it }
        startForeground(NOTIFICATION_ID, buildNotification())
        if (floatingView == null) addFloatingButton()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        sttManager.destroy()
        floatingView?.let { runCatching { windowManager.removeView(it) } }
        floatingView = null
    }

    private fun addFloatingButton() {
        val button = createButtonView(recording = false)
        layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayWindowType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 24
            y = 400
        }
        setupDragAndTap(button)
        windowManager.addView(button, layoutParams)
        floatingView = button
    }

    private fun overlayWindowType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

    private fun createButtonView(recording: Boolean): TextView = TextView(this).apply {
        text = "●"
        textSize = 20f
        gravity = android.view.Gravity.CENTER
        setTextColor(if (recording) 0xFFFFFFFF.toInt() else 0xFF8B95A5.toInt())
        val size = (58 * resources.displayMetrics.density).toInt()
        layoutParams = android.widget.FrameLayout.LayoutParams(size, size)
        background = buttonBackground(recording)
    }

    private fun buttonBackground(recording: Boolean): GradientDrawable = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(if (recording) 0xFFE5484D.toInt() else 0xFF1A1D22.toInt())
        setStroke((2 * resources.displayMetrics.density).toInt(), if (recording) 0xFFFF6B6B.toInt() else 0xFF3A4048.toInt())
    }

    private fun setupDragAndTap(view: TextView) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var dragged = false

        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = layoutParams.x
                    initialY = layoutParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    dragged = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (abs(dx) > DRAG_THRESHOLD_PX || abs(dy) > DRAG_THRESHOLD_PX) dragged = true
                    layoutParams.x = initialX + dx
                    layoutParams.y = initialY + dy
                    windowManager.updateViewLayout(view, layoutParams)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!dragged) toggleRecording(view)
                    true
                }
                else -> false
            }
        }
    }

    private fun toggleRecording(button: TextView) {
        isRecording = !isRecording
        button.setTextColor(if (isRecording) 0xFFFFFFFF.toInt() else 0xFF8B95A5.toInt())
        button.background = buttonBackground(isRecording)

        if (isRecording) {
            startRecording(button)
        } else {
            sttManager.stopListening()
        }
    }

    private fun startRecording(button: TextView) {
        if (projectId.isBlank()) {
            Toast.makeText(this, "먼저 프로젝트를 선택해주세요.", Toast.LENGTH_SHORT).show()
            resetButton(button)
            return
        }
        sttManager.startListening(object : SpeechToTextManager.Listener {
            override fun onResult(text: String) {
                resetButton(button)
                handleRecognizedText(text)
            }

            override fun onError(message: String) {
                resetButton(button)
                Toast.makeText(this@OverlayService, message, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun resetButton(button: TextView) {
        isRecording = false
        button.setTextColor(0xFF8B95A5.toInt())
        button.background = buttonBackground(recording = false)
    }

    private fun handleRecognizedText(text: String) {
        val container = (application as ProjectMemoryApplication).container
        serviceScope.launch {
            val result = container.documentManager.processVoiceMemo(projectId, text)
            Toast.makeText(
                this@OverlayService,
                result.userMessage,
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    private fun buildNotification(): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, getString(R.string.notif_channel_overlay), NotificationManager.IMPORTANCE_MIN),
            )
        }
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("녹음 오버레이 실행 중")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .setContentIntent(contentIntent)
            .build()
    }

    companion object {
        const val EXTRA_PROJECT_ID = "extra_project_id"
        private const val CHANNEL_ID = "overlay_channel"
        private const val NOTIFICATION_ID = 1001
        private const val DRAG_THRESHOLD_PX = 12
    }
}
