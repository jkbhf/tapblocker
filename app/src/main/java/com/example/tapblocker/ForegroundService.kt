package com.example.tapblocker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.Resources
import android.graphics.PixelFormat
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import java.util.concurrent.atomic.AtomicBoolean

class ForegroundService : Service() {
    private var overlayView: View? = null
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var usageStatsManager: UsageStatsManager
    private val isRunning = AtomicBoolean(false)
    private var lastForegroundPackage: String? = null

    override fun onCreate() {
        super.onCreate()
        if (isRunning.compareAndSet(false, true)) {
            createNotificationChannel()
            usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val notification = createNotification()
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            handler.post(checkForegroundApp)
        }
        Log.d("TapBlocker", "Started foreground service")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isRunning.get()) {
            onCreate()
        }
        return START_STICKY
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            "overlay_channel",
            "Overlay Service",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        val builder = NotificationCompat.Builder(this, "overlay_channel")
            .setContentTitle("TapBlocker läuft")
            .setContentText("Overlay ist aktiv")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)

            builder.setChannelId("overlay_channel")
        return builder.build()
    }

    private val checkForegroundApp = object : Runnable {
        override fun run() {
            if (!isRunning.get()) {
                return
            }

            val endTime = System.currentTimeMillis()
            val beginTime = endTime - 10000

            val usageEvents = usageStatsManager.queryEvents(beginTime, endTime)
            var currentForegroundPackage: String? = null
            val event = UsageEvents.Event()

            while (usageEvents.hasNextEvent()) {
                usageEvents.getNextEvent(event)
                if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                    currentForegroundPackage = event.packageName
                }
            }

            val instagramDetected = (currentForegroundPackage == null && lastForegroundPackage == "com.instagram.android") || currentForegroundPackage == "com.instagram.android"

            if (instagramDetected) {
//                Log.d("TapBlocker", "Instagram erkannt")
                if (overlayView == null) showOverlay()
                if(currentForegroundPackage != null) lastForegroundPackage = currentForegroundPackage
            } else {
                if (overlayView != null) removeOverlay()
            }
            handler.postDelayed(this, 1000)
        }
    }


    private fun showOverlay() {
        if (overlayView != null) return

        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        overlayView = inflater.inflate(R.layout.overlay_block, null)

        val height = (Resources.getSystem().displayMetrics.heightPixels * 0.5).toInt()
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            height,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.BOTTOM

        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        try {
            wm.addView(overlayView, params)
            Log.d("TapBlocker", "Overlay activated")
        } catch (e: Exception) {
            Log.e("TapBlocker", "Error showing overlay: ${e.message}")
            overlayView = null
        }
    }

    private fun removeOverlay() {
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        overlayView?.let {
            try {
                wm.removeView(it)
                overlayView = null
                Log.d("TapBlocker", "Overlay removed")
            } catch (e: Exception) {
                Log.e("TapBlocker", "Error removing overlay: ${e.message}")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning.set(false)
        handler.removeCallbacks(checkForegroundApp)
        removeOverlay()
        Log.d("TapBlocker", "Destroyed foreground service")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
