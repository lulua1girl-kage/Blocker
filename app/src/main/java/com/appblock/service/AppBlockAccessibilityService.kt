package com.appblock.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import com.appblock.data.*

class AppBlockAccessibilityService:AccessibilityService(){
 private lateinit var engine:BlockingEngine
 private lateinit var repo:AppBlockRepository
 private var lastSignature=""
 private var lastLaunchAt=0L
 override fun onServiceConnected(){
  super.onServiceConnected();engine=BlockingEngine(this);repo=AppBlockRepository(this)
  setServiceInfo(serviceInfo.apply{eventTypes=AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or AccessibilityEvent.TYPE_WINDOWS_CHANGED or AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED;notificationTimeout=100})
  repo.heartbeat(true)
 }
 override fun onAccessibilityEvent(event:AccessibilityEvent?){
  if(event==null||!::engine.isInitialized)return
  val pkg=event.packageName?.toString().orEmpty()
  if(pkg.isBlank()||pkg==packageName)return
  val type=event.eventType
  if(type==AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED||type==AccessibilityEvent.TYPE_WINDOWS_CHANGED){
   repo.heartbeat(true)
   val d=engine.decision(pkg)
   if(d.blocked){launchBlocked(pkg,d,null,d.matchedRule);return}
  }
  if(!WebsiteUrlDetector.isBrowserPackage(pkg))return
  val domain=WebsiteUrlDetector.extractAddressBarDomain(event,rootInActiveWindow)?:return
  val d=engine.websiteDecision(domain)
  if(d.blocked)launchBlocked(pkg,d,domain,d.matchedRule)
 }
 private fun launchBlocked(pkg:String,d:BlockDecision,domain:String?,rule:String?){
  val now=SystemClock.elapsedRealtime()
  val sig=pkg+"|"+d.reason+"|"+domain.orEmpty()+"|"+rule.orEmpty()
  if(sig==lastSignature&&now-lastLaunchAt<1500)return
  lastSignature=sig;lastLaunchAt=now
  repo.addBlockEvent(BlockEvent(System.currentTimeMillis(),System.currentTimeMillis(),pkg,domain?:pkg,d.reason?:BlockReason.MODE,d.detail))
  startActivity(Intent(this,BlockedActivity::class.java).apply{
   addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
   putExtra("package",pkg);putExtra("reason",if(domain!=null)"website"else"app");putExtra("domain",domain);putExtra("rule",rule);putExtra("detail",d.detail)
  })
 }
 override fun onInterrupt(){if(::repo.isInitialized)repo.heartbeat(false,"Accessibility service interrupted")}
 override fun onDestroy(){if(::repo.isInitialized)repo.heartbeat(false,"Accessibility service stopped");super.onDestroy()}
}