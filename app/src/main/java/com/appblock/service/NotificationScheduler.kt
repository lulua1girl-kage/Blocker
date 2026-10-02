package com.appblock.service
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar
import java.util.TimeZone
import com.appblock.data.BlockSchedule

object NotificationScheduler{
 fun schedule(context:Context,s:BlockSchedule){cancel(context,s);val now=System.currentTimeMillis();scheduleAlarm(context,s,next(s,true,now),"start");scheduleAlarm(context,s,next(s,false,now),"end")}
 fun cancel(context:Context,s:BlockSchedule){val a=context.getSystemService(AlarmManager::class.java);listOf("start","end").forEach{a.cancel(pending(context,s,it))}}
 private fun scheduleAlarm(context:Context,s:BlockSchedule,at:Long,type:String){if(at<=0)return;context.getSystemService(AlarmManager::class.java).setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,pending(context,s,type))}
 private fun pending(context:Context,s:BlockSchedule,type:String)=PendingIntent.getBroadcast(context,s.id.hashCode()*31+type.hashCode(),Intent(context,AppBlockNotificationReceiver::class.java).apply{putExtra("scheduleId",s.id);putExtra("type",type);putExtra("title",s.name);putExtra("message",if(type=="start")"Focus schedule is starting."else"Focus schedule is ending.")},PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
 private fun next(s:BlockSchedule,start:Boolean,now:Long):Long{
  val tz=TimeZone.getTimeZone(s.timezoneId);val base=Calendar.getInstance(tz).apply{timeInMillis=now}
  for(i in 0..7){val c=(base.clone() as Calendar).apply{add(Calendar.DAY_OF_YEAR,i);set(Calendar.HOUR_OF_DAY,if(start)s.startMinutes/60 else s.endMinutes/60);set(Calendar.MINUTE,if(start)s.startMinutes%60 else s.endMinutes%60);set(Calendar.SECOND,0);set(Calendar.MILLISECOND,0)};if(c.timeInMillis<=now)continue;val day=c.get(Calendar.DAY_OF_WEEK);if(start&&s.days.contains(day))return c.timeInMillis;if(!start&&s.days.contains(if(s.startMinutes>s.endMinutes){if(day==Calendar.SUNDAY)Calendar.SATURDAY else day-1}else day))return c.timeInMillis}
  return -1
 }
}