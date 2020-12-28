package com.app.lite.stopwatch

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.provider.Contacts.Intents.Insert.ACTION
import android.provider.ContactsContract.Intents.Insert.ACTION
import android.provider.SyncStateContract
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.app.lite.stopwatch.MainActivity.Companion.timer
import java.util.*


class NotificationService : Service(){

    companion object{
        const val CHANNEL_ID = "STOPWATCH APP ID"
        const val NOTIFICATION_ID = 312312
        const val ACTION_STOP_SERVICE = "ACTION_STOP_SERVICE"
        const val ACTION_START_ACTIVITY = "ACTION_START_ACTIVITY"
        const val ACTION_START_SERVICE = "ACTION_START_SERVICE"
        const val ACTION_PAUSE = "ACTION_PAUSE"
        const val ACTION_RESUME = "ACTION_RESUME"
        const val ACTION_FINISH = "ACTION_FINISH"
    }

    private fun buildNotification(): Notification {
            val startActivityPendingIntent = PendingIntent.getService(this, 0, Intent(this, NotificationService::class.java).apply {
            action = ACTION_START_ACTIVITY
        }, 0)
        val finishPendingIntent = PendingIntent.getService(this, 0, Intent(this, NotificationService::class.java).apply {
            action = ACTION_FINISH
        }, 0)
        val pausePendingIntent = PendingIntent.getService(this, 0, Intent(this, NotificationService::class.java).apply {
            action = ACTION_PAUSE
        }, 0)
        val resumePendingIntent = PendingIntent.getService(this, 0, Intent(this, NotificationService::class.java).apply {
            action = ACTION_RESUME
        }, 0)
        val builder = NotificationCompat.Builder(
            this,
            CHANNEL_ID
        )
            .setSmallIcon(R.drawable.ic_notification)
            .setOnlyAlertOnce(true)
            .setContentIntent(startActivityPendingIntent)
            .addAction(NotificationCompat.Action(0,
                if(MainActivity.isPaused) "Resume" else "Pause",
                if(MainActivity.isPaused) resumePendingIntent else pausePendingIntent))
            .addAction(NotificationCompat.Action(0,
                "Finish",
                finishPendingIntent))
            .setColor(ContextCompat.getColor(this, R.color.colorAccent))
            .setContentTitle(formTimeText(MainActivity.millisCount))

        return builder!!.build()
    }

    override fun onBind(p0: Intent?): IBinder? {
        return Binder()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "App Channel"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance)
            val notificationManager: NotificationManager =
                this.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun formTimeText(timeInMillis: Long): String{
        val seconds = (timeInMillis % MainActivity.millisInMinute)/ MainActivity.millisInSecond
        val minutes = (timeInMillis % MainActivity.millisInHour)/ MainActivity.millisInMinute
        val hours = timeInMillis/ MainActivity.millisInHour

        val secondsPart = if(seconds.toString().length < 2){
            "0".repeat(2 - seconds.toString().length) + seconds.toString()
        }else{
            seconds.toString()
        }

        val minutesPart = if(minutes.toString().length < 2){
            "0".repeat(2 - minutes.toString().length) + minutes.toString()
        }else{
            minutes.toString()
        }

        return "${if(hours != 0L) "$hours:" else ""}${minutesPart}:${secondsPart}"
    }

    private fun stopTimer(){
        timer?.let{
            it.cancel()
            it.purge()
        }
    }

    private fun startTimer(){
        stopTimer()
        timer = Timer()
        timer?.schedule(object : TimerTask() {
            override fun run() {
                if(!MainActivity.isPaused) {
                    MainActivity.millisCount += 1000
                    (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(
                        NOTIFICATION_ID, buildNotification()
                    )
                }
            }
        }, 1000, 1000)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when(intent?.action){
            ACTION_START_SERVICE -> {
                createNotificationChannel()
                startForeground(NOTIFICATION_ID, buildNotification())
                startTimer()
            }
            ACTION_START_ACTIVITY -> {
                startActivity(Intent(this, MainActivity::class.java).apply {
                    addFlags(FLAG_ACTIVITY_NEW_TASK)
                })
                stopTimer()
                stopForeground(true);
                stopSelfResult(startId)
            }
            ACTION_STOP_SERVICE -> {
                stopForeground(true);
                stopSelfResult(startId)
            }
            ACTION_FINISH -> {
                MainActivity.isPaused = true
                MainActivity.isStarted = false
                stopTimer()
                MainActivity.millisCount = 0
                stopForeground(true);
                stopSelfResult(startId)
            }
            ACTION_PAUSE -> {
                MainActivity.isPaused = true
                stopTimer()
                (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(
                    NOTIFICATION_ID, buildNotification()
                )
            }
            ACTION_RESUME -> {
                MainActivity.isPaused = false
                startTimer()
                (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(
                    NOTIFICATION_ID, buildNotification()
                )
            }
        }
        return START_STICKY
    }


}