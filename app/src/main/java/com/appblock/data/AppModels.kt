package com.appblock.data

import android.content.pm.ApplicationInfo

/** Domain models. Defaults preserve compatibility with older App Block backups. */
data class InstalledApp(val packageName:String,val name:String,val category:String,val icon:ApplicationInfo?=null,val isSystem:Boolean=false)

data class BlockedApp(val packageName:String,val name:String,val category:String,val blocked:Boolean=true,val dailyLimitMinutes:Int=0,val allowedDuringProtected:Boolean=false)

data class BlockList(
    val id:Long,
    val name:String,
    val packageNames:Set<String> = emptySet(),
    val websites:Set<String> = emptySet(),
    val enabled:Boolean = true,
    val alwaysActive:Boolean = false,
    val category:String = "Custom"
)

data class BlockSchedule(
    val id:Long,
    val name:String,
    val days:Set<Int>,
    val startMinutes:Int,
    val endMinutes:Int,
    val enabled:Boolean=true,
    val oneTimeDate:String?=null,
    val blockListIds:Set<Long> = emptySet(),
    val timezoneId:String = java.util.TimeZone.getDefault().id,
    val breakMinutes:Int = 0
)

data class UsageItem(val packageName:String,val name:String,val minutes:Int,val category:String)
data class FocusSession(val id:Long,val startedAt:Long,val durationMinutes:Int,val completed:Boolean,val plannedMinutes:Int=0,val breaksMinutes:Int=0,val interrupted:Boolean=false)
data class SessionBreak(val id:Long,val sessionId:Long,val startedAt:Long,val durationMinutes:Int,val completed:Boolean)

data class BlockEvent(val id:Long,val timestamp:Long,val packageName:String,val target:String,val reason:BlockReason,val detail:String="")
data class UnlockGrant(val packageName:String,val expiresAt:Long,val reason:String="Temporary unlock")

data class GoalSettings(val dailyFocusMinutes:Int=60,val weeklyFocusMinutes:Int=300,val enabled:Boolean=true)

data class HealthCheck(val key:String,val title:String,val ok:Boolean,val detail:String)

enum class BlockReason(val label:String) { APP_RULE("Blocked app"), DAILY_LIMIT("Daily limit reached"), SCHEDULE("Active schedule"), PROTECTED_SESSION("Protected focus session"), MODE("Blocking mode") }

enum class BlockMode(val label:String,val description:String) {
    FOCUS("Focus Mode","Block the apps and websites you selected"),
    STRICT("Strict Mode","Block discovered apps unless explicitly allowed"),
    STUDY("Study Mode","Block selected distractions plus known distraction categories"),
    SLEEP("Sleep Mode","Block selected rules during sleep schedules"),
    CUSTOM("Custom Mode","Use your selected rules and schedules")
}

data class AppSettings(
    val focusEnabled:Boolean=true,
    val mode:BlockMode=BlockMode.FOCUS,
    val dailyLimit:Int=0,
    val notifications:Boolean=true,
    val onboardingComplete:Boolean=false,
    val soundEnabled:Boolean=true,
    val vibrationEnabled:Boolean=true,
    val darkMode:Boolean=true,
    val disclosureAccepted:Boolean=false,
    val protectedSessionUntil:Long=0L,
    val localOnlyData:Boolean=true,
    val allowTemporaryUnlock:Boolean=true,
    val breakMinutes:Int=0,
    val timezoneId:String=java.util.TimeZone.getDefault().id,
    val clock24Hour:Boolean=true,
    val protectionHeartbeatAt:Long=0L,
    val protectionFailureReason:String?=null,
    val sessionBreakUntil:Long=0L
)