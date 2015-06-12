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
import org.bone.soplurk.constant.PlurkType
import org.bone.soplurk.constant.ReadStatus._

import org.bone.soplurk.model._

import java.net.URL
import java.util.Date
import java.text.SimpleDateFormat

class CliqueListItem(activity: Activity) extends LinearLayout(activity) {

  private implicit val mActivity = activity
  private val inflater = LayoutInflater.from(activity)
  private var ownerID = -1L

  lazy val avatar = this.findView(TR.itemPeopleWithAvatarAvatar)
  lazy val title = this.findView(TR.itemPeopleWithAvatarTitle)
 
  initView()

  private def initView() {
    inflater.inflate(R.layout.item_people_with_avatar, this, true)
    avatar.setImageResource(R.drawable.ic_group_white_48dp)
  }


  def update(cliqueName: String) {
    title.setText(cliqueName)
  }

}

