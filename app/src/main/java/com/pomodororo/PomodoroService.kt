package com.pomodororo

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.pomodororo.model.PomodoroCycleModel
import kotlinx.coroutines.*

class PomodoroService : Service() {

    companion object {
        const val CHANNEL_ID = "pomodoro_channel"
        const val ALERT_CHANNEL_ID = "pomodoro_alerts"
        const val NOTIFICATION_ID = 1
    }

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private var notificationJob: Job? = null
    private var lastPhase: String? = null

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()

        // Start foreground immediately
        startForeground(
            NOTIFICATION_ID,
            createNotification(PomodoroController.state.value)
        )

        // Observe timer state
        notificationJob = serviceScope.launch {

            PomodoroController.state.collect { model ->

                // Trigger ONLY once when phase changes
                if (lastPhase != null && lastPhase != model.currentPhase) {

                    val oldPhase = lastPhase!!

                    // Update phase BEFORE notifying
                    lastPhase = model.currentPhase

                    // Play sound
                    if (oldPhase == "rest") {
                        playSound("katana")
                    } else {
                        playSound("clash_royale")
                    }

                    // Show notification
                    showPhaseFinishedNotification(oldPhase)

                } else {
                    lastPhase = model.currentPhase
                }

                // Update persistent notification
                val notif = createNotification(model)

                val manager =
                    getSystemService(NotificationManager::class.java)

                manager.notify(NOTIFICATION_ID, notif)
            }
        }
    }

    private fun playSound(name: String) {

        val player = MediaPlayer.create(
            this,
            if (name == "katana")
                R.raw.katana_samurai
            else
                R.raw.clash_royale
        )

        player.setOnCompletionListener {
            it.release()
        }

        player.start()
    }

    private fun showPhaseFinishedNotification(phase: String) {

        val intent = Intent(this, MainActivity::class.java).apply {
            flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
        )

        val text =
            if (phase == "work") {
                "Work session finished. Time to rest."
            } else {
                "Break finished. Back to work."
            }

        val notification =
            NotificationCompat.Builder(this, ALERT_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Pomodoro")
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_ALL)
                .setContentIntent(pendingIntent)
                .build()

        val manager =
            getSystemService(NotificationManager::class.java)

        // Unique ID so notifications don't overwrite
        manager.notify(
            System.currentTimeMillis().toInt(),
            notification
        )
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()

        notificationJob?.cancel()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(
        model: PomodoroCycleModel
    ): Notification {

        val minutes = model.remainingSeconds / 60
        val seconds = model.remainingSeconds % 60

        val timeText =
            "%02d:%02d".format(minutes, seconds)

        val intent = Intent(this, MainActivity::class.java).apply {
            flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(
                "Pomodoro Timer - ${
                    model.currentPhase.replaceFirstChar {
                        it.uppercase()
                    }
                }"
            )
            .setContentText("Time left: $timeText")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {

        val serviceChannel = NotificationChannel(
            CHANNEL_ID,
            "Pomodoro Timer Channel",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description =
                "Pomodoro Timer foreground service"
        }

        val alertChannel = NotificationChannel(
            ALERT_CHANNEL_ID,
            "Pomodoro Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description =
                "Pomodoro session alerts"
        }

        val manager =
            getSystemService(NotificationManager::class.java)

        manager.createNotificationChannel(serviceChannel)
        manager.createNotificationChannel(alertChannel)
    }
}