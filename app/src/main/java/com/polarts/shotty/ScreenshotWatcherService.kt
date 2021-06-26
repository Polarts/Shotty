package com.polarts.shotty

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.*
import android.os.Build.*
import android.os.Environment.DIRECTORY_DCIM
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File


enum class Actions {
    START,
    STOP
}

class ScreenshotWatcherService : Service() {

    private val tag = "ScreenshotWatcherService"
    private var wakeLock: PowerManager.WakeLock? = null
    private var isServiceStarted = false
    private var fileCreationObserver: ScreenshotFileObserver? = null

    override fun onBind(intent: Intent): IBinder? {
        Log.d(tag, "Some component want to bind with the service")
        // We don't provide binding, so return null
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(tag, "onStartCommand executed with startId: $startId")
        if (intent != null) {
            val action = intent.action
            Log.d(tag, "using an intent with action $action")
            when (action) {
                Actions.START.name -> startService()
                Actions.STOP.name -> stopService()
                else -> Log.d(tag, "This should never happen. No action in the received intent")
            }
        } else {
            Log.d(
                tag,
                "with a null intent. It has been probably restarted by the system."
            )
        }
        val notification = createNotification()
        startForeground(1, notification)
        // by returning this we make sure the service is restarted if the system kills the service
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(tag, "The service has been created".toUpperCase())
        if (VERSION.SDK_INT >= VERSION_CODES.R) {
            fileCreationObserver = ScreenshotFileObserver(this::onObserverFinalize)
        }

        val notification = createNotification()
        startForeground(1, notification)
    }

    private fun onObserverFinalize() {
        if (VERSION.SDK_INT >= VERSION_CODES.Q) {
            fileCreationObserver = ScreenshotFileObserver(this::onObserverFinalize)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(tag, "The service has been destroyed".toUpperCase())
        Toast.makeText(this, "Service destroyed", Toast.LENGTH_SHORT).show()
    }

    override fun onTaskRemoved(rootIntent: Intent) {
        val restartServiceIntent = Intent(applicationContext, ScreenshotWatcherService::class.java).also {
            it.setPackage(packageName)
        };
        val restartServicePendingIntent: PendingIntent = PendingIntent.getService(
            this,
            1,
            restartServiceIntent,
            PendingIntent.FLAG_ONE_SHOT
        );
        applicationContext.getSystemService(Context.ALARM_SERVICE);
        val alarmService: AlarmManager = applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager;
        alarmService.set(
            AlarmManager.ELAPSED_REALTIME,
            SystemClock.elapsedRealtime() + 1000,
            restartServicePendingIntent
        );
    }

    private fun startService() {
        if (isServiceStarted) return
        Log.d(tag, "Starting the foreground service task")
        Toast.makeText(this, "Service starting its task", Toast.LENGTH_SHORT).show()
        isServiceStarted = true
        setServiceState(this, ServiceState.STARTED)

        // we need this lock so our service gets not affected by Doze Mode
        wakeLock =
            (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
                newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ScreenshotWatcherService::lock").apply {
                    acquire()
                }
            }

        GlobalScope.launch(Dispatchers.IO) {
            fileCreationObserver?.let {
                it.startWatching()
            }
        }
    }

    private fun stopService() {
        Log.d(tag, "Stopping the foreground service")
        //Toast.makeText(this, "Service stopping", Toast.LENGTH_SHORT).show()
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                }
            }
            stopForeground(true)
            //stopSelf()
        } catch (e: Exception) {
            Log.d(tag, "Service stopped without being started: ${e.message}")
        }
        isServiceStarted = false
        setServiceState(this, ServiceState.STOPPED)
    }

    private fun createNotification(): Notification {
        val notificationChannelId = "SC WATCHER SERVICE CHANNEL"

        // depending on the Android API that we're dealing with we will have
        // to use a specific method to create the notification
        if (VERSION.SDK_INT >= VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                notificationChannelId,
                "SC Watcher Service notifications channel",
                NotificationManager.IMPORTANCE_HIGH
            ).let {
                it.description = "SC Watcher Service channel"
                it
            }
            notificationManager.createNotificationChannel(channel)
        }

        val pendingIntent: PendingIntent = Intent(this, MainActivity::class.java).let { notificationIntent ->
            PendingIntent.getActivity(this, 0, notificationIntent, 0)
        }

        val builder: Notification.Builder =
            if (VERSION.SDK_INT >= VERSION_CODES.O)
                Notification.Builder(
                    this,
                    notificationChannelId
            ) else Notification.Builder(this)

        return builder
            .setContentTitle("Shotty Service")
            .setContentText("Shotty is watching your screenshots folder")
            .setContentIntent(pendingIntent)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setTicker("Ticker text")
            .setPriority(Notification.PRIORITY_HIGH) // for under android 26 compatibility
            .build()
    }
}