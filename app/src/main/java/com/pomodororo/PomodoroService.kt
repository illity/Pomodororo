package com.pomodororo

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
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
        const val NOTIFICATION_ID = 1
    }

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var notificationJob: Job? = null
    private var lastPhase: String? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        // ⚡ Start foreground immediately with current state
        startForeground(NOTIFICATION_ID, createNotification(PomodoroController.state.value))

        // ⚡ Observe shared state and update notification every second
        notificationJob = serviceScope.launch {
            PomodoroController.state.collect { model ->
                if (lastPhase != null && lastPhase != model.currentPhase) {
                    if (lastPhase == "rest") playSound("katana") else playSound("clash_royale")
                }
                lastPhase = model.currentPhase

                val notif = createNotification(model)
                val manager = getSystemService(NotificationManager::class.java)
                manager.notify(NOTIFICATION_ID, notif)
            }
        }
    }


    private fun playSound(name: String) {
        val player = MediaPlayer.create(this,
            if (name == "katana") R.raw.katana_samurai else R.raw.clash_royale
        )
        player.setOnCompletionListener {
            it.release()
        }
        player.start()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // START_STICKY ensures the service restarts if the system kills it
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        notificationJob?.cancel()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(model: PomodoroCycleModel): Notification {
        val minutes = model.remainingSeconds / 60
        val seconds = model.remainingSeconds % 60
        val timeText = "%02d:%02d".format(minutes, seconds)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Pomodoro Timer - ${model.currentPhase.replaceFirstChar { it.uppercase() }}")
            .setContentText("Time left: $timeText")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // replace with your icon
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Pomodoro Timer Channel",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Pomodoro Timer foreground service" }

            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }
}
