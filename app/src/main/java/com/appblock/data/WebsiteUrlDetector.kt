package com.appblock.data

import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityEvent
import java.net.URI
import java.util.Locale

object WebsiteUrlDetector {
    private val knownBrowserPackages = setOf(
        "com.android.chrome","org.mozilla.firefox","com.microsoft.emmx","com.brave.browser",
        "com.opera.browser","com.opera.mini.native","com.vivaldi.browser","com.sec.android.app.sbrowser",
        "com.duckduckgo.mobile.android","com.ecosia.android","com.kiwibrowser.browser","com.ucmobile.intl"
    )
    private val domainPattern = Regex("(?i)(?:https?://)?(?:www\.)?([a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?(?:\.[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?)+)")
    fun isBrowserPackage(packageName:String)=packageName.lowercase(Locale.US) in knownBrowserPackages
    fun extractAddressBarDomain(event:AccessibilityEvent,root:AccessibilityNodeInfo?):String?{
        val candidates=ArrayList<String>(4);event.source?.let{collect(it,candidates,HashSet(),0)};if(root!=null)collect(root,candidates,HashSet(),0)
        return candidates.asSequence().mapNotNull(::extractDomain).map(::normalizeDomain).firstOrNull()
    }
    private fun collect(node:AccessibilityNodeInfo,out:MutableList<String>,visited:MutableSet<Long>,depth:Int){
        if(depth>12||out.size>=8)return
        val id=System.identityHashCode(node).toLong();if(!visited.add(id))return
        val rid=node.viewIdResourceName.orEmpty().lowercase(Locale.US);val cls=node.className?.toString().orEmpty().lowercase(Locale.US)
        val addressId=listOf("url_bar","urlbar","address","omnibox","location","url_view").any{rid.contains(it)}
        val addressClass=cls.contains("edittext")&&(addressId||node.isEditable)
        if(addressId||addressClass){node.text?.toString()?.takeIf{it.isNotBlank()}?.let(out::add);node.contentDescription?.toString()?.takeIf{it.isNotBlank()}?.let(out::add)}
        for(i in 0 until node.childCount){if(out.size>=8)break;val child=runCatching{node.getChild(i)}.getOrNull()?:continue;try{collect(child,out,visited,depth+1)}finally{child.recycle()}}
    }
    private fun extractDomain(raw:String):String?{val value=raw.trim().removePrefix("<").removeSuffix(">");if(value.isBlank()||value.contains(' '))return null;domainPattern.find(value)?.groupValues?.getOrNull(1)?.let{return it};return runCatching{URI(if(value.contains("://"))value else "https://$value").host}.getOrNull()}
    fun normalizeDomain(value:String)=value.trim().lowercase(Locale.US).removePrefix("https://").removePrefix("http://").removePrefix("www.").substringBefore('/').substringBefore('?').substringBefore('#').trimEnd('.')
    fun normalizeRule(value:String):String?{val c=normalizeDomain(value);if(c.isBlank()||c.contains(' ')||c.contains(':'))return null;return if(domainPattern.matches(c))c else null}
}