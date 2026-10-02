package com.appblock.service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.appblock.data.AppBlockRepository

class BootReceiver:BroadcastReceiver(){
 override fun onReceive(context:Context,intent:Intent){
  if(intent.action !in setOf(Intent.ACTION_BOOT_COMPLETED,Intent.ACTION_MY_PACKAGE_REPLACED,Intent.ACTION_TIMEZONE_CHANGED,Intent.ACTION_TIME_CHANGED))return
  AppBlockRepository(context).schedules().filter{it.enabled}.forEach{NotificationScheduler.schedule(context,it)}
 }
}