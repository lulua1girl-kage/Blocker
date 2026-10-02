package com.appblock.data

import android.content.Context
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class BlockingEngine(context:Context){
    private val appContext=context.applicationContext
    private val repo=AppBlockRepository(appContext)
    private val usage=UsageStatsReader(appContext)

    fun decision(packageName:String,now:Calendar=Calendar.getInstance()):BlockDecision{
        if(packageName.isBlank()||packageName==appContext.packageName)return BlockDecision.allow()
        val s=repo.settings()
        if(!s.focusEnabled||s.sessionBreakUntil>System.currentTimeMillis())return BlockDecision.allow()
        val unlock=repo.unlocks().firstOrNull{it.packageName==packageName&&it.expiresAt>System.currentTimeMillis()}
        if(unlock!=null&&!isProtected(s))return BlockDecision.allow()
        val activeSchedules=repo.schedules().filter{it.enabled&&isScheduleActive(it,now)}
        val activeLists=repo.blockLists().filter{it.enabled&&(it.alwaysActive||activeSchedules.any{sch->sch.blockListIds.isEmpty()||sch.blockListIds.contains(it.id)})}
        val selected=repo.blockedApps().firstOrNull{it.packageName==packageName}
        val listMatch=activeLists.firstOrNull{it.packageNames.contains(packageName)}
        val used=usage.minutesToday(packageName)
        val limit=selected?.dailyLimitMinutes?:0
        if(isProtected(s)){
            if(selected?.allowedDuringProtected==true)return BlockDecision.allow()
            if(listMatch!=null)return BlockDecision(true,BlockReason.PROTECTED_SESSION,"Protected Focus Session",listMatch.name)
            if(selected?.blocked==true)return BlockDecision(true,BlockReason.PROTECTED_SESSION,"Protected Focus Session",selected.name)
        }
        if(limit>0&&used>=limit)return BlockDecision(true,BlockReason.DAILY_LIMIT,"Daily limit reached",selected?.name)
        if(s.dailyLimit>0&&selected!=null&&used>=s.dailyLimit)return BlockDecision(true,BlockReason.DAILY_LIMIT,"Default daily limit reached",selected.name)
        if(listMatch!=null)return BlockDecision(true,BlockReason.APP_RULE,"Blocklist match",listMatch.name)
        return when(s.mode){
            BlockMode.STRICT->if(selected?.blocked!=false)BlockDecision(true,BlockReason.MODE,"Strict Mode: app is not explicitly allowed") else BlockDecision.allow()
            BlockMode.STUDY->if(selected?.blocked==true||knownDistraction(packageName))BlockDecision(true,BlockReason.MODE,"Study Mode") else BlockDecision.allow()
            else->if(selected?.blocked==true)BlockDecision(true,BlockReason.APP_RULE,"Selected blocked app",selected.name) else BlockDecision.allow()
        }
    }

    fun websiteDecision(domain:String?,now:Calendar=Calendar.getInstance()):BlockDecision{
        if(domain.isNullOrBlank())return BlockDecision.allow()
        val s=repo.settings()
        if(!s.focusEnabled||s.sessionBreakUntil>System.currentTimeMillis())return BlockDecision.allow()
        val candidate=WebsiteUrlDetector.normalizeDomain(domain)
        val schedules=repo.schedules().filter{it.enabled&&isScheduleActive(it,now)}
        val lists=repo.blockLists().filter{it.enabled&&(it.alwaysActive||schedules.any{sch->sch.blockListIds.isEmpty()||sch.blockListIds.contains(it.id)})}
        val rule=lists.firstNotNullOfOrNull{l->l.websites.firstOrNull{r->matches(candidate,r)}?.let{l.name}}
            ?:repo.websites().firstOrNull{matches(candidate,it)}
        return if(rule!=null)BlockDecision(true,BlockReason.APP_RULE,"Website rule",rule) else BlockDecision.allow()
    }

    fun validateSchedule(s:BlockSchedule):String?{
        if(s.name.isBlank())return "Schedule needs a name"
        if(s.startMinutes !in 0..1439||s.endMinutes !in 0..1439)return "Invalid schedule time"
        if(s.oneTimeDate==null&&s.days.isEmpty())return "Choose at least one day"
        return null
    }

    fun isScheduleActive(s:BlockSchedule,instant:Long=System.currentTimeMillis()):Boolean=
        isScheduleActive(s,Calendar.getInstance(TimeZone.getTimeZone(s.timezoneId)).apply{timeInMillis=instant})

    fun isScheduleActive(s:BlockSchedule,now:Calendar):Boolean{
        if(!s.enabled)return false
        val c=now.clone() as Calendar
        c.timeZone=TimeZone.getTimeZone(s.timezoneId)
        val minute=c.get(Calendar.HOUR_OF_DAY)*60+c.get(Calendar.MINUTE)
        val overnight=s.startMinutes>s.endMinutes
        if(s.oneTimeDate!=null){
            val today=dateKey(c)
            if(today==s.oneTimeDate)return if(!overnight)minute in s.startMinutes..s.endMinutes else minute>=s.startMinutes
            if(overnight){val p=(c.clone() as Calendar).apply{add(Calendar.DAY_OF_YEAR,-1)};return dateKey(p)==s.oneTimeDate&&minute<=s.endMinutes}
            return false
        }
        val day=c.get(Calendar.DAY_OF_WEEK)
        if(!overnight)return s.days.contains(day)&&minute in s.startMinutes..s.endMinutes
        if(s.days.contains(day)&&minute>=s.startMinutes)return true
        val p=(c.clone() as Calendar).apply{add(Calendar.DAY_OF_YEAR,-1)}
        return s.days.contains(p.get(Calendar.DAY_OF_WEEK))&&minute<=s.endMinutes
    }

    fun conflicts(candidate:BlockSchedule,existing:List<BlockSchedule>)=
        existing.filter{it.enabled}.any{a->(0 until 7).any{d->(0 until 1440).any{m->activeOnDay(a,d+Calendar.SUNDAY,m)&&activeOnDay(candidate,d+Calendar.SUNDAY,m)}}}

    private fun activeOnDay(s:BlockSchedule,day:Int,minute:Int):Boolean{
        if(s.startMinutes<=s.endMinutes)return s.days.contains(day)&&minute in s.startMinutes..s.endMinutes
        if(s.days.contains(day)&&minute>=s.startMinutes)return true
        val previous=if(day==Calendar.SUNDAY)Calendar.SATURDAY else day-1
        return s.days.contains(previous)&&minute<=s.endMinutes
    }

    private fun matches(candidate:String,rule:String):Boolean{
        val r=WebsiteUrlDetector.normalizeRule(rule)?:return false
        return candidate==r||candidate.endsWith("."+r)
    }
    private fun isProtected(s:AppSettings)=s.protectedSessionUntil>System.currentTimeMillis()
    private fun dateKey(c:Calendar)="%04d-%02d-%02d".format(Locale.US,c.get(Calendar.YEAR),c.get(Calendar.MONTH)+1,c.get(Calendar.DAY_OF_MONTH))
    private fun knownDistraction(p:String)=listOf("youtube","instagram","tiktok","facebook","netflix","snapchat","twitter","reddit","discord").any{p.contains(it,true)}
}
data class BlockDecision(val blocked:Boolean,val reason:BlockReason?=null,val detail:String="",val matchedRule:String?=null){
    companion object{fun allow()=BlockDecision(false)}
}