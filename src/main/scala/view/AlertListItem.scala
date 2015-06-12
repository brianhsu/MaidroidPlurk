package idv.brianhsu.maidroid.plurk.view

import idv.brianhsu.maidroid.plurk._
import idv.brianhsu.maidroid.plurk.activity._
import idv.brianhsu.maidroid.plurk.adapter._
import idv.brianhsu.maidroid.plurk.cache._
import idv.brianhsu.maidroid.plurk.dialog._
import idv.brianhsu.maidroid.plurk.fragment._
import idv.brianhsu.maidroid.plurk.TypedResource._
import idv.brianhsu.maidroid.plurk.util._
import idv.brianhsu.maidroid.ui.util.AsyncUI._
import idv.brianhsu.maidroid.ui.util.CallbackConversions._

import scala.concurrent._

import android.app.Activity
import android.content.ClipboardManager
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.support.v7.internal.view.menu.MenuBuilder
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast

import org.bone.soplurk.api.PlurkAPI._
import org.bone.soplurk.constant.AlertType
import org.bone.soplurk.model._

import java.net.URL
import java.util.Date
import java.text.SimpleDateFormat

class AlertListItem(activity: Activity) extends LinearLayout(activity) {

  private implicit val mActivity = activity
  private val inflater = LayoutInflater.from(activity)
  private var ownerID = -1L

  lazy val avatar = this.findView(TR.itemAlertAvatar)
  lazy val username = this.findView(TR.itemAlertUser)
  lazy val content = this.findView(TR.itemAlertContent)

  initView()

  private def initView() {
    inflater.inflate(R.layout.item_alert, this, true)
  }

  def setAvatarFromCache(avatarBitmap: Bitmap) {
    avatar.setImageBitmap(avatarBitmap)
  }

  def setAvatarFromNetwork(context: Context, user: User) {
    val avatarFuture = AvatarCache.getAvatarBitmapFromNetwork(context, user)
    avatarFuture.onSuccessInUI { case(userID, bitmap) =>
      // Prevent race condition that cause display incorrect avatar for
      // recylced row view.
      if (userID == ownerID) {
        avatar.setImageBitmap(bitmap)
      }
    }
  }


  def update(alert: Alert) {
    val user = alert.user
    this.ownerID = alert.user.id
    val displayName = user.displayName.filterNot(_.isEmpty).getOrElse(user.nickname)
    val contentText = alert.alertType match {
      case AlertType.FriendshipRequest => R.string.viewAlertItemRequest
      case AlertType.FriendshipPending => R.string.viewAlertItemPending
      case AlertType.FriendshipAccepted => R.string.viewAlertItemNewFriend
      case AlertType.NewFriend => R.string.viewAlertItemNewFriend
      case AlertType.NewFan => R.string.viewAlertItemNewFan
    }


    username.setText(s"$displayName (${user.fullName})")
    content.setText(contentText)

    avatar.setImageResource(R.drawable.default_avatar)
    avatar.setOnClickListener { view: View => UserTimelineActivity.startActivity(activity, user) }
    AvatarCache.getAvatarBitmapFromCache(activity, user) match {
      case Some(avatarBitmap) => setAvatarFromCache(avatarBitmap)
      case None => setAvatarFromNetwork(activity, user)
    }

  }

}

