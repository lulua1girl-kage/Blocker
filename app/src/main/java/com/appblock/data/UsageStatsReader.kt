package com.appblock.data

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import java.util.Calendar

class UsageStatsReader(private val context: Context) {
    fun today() = period(UsageStatsManager.INTERVAL_DAILY, 1)
    fun week() = period(UsageStatsManager.INTERVAL_WEEKLY, 7)
    fun month() = period(UsageStatsManager.INTERVAL_MONTHLY, 30)

    fun minutesToday(packageName: String): Int {
        val manager = context.getSystemService(UsageStatsManager::class.java) ?: return 0
        val start = startOfDay()
        val end = System.currentTimeMillis()
        val events = manager.queryEvents(start, end)
        val event = UsageEvents.Event()
        var foregroundAt = -1L
        var total = 0L
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.packageName != packageName) continue
            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED, UsageEvents.Event.MOVE_TO_FOREGROUND -> foregroundAt = event.timeStamp
                UsageEvents.Event.ACTIVITY_PAUSED, UsageEvents.Event.ACTIVITY_STOPPED, UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                    if (foregroundAt >= 0) { total += (event.timeStamp - foregroundAt).coerceAtLeast(0); foregroundAt = -1L }
                }
            }
        }
        if (foregroundAt >= 0) total += (end - foregroundAt).coerceAtLeast(0)
        return (total / 60_000L).toInt()
    }

    fun period(interval: Int, days: Int): List<UsageItem> {
        val end = System.currentTimeMillis()
        val start = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -days); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
        val stats = context.getSystemService(UsageStatsManager::class.java)?.queryUsageStats(interval, start, end).orEmpty()
        return stats.filter { it.totalTimeInForeground > 0 }.groupBy { it.packageName }.map { (pkg, values) ->
            UsageItem(pkg, pkg.substringAfterLast('.'), (values.sumOf { it.totalTimeInForeground } / 60000L).toInt(), "App")
        }.sortedByDescending { it.minutes }
    }

    private fun startOfDay(): Long = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
}