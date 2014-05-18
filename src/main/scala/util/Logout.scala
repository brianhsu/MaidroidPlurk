package idv.brianhsu.maidroid.plurk.util

import idv.brianhsu.maidroid.plurk.R

import idv.brianhsu.maidroid.plurk.activity._
import idv.brianhsu.maidroid.plurk.dialog._
import idv.brianhsu.maidroid.plurk.fragment._

import android.content.Intent
import android.content.Context
import android.webkit.CookieManager
import android.support.v4.app.FragmentActivity
import android.webkit.CookieSyncManager

object Logout {

  def doLogout(context: Context) {
    val intent = new Intent(context, classOf[MaidroidPlurk])
    PlurkAPIHelper.logout(context)
    val syncManager = CookieSyncManager.createInstance(context)
    val cookieManager = CookieManager.getInstance()
    cookieManager.removeAllCookie()
    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
    context.startActivity(intent)
  }

  def logout(activity: FragmentActivity with ConfirmDialog.Listener) {
    val dialog = ConfirmDialog.createDialog(
      activity, 'LogoutConfirm, 
      activity.getString(R.string.utilLogoutConfirmTitle),
      activity.getString(R.string.utilLogoutConfirm),
      activity.getString(R.string.utilLogoutLogout),
      activity.getString(R.string.cancel)
    ) 

    dialog.show(activity.getSupportFragmentManager, "LogoutConfirm")
  }

}

