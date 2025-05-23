package com.example.tapblocker

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageEvents.Event.MOVE_TO_FOREGROUND
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
import com.example.tapblocker.data.AppDatabase
import com.example.tapblocker.data.RegionEntity
import com.example.tapblocker.data.RegionOrientation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class ForegroundService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var usageStatsManager: UsageStatsManager
    private val isRunning = AtomicBoolean(false)
    private var lastForegroundPackage: String? = null
    private var monitoredRegions: List<RegionEntity> = emptyList()

    // Verwende Long als Schlüsseltyp, da regionId vom Typ Long ist
    private val regionOverlays = mutableMapOf<Long, View>()
    private val ioScope = CoroutineScope(Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        if (isRunning.compareAndSet(false, true)) {
            createNotificationChannel()
            usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            observeDatabase()
            val notification = createNotification()
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            handler.post(checkForegroundApp)
        }
        Log.d("TapBlocker", "Started foreground service")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isRunning.get()) onCreate()
        return START_STICKY
    }

    private fun observeDatabase() {
        val dao = AppDatabase.getInstance(applicationContext).appSettingsDao()
        ioScope.launch {
            dao.getAllSettings().collectLatest { list ->
                val active = list.filter { it.app.activated }
                monitoredRegions = active.flatMap { it.regions }
                Log.d(
                    "TapBlocker",
                    "Loaded ${monitoredRegions.size} regions from DB: $monitoredRegions"
                )
            }
        }
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

    @SuppressLint("SuspiciousIndentation")
    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, "overlay_channel")
            .setContentTitle("TapBlocker is running")
            .setContentText("Overlay is active")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private val checkForegroundApp = object : Runnable {
        override fun run() {
            if (!isRunning.get()) return

            val endTime = System.currentTimeMillis()
            val beginTime = endTime - 1000L

            val usageEvents = usageStatsManager.queryEvents(beginTime, endTime)
            val event = UsageEvents.Event()
            var currentForegroundPackage: String? = null

            while (usageEvents.hasNextEvent()) {
                usageEvents.getNextEvent(event)
                if (event.eventType == MOVE_TO_FOREGROUND) {
                    currentForegroundPackage = event.packageName
                }
            }

            val match = (currentForegroundPackage == null && monitoredRegions.any { it.appId == lastForegroundPackage }) || monitoredRegions.any { it.appId == currentForegroundPackage }
            Log.d("TapBlocker", "Foreground app: $currentForegroundPackage -- last: $lastForegroundPackage")
            if (match) {
                if(currentForegroundPackage == null) {
                    updateOverlays(monitoredRegions.filter { it.appId == lastForegroundPackage })
                } else {
                    updateOverlays(monitoredRegions.filter { it.appId == currentForegroundPackage })
                    lastForegroundPackage = currentForegroundPackage
                }
            } else {
                removeAllOverlays()
                lastForegroundPackage = null
            }

            handler.postDelayed(this, 1000)
        }
    }

    private fun updateOverlays(regions: List<RegionEntity>) {
        // Entferne Overlays für Regionen, die nicht mehr gematcht werden
        val toRemove = regionOverlays.keys - regions.map { it.regionId }
        toRemove.forEach { removeOverlayFor(it) }

        // Füge Overlays für neue Regionen hinzu
        regions.forEach { region ->
            if (!regionOverlays.containsKey(region.regionId)) showOverlayFor(region)
        }
    }

    @SuppressLint("InflateParams")
    private fun showOverlayFor(region: RegionEntity) {
        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(R.layout.overlay_block, null)

        // convert dp→px
        val density = Resources.getSystem().displayMetrics.density
        val widthPx  = (region.xSize * density).toInt()
        val heightPx = (region.ySize * density).toInt()
        val xOffPx   = (region.xOffset * density).toInt()
        val yOffPx   = (region.yOffset * density).toInt()

        val orientation = RegionOrientation.valueOf(region.orientation)
        val params = WindowManager.LayoutParams(
            widthPx,
            heightPx,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = orientation.toGravity()
            x = xOffPx
            y = yOffPx
        }

        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        try {
            wm.addView(view, params)
            regionOverlays[region.regionId] = view
            Log.d("TapBlocker", "Overlay for region ${region.regionId} at $xOffPx,$yOffPx size ${widthPx}×${heightPx}")
        } catch (e: Exception) {
            Log.e("TapBlocker", "Error showing overlay: ${e.message}")
        }
    }


    private fun removeOverlayFor(regionId: Long) {
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        regionOverlays[regionId]?.let { view ->
            try {
                wm.removeView(view)
                regionOverlays.remove(regionId)
                Log.d("TapBlocker", "Overlay for region $regionId removed")
            } catch (e: Exception) {
                Log.e("TapBlocker", "Error removing overlay for $regionId: ${e.message}")
            }
        }
    }

    private fun removeAllOverlays() {
        regionOverlays.keys.toList().forEach { removeOverlayFor(it) }
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning.set(false)
        handler.removeCallbacks(checkForegroundApp)
        removeAllOverlays()
        Log.d("TapBlocker", "Destroyed foreground service")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun RegionOrientation.toGravity(): Int = when (this) {
        RegionOrientation.TOPLEFT -> Gravity.TOP or Gravity.START
        RegionOrientation.TOP -> Gravity.TOP or Gravity.CENTER_HORIZONTAL
        RegionOrientation.TOPRIGHT -> Gravity.TOP or Gravity.END
        RegionOrientation.RIGHT -> Gravity.CENTER_VERTICAL or Gravity.END
        RegionOrientation.BOTTOMRIGHT -> Gravity.BOTTOM or Gravity.END
        RegionOrientation.BOTTOM -> Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        RegionOrientation.BOTTOMLEFT -> Gravity.BOTTOM or Gravity.START
        RegionOrientation.LEFT -> Gravity.CENTER_VERTICAL or Gravity.START
    }
}