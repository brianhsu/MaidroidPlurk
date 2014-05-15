package idv.brianhsu.maidroid.plurk.util

import idv.brianhsu.maidroid.plurk.activity._
import idv.brianhsu.maidroid.plurk.fragment._

import android.content.Intent
import android.content.Context
import android.webkit.CookieManager

object Logout {

  def logout(context: Context) {
    val dialog = ConfirmDialog.createDialog(
      context, "要登出嗎？", "確定要登出嗎？", "登出"
    ) {
      val intent = new Intent(context, classOf[MaidroidPlurk])
      PlurkAPIHelper.logout(context)
      val cookieManager = CookieManager.getInstance()
      cookieManager.removeAllCookie()
      intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
      context.startActivity(intent)
    }

    dialog.show()
  }

}

