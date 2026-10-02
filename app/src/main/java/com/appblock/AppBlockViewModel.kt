package com.appblock

import android.app.Application
import android.app.AppOpsManager
import android.content.Intent
import android.provider.Settings
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import com.appblock.data.*
import com.appblock.service.NotificationScheduler

class AppBlockViewModel(app:Application):AndroidViewModel(app){
 private val repo=AppBlockRepository(app)
 private val installedRepo=InstalledAppRepository(app)
 private val usage=UsageStatsReader(app)
 private val engine=BlockingEngine(app)
 var settings by mutableStateOf(repo.settings());private set
 var apps by mutableStateOf(installedRepo.apps());private set
 var blockedApps by mutableStateOf(repo.blockedApps());private set
 var schedules by mutableStateOf(repo.schedules());private set
 var blockLists by mutableStateOf(repo.blockLists());private set
 var websites by mutableStateOf(repo.websites().toList().sorted());private set
 var events by mutableStateOf(repo.blockEvents());private set
 var usageToday by mutableStateOf(usage.today());private set
 var search by mutableStateOf("");private set
 var serviceEnabled by mutableStateOf(false);private set
 var usageAccess by mutableStateOf(false);private set
 init{refresh()}
 fun refresh(){apps=installedRepo.apps();blockedApps=repo.blockedApps();schedules=repo.schedules();blockLists=repo.blockLists();websites=repo.websites().toList().sorted();events=repo.blockEvents();usageToday=usage.today();settings=repo.settings();checkPermissions()}
 fun setSearch(v:String){search=v}
 fun filteredApps()=apps.filter{search.isBlank()||it.name.contains(search,true)||it.category.contains(search,true)}
 fun toggleFocus(){if(!protected())save(settings.copy(focusEnabled=!settings.focusEnabled))}
 fun setMode(m:BlockMode){if(!protected())save(settings.copy(mode=m))}
 fun toggleApp(pkg:String){
  if(protected())return
  val app=apps.firstOrNull{it.packageName==pkg}?:return
  val old=blockedApps.firstOrNull{it.packageName==pkg}
  val next=old?.copy(blocked=!old.blocked)?:BlockedApp(pkg,app.name,app.category,settings.mode!=BlockMode.STRICT)
  blockedApps=if(old==null)blockedApps+next else blockedApps.map{if(it.packageName==pkg)next else it}
  repo.saveBlockedApps(blockedApps)
 }
 fun setLimit(pkg:String,minutes:Int){if(protected())return;val app=apps.firstOrNull{it.packageName==pkg}?:return;val old=blockedApps.firstOrNull{it.packageName==pkg};val next=(old?:BlockedApp(pkg,app.name,app.category,false)).copy(dailyLimitMinutes=minutes.coerceAtLeast(0));blockedApps=if(old==null)blockedApps+next else blockedApps.map{if(it.packageName==pkg)next else it};repo.saveBlockedApps(blockedApps)}
 fun createList(name:String){if(name.isBlank()||protected())return;val n=BlockList(System.currentTimeMillis(),name);blockLists=blockLists+n;repo.saveBlockLists(blockLists)}
 fun toggleList(id:Long){if(protected())return;blockLists=blockLists.map{if(it.id==id)it.copy(enabled=!it.enabled)else it};repo.saveBlockLists(blockLists)}
 fun addWebsite(value:String){if(protected())return;val v=WebsiteUrlDetector.normalizeRule(value)?:return;websites=(websites+v).distinct().sorted();repo.saveWebsites(websites.toSet())}
 fun removeWebsite(value:String){if(protected())return;websites=websites-value;repo.saveWebsites(websites.toSet())}
 fun addSchedule(name:String,start:Int,end:Int,days:Set<Int>){if(protected())return;val s=BlockSchedule(System.currentTimeMillis(),name.ifBlank{"Focus schedule"},days,start,end,true,null,emptySet(),settings.timezoneId,settings.breakMinutes);if(engine.validateSchedule(s)!=null||engine.conflicts(s,schedules))return;schedules=schedules+s;repo.saveSchedules(schedules);NotificationScheduler.schedule(getApplication(),s)}
 fun toggleSchedule(id:Long){if(protected())return;val updated=schedules.map{if(it.id==id)it.copy(enabled=!it.enabled)else it};schedules=updated;repo.saveSchedules(updated);updated.firstOrNull{it.id==id}?.let{if(it.enabled)NotificationScheduler.schedule(getApplication(),it)else NotificationScheduler.cancel(getApplication(),it)}}
 fun deleteSchedule(id:Long){if(protected())return;schedules.firstOrNull{it.id==id}?.let{NotificationScheduler.cancel(getApplication(),it)};schedules=schedules.filterNot{it.id==id};repo.saveSchedules(schedules)}
 fun quickBlock(minutes:Int){if(protected())return;save(settings.copy(focusEnabled=true,protectedSessionUntil=System.currentTimeMillis()+minutes*60000L))}
 fun endProtected(){save(settings.copy(protectedSessionUntil=0L))}
 fun setNotifications(v:Boolean){if(!protected())save(settings.copy(notifications=v))}
 fun setTemporaryUnlock(v:Boolean){if(!protected())save(settings.copy(allowTemporaryUnlock=v))}
 fun exportData()=repo.exportJson()
 fun deleteData(){if(!protected()){repo.clearAll();refresh()}}
 fun protected()=settings.protectedSessionUntil>System.currentTimeMillis()
 fun focusMinutesToday()=repo.focusSessions().filter{it.startedAt>=dayStart()}.sumOf{it.durationMinutes}
 fun blockCount()=blockedApps.count{it.blocked}
 private fun save(s:AppSettings){settings=s;repo.saveSettings(s)}
 private fun dayStart()=java.util.Calendar.getInstance().apply{set(java.util.Calendar.HOUR_OF_DAY,0);set(java.util.Calendar.MINUTE,0);set(java.util.Calendar.SECOND,0);set(java.util.Calendar.MILLISECOND,0)}.timeInMillis
 private fun checkPermissions(){
  val c=getApplication<Application>()
  usageAccess=runCatching{c.getSystemService(AppOpsManager::class.java).checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,android.os.Process.myUid(),c.packageName)==AppOpsManager.MODE_ALLOWED}.getOrDefault(false)
  serviceEnabled=runCatching{c.getSystemService(android.view.accessibility.AccessibilityManager::class.java).getEnabledAccessibilityServiceList(android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK).any{it.resolveInfo.serviceInfo.packageName==c.packageName}}.getOrDefault(false)
 }
 fun openAccessibility():Intent=Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
 fun openUsageAccess():Intent=Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
}