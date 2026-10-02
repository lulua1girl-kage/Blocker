package com.appblock

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.appblock.data.BlockMode
import java.util.Calendar

class MainActivity:ComponentActivity(){
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);setContent{AppBlockTheme{AppBlockApp(this)}}}
}

@Composable
private fun AppBlockTheme(content:@Composable()->Unit){MaterialTheme(colorScheme=darkColorScheme(primary=androidx.compose.ui.graphics.Color(0xFF3198FF),background=androidx.compose.ui.graphics.Color(0xFF06101C),surface=androidx.compose.ui.graphics.Color(0xFF0D1826)),content=content)}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppBlockApp(activity:ComponentActivity,vm:AppBlockViewModel=viewModel()){
 var tab by remember{mutableIntStateOf(0)}
 Scaffold(topBar={TopAppBar(title={Text(if(tab==0)"App Block" else if(tab==1)"Apps" else if(tab==2)"Schedules" else "Settings")},actions={if(vm.protected())TextButton(onClick=vm::endProtected){Text("End")}})},bottomBar={NavigationBar{listOf("Home","Apps","Schedules","Settings").forEachIndexed{index,label->NavigationBarItem(selected=tab==index,onClick={tab=index},icon={Icon(if(index==0)Icons.Default.Home else if(index==1)Icons.Default.Apps else if(index==2)Icons.Default.Schedule else Icons.Default.Settings,label)},label={Text(label)})}}}){pad->
 when(tab){0->HomeScreen(vm,Modifier.padding(pad));1->AppsScreen(vm,Modifier.padding(pad));2->SchedulesScreen(vm,Modifier.padding(pad));3->SettingsScreen(activity,vm,Modifier.padding(pad))}
 }
}

@Composable
private fun HomeScreen(vm:AppBlockViewModel,modifier:Modifier){
 var quick by remember{mutableStateOf(false)}
 LazyColumn(modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
  item{Card{Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){Text("Protection",style=MaterialTheme.typography.headlineSmall);Text(if(vm.settings.focusEnabled)"Active":"Paused",style=MaterialTheme.typography.titleLarge);Text(vm.settings.mode.label);Row(verticalAlignment=Alignment.CenterVertically){Text("Protection switch",Modifier.weight(1f));Switch(checked=vm.settings.focusEnabled,onCheckedChange={vm.toggleFocus()})}}}}
  item{Row(horizontalArrangement=Arrangement.spacedBy(10.dp),modifier=Modifier.fillMaxWidth()){StatCard("Blocked",vm.blockCount().toString(),Modifier.weight(1f));StatCard("Focus today",vm.focusMinutesToday().toString()+"m",Modifier.weight(1f))}}
  item{Card{Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){Text("Quick Block",style=MaterialTheme.typography.titleLarge);Text("Start a protected session without creating a schedule.");Button(onClick={quick=true}){Text("Start 25 minutes")}}}}
  item{if(vm.protected())Card{Column(Modifier.padding(18.dp)){Text("Protected session active",style=MaterialTheme.typography.titleMedium);Text("Rule changes are locked until the session ends.")}}}
  item{Text("Usage today",style=MaterialTheme.typography.titleLarge)}
  items(vm.usageToday.take(8)){u->ListItem(headlineContent={Text(u.name)},supportingContent={Text(u.category)},trailingContent={Text(u.minutes.toString()+" min")})}
 }
 if(quick){AlertDialog(onDismissRequest={quick=false},title={Text("Quick Block")},text={Text("Start a 25-minute protected session?")},confirmButton={Button(onClick={quick=false;vm.quickBlock(25)}){Text("Start")}},dismissButton={TextButton(onClick={quick=false}){Text("Cancel")}})}
}

@Composable private fun StatCard(title:String,value:String,modifier:Modifier){Card(modifier){Column(Modifier.padding(16.dp)){Text(title);Text(value,style=MaterialTheme.typography.headlineMedium)}}}

@Composable
private fun AppsScreen(vm:AppBlockViewModel,modifier:Modifier){
 Column(modifier.fillMaxSize().padding(16.dp)){OutlinedTextField(value=vm.search,onValueChange=vm::setSearch,modifier=Modifier.fillMaxWidth(),singleLine=true,label={Text("Search apps")})
 Spacer(Modifier.height(10.dp))
 Text("Tap an app to block/unblock. Strict Mode treats unselected apps as blocked.",style=MaterialTheme.typography.bodySmall)
 LazyColumn{items(vm.filteredApps()){app->val state=vm.blockedApps.firstOrNull{it.packageName==app.packageName};ListItem(headlineContent={Text(app.name)},supportingContent={Text(app.category+" • "+app.packageName)},trailingContent={Switch(checked=state?.blocked==true,onCheckedChange={vm.toggleApp(app.packageName)})})}}}
}

@Composable
private fun SchedulesScreen(vm:AppBlockViewModel,modifier:Modifier){
 var dialog by remember{mutableStateOf(false)}
 Column(modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
  Row(verticalAlignment=Alignment.CenterVertically){Text("Schedules",style=MaterialTheme.typography.headlineSmall,modifier=Modifier.weight(1f));Button(onClick={dialog=true}){Text("Add")}}
  Text("Timezone: "+vm.settings.timezoneId,style=MaterialTheme.typography.bodySmall)
  LazyColumn{items(vm.schedules){s->Card(Modifier.fillMaxWidth().padding(vertical=4.dp)){Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(s.name);Text(formatMinutes(s.startMinutes)+" – "+formatMinutes(s.endMinutes));Text("Days: "+s.days.sorted().joinToString(","))};Switch(checked=s.enabled,onCheckedChange={vm.toggleSchedule(s.id)});IconButton(onClick={vm.deleteSchedule(s.id)}){Icon(Icons.Default.Delete,"Delete")}}}}}
 }
 if(dialog)ScheduleDialog({dialog=false},vm)
}

private fun formatMinutes(v:Int)="%02d:%02d".format(v/60,v%60)

@Composable
private fun ScheduleDialog(close:()->Unit,vm:AppBlockViewModel){
 var name by remember{mutableStateOf("")};var start by remember{mutableStateOf("09:00")};var end by remember{mutableStateOf("17:00")};var days by remember{mutableStateOf(setOf(Calendar.MONDAY,Calendar.TUESDAY,Calendar.WEDNESDAY,Calendar.THURSDAY,Calendar.FRIDAY))}
 AlertDialog(onDismissRequest=close,title={Text("New schedule")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){OutlinedTextField(name,{name=it},label={Text("Name")},singleLine=true);OutlinedTextField(start,{start=it},label={Text("Start HH:MM")},singleLine=true);OutlinedTextField(end,{end=it},label={Text("End HH:MM")},singleLine=true);Text("Days: "+days.sorted().joinToString(","))}},confirmButton={Button(onClick={val a=parseTime(start);val b=parseTime(end);if(a!=null&&b!=null){vm.addSchedule(name,a,b,days);close()}}){Text("Save")}},dismissButton={TextButton(onClick=close){Text("Cancel")}})
}
private fun parseTime(s:String):Int?{val p=s.split(":");if(p.size!=2)return null;val h=p[0].toIntOrNull()?:return null;val m=p[1].toIntOrNull()?:return null;if(h !in 0..23||m !in 0..59)return null;return h*60+m}

@Composable
private fun SettingsScreen(activity:ComponentActivity,vm:AppBlockViewModel,modifier:Modifier){
 var mode by remember(vm.settings.mode){mutableStateOf(vm.settings.mode)}
 LazyColumn(modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
  item{Text("Protection setup",style=MaterialTheme.typography.headlineSmall)}
  item{Card{Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Text(if(vm.serviceEnabled)"Accessibility service enabled"else"Accessibility service required");Button(onClick={activity.startActivity(vm.openAccessibility())}){Text("Open Accessibility Settings")};Text(if(vm.usageAccess)"Usage Access enabled"else"Usage Access required");Button(onClick={activity.startActivity(vm.openUsageAccess())}){Text("Open Usage Access Settings")}}}}
  item{Text("Mode",style=MaterialTheme.typography.titleLarge)}
  items(BlockMode.entries.toList()){m->RadioRow(m.label,m.description,mode==m){mode=m;vm.setMode(m)}}
  item{SettingSwitch("Notifications",vm.settings.notifications,vm::setNotifications)}
  item{SettingSwitch("Temporary unlocks",vm.settings.allowTemporaryUnlock,vm::setTemporaryUnlock)}
  item{Text("Website rules",style=MaterialTheme.typography.titleLarge)}
  item{WebsiteEditor(vm)}
  item{Text("Recent blocked events",style=MaterialTheme.typography.titleLarge)}
  items(vm.events.take(10)){e->ListItem(headlineContent={Text(e.target)},supportingContent={Text(e.reason.label+" • "+e.detail)})}
  item{Button(onClick=vm::refresh,modifier=Modifier.fillMaxWidth()){Text("Refresh protection status")}}
 }
}

@Composable private fun RadioRow(title:String,description:String,selected:Boolean,onClick:()->Unit){Card(onClick=onClick,modifier=Modifier.fillMaxWidth()){Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically){RadioButton(selected,onClick);Column{Text(title);Text(description,style=MaterialTheme.typography.bodySmall)}}}}
@Composable private fun SettingSwitch(title:String,checked:Boolean,onChange:(Boolean)->Unit){Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text(title,Modifier.weight(1f));Switch(checked,onChange)}}

@Composable
private fun WebsiteEditor(vm:AppBlockViewModel){
 var value by remember{mutableStateOf("")}
 Column(verticalArrangement=Arrangement.spacedBy(8.dp)){Row(verticalAlignment=Alignment.CenterVertically){OutlinedTextField(value,{value=it},modifier=Modifier.weight(1f),singleLine=true,label={Text("example.com")});Spacer(Modifier.width(8.dp));Button(onClick={vm.addWebsite(value);value=""}){Text("Add")}}
 vm.websites.forEach{site->ListItem(headlineContent={Text(site)},trailingContent={IconButton(onClick={vm.removeWebsite(site)}){Icon(Icons.Default.Delete,"Remove")}})}
 }
}
