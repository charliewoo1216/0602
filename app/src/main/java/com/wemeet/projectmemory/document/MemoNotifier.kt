package com.wemeet.projectmemory.document

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.wemeet.projectmemory.R
import java.util.concurrent.atomic.AtomicInteger

/** Phase 5: "처리 완료 알림" — a memo finished applying to a document. */
class MemoNotifier(private val context: Context) {

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, context.getString(R.string.notif_channel_processing), NotificationManager.IMPORTANCE_DEFAULT),
            )
        }
    }

    fun notifyApplied(projectName: String, message: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(projectName)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(nextId.incrementAndGet(), notification)
    }

    companion object {
        private const val CHANNEL_ID = "memo_processing_channel"
        private val nextId = AtomicInteger(2000)
    }
}
