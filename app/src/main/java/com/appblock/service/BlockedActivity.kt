package com.appblock.service

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.appblock.data.AppBlockRepository
import com.appblock.data.BlockMode
import com.appblock.data.UnlockGrant

class BlockedActivity:Activity(){
 override fun onCreate(savedInstanceState:Bundle?){
  super.onCreate(savedInstanceState)
  val pkg=intent.getStringExtra("package").orEmpty();val reason=intent.getStringExtra("reason").orEmpty();val domain=intent.getStringExtra("domain")
  val rule=intent.getStringExtra("rule");val detail=intent.getStringExtra("detail").orEmpty();val repo=AppBlockRepository(this);val s=repo.settings()
  val app=repo.blockedApps().firstOrNull{it.packageName==pkg}
  val title=if(reason=="website")"Website blocked" else (app?.name?:pkg)+" is blocked"
  val why=when{reason=="website"->(domain?:"This website")+" matches "+(rule?:"a blocked rule");detail.isNotBlank()->detail;else->s.mode.label}
  val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER;setPadding(48,48,48,48);setBackgroundColor(Color.rgb(6,16,28))}
  root.addView(TextView(this).apply{text="🔒";textSize=56f;gravity=Gravity.CENTER})
  root.addView(TextView(this).apply{text=title;textSize=26f;setTextColor(Color.WHITE);gravity=Gravity.CENTER;setPadding(0,18,0,12)})
  root.addView(TextView(this).apply{text=why;textSize=16f;setTextColor(Color.LTGRAY);gravity=Gravity.CENTER})
  root.addView(Button(this).apply{text="Why is this blocked?";setOnClickListener{AlertDialog.Builder(this@BlockedActivity).setTitle("Protection reason").setMessage(why).setPositiveButton("OK",null).show()}},params(28))
  root.addView(Button(this).apply{text="Back to launcher";setOnClickListener{startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME));finish()}},params(8))
  if(reason!="website"&&s.allowTemporaryUnlock&&s.mode!=BlockMode.STRICT&&s.protectedSessionUntil<=System.currentTimeMillis())root.addView(Button(this).apply{text="Unlock for 5 minutes";setOnClickListener{repo.saveUnlocks(repo.unlocks().filter{it.packageName!=pkg}+UnlockGrant(pkg,System.currentTimeMillis()+300000));finish()}},params(8))
  setContentView(root)
 }
 private fun params(top:Int)=LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT).apply{topMargin=top}
}