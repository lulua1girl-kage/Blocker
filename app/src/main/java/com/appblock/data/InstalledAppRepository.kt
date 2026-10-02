package com.appblock.data

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

/** Discovers launchable apps and packages visible through UsageStats without QUERY_ALL_PACKAGES. */
class InstalledAppRepository(private val context:Context){
 fun apps():List<InstalledApp>{
  val pm=context.packageManager;val packages=linkedSetOf<String>();val launcher=Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
  pm.queryIntentActivities(launcher,PackageManager.MATCH_ALL).forEach{it.activityInfo?.applicationInfo?.packageName?.let(packages::add)}
  val usage=context.getSystemService(UsageStatsManager::class.java);runCatching{usage?.queryUsageStats(UsageStatsManager.INTERVAL_DAILY,System.currentTimeMillis()-7*24*60*60*1000L,System.currentTimeMillis())?.forEach{packages.add(it.packageName)}}
  return packages.mapNotNull{pkg->runCatching{val ai=pm.getApplicationInfo(pkg,0);if(pkg==context.packageName)return@runCatching null;val label=pm.getApplicationLabel(ai).toString();InstalledApp(pkg,label,categoryFor(pkg,label),ai,ai.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM!=0)}.getOrNull()}.distinctBy{it.packageName}.filterNot{it.isSystem&&it.packageName.startsWith("android")}.sortedBy{it.name.lowercase()}
 }
 private fun categoryFor(pkg:String,name:String)=when{listOf("instagram","tiktok","facebook","twitter","snapchat","reddit","discord").any{pkg.contains(it,true)||name.contains(it,true)}->"Social";listOf("youtube","netflix","spotify","primevideo","disney").any{pkg.contains(it,true)||name.contains(it,true)}->"Entertainment";listOf("chrome","browser","firefox","opera","edge").any{pkg.contains(it,true)||name.contains(it,true)}->"Browser";listOf("docs","drive","classroom","notion","office","onenote").any{pkg.contains(it,true)||name.contains(it,true)}->"Study";else->"Other"}
}