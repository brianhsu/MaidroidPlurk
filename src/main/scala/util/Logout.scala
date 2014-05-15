package idv.brianhsu.maidroid.plurk.util

import idv.brianhsu.maidroid.plurk.activity._

import android.content.Intent
import android.content.Context
import android.webkit.CookieManager

object Logout {
  def logout(context: Context) {
    val intent = new Intent(context, classOf[MaidroidPlurk])
    PlurkAPIHelper.logout(context)
    val cookieManager = CookieManager.getInstance()
    cookieManager.removeAllCookie()
    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
    context.startActivity(intent)
  }
}

