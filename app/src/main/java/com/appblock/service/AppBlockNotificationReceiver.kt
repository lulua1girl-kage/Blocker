package com.appblock.service
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.appblock.R
import com.appblock.data.AppBlockRepository

class AppBlockNotificationReceiver:BroadcastReceiver(){
 override fun onReceive(context:Context,intent:Intent){
  val manager=context.getSystemService(NotificationManager::class.java)
  manager.createNotificationChannel(NotificationChannel("app_block_schedule","Schedules",NotificationManager.IMPORTANCE_DEFAULT))
  manager.notify((System.currentTimeMillis()%Int.MAX_VALUE).toInt(),NotificationCompat.Builder(context,"app_block_schedule").setSmallIcon(R.drawable.ic_app_block).setContentTitle(intent.getStringExtra("title")?:"App Block").setContentText(intent.getStringExtra("message")?:"Protection schedule update").setAutoCancel(true).build())
  val id=intent.getLongExtra("scheduleId",-1)
  if(id!=-1)AppBlockRepository(context).schedules().firstOrNull{it.id==id}?.let{if(it.enabled&&it.oneTimeDate==null)NotificationScheduler.schedule(context,it)}
 }
}