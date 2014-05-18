package idv.brianhsu.maidroid.plurk.view

import idv.brianhsu.maidroid.plurk._
import idv.brianhsu.maidroid.plurk.adapter._
import idv.brianhsu.maidroid.plurk.dialog._
import idv.brianhsu.maidroid.plurk.fragment._
import idv.brianhsu.maidroid.plurk.TypedResource._
import idv.brianhsu.maidroid.plurk.cache._
import idv.brianhsu.maidroid.plurk.util._
import idv.brianhsu.maidroid.ui.util.AsyncUI._
import idv.brianhsu.maidroid.ui.util.CallbackConversions._

import scala.concurrent._

import android.app.Activity
import android.os.Bundle
import android.content.Context
import android.graphics.Bitmap
import android.text.method.LinkMovementMethod
import android.text.Html
import android.view.View
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.view.MenuItem
import android.support.v4.app.FragmentActivity
import android.support.v7.widget.PopupMenu
import android.support.v7.internal.view.menu.MenuBuilder

import org.bone.soplurk.api.PlurkAPI._
import org.bone.soplurk.constant.ReadStatus._
import org.bone.soplurk.model._

import java.text.SimpleDateFormat
import java.net.URL

class ResponseView(adapter: ResponseAdapter)
                  (implicit val activity: FragmentActivity with ConfirmDialog.Listener) 
                  extends LinearLayout(activity) {

  private val inflater = LayoutInflater.from(activity)

  initView()

  lazy val dateTimeFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
  
  lazy val avatar = this.findView(TR.itemResponseAvatar)
  lazy val content = this.findView(TR.itemResponseText)
  lazy val displayName = this.findView(TR.itemResponseDisplayName)
  lazy val qualifier = this.findView(TR.itemResponseQualifier)
  lazy val postedDate = this.findView(TR.itemResponsePostedDate)
  lazy val dropdownMenu = this.findView(TR.itemResponseDropdownMenu)
  lazy val plurkAPI = PlurkAPIHelper.getPlurkAPI(activity)

  private var owner: User = _

  private def initView() {
    inflater.inflate(R.layout.item_response, this, true)
    content.setMovementMethod(LinkMovementMethod.getInstance())
  }


  private def showDeleteConfirmDialog(response: Response) {
    val data = new Bundle
    data.putLong("plurkID", response.plurkID)
    data.putLong("responseID", response.id)
    val alertDialog = ConfirmDialog.createDialog(
      activity, 'DeleteResponseConfirm, 
      activity.getString(R.string.viewResponseViewDeleteConfirmTitle),
      activity.getString(R.string.viewResponseViewDeleteConfirm),
      activity.getString(R.string.delete),
      activity.getString(R.string.cancel),
      Some(data)
    )
    
    val fm = activity.getSupportFragmentManager
    alertDialog.show(fm, "DeleteResponseConfirm")
  }

  private def setDropdownMenu(response: Response, isDeletable: Boolean) {
    if (isDeletable) {
      dropdownMenu.setOnClickListener { button: View =>
        val popupMenu = new test.MyPopupMenu(activity, button) {
          override def onMenuItemSelected(menu: MenuBuilder, item: MenuItem): Boolean = {
            item.getItemId match {
              case R.id.popup_comment_delete => showDeleteConfirmDialog(response); true
              case _ => true
            }
          }
        }
        popupMenu.getMenuInflater.inflate(R.menu.popup_comment, popupMenu.getMenu)
        popupMenu.show()
      }
      dropdownMenu.setEnabled(true)
      dropdownMenu.setVisibility(View.VISIBLE)

    } else {
      dropdownMenu.setEnabled(false)
      dropdownMenu.setVisibility(View.GONE)
    }
  }

  def update(response: Response, owner: User, isDeletable: Boolean, 
             imageGetter: PlurkImageGetter): View = {

    this.owner = owner
    content.setText(Html.fromHtml(response.content, imageGetter, StrikeTagHandler))
    postedDate.setText(dateTimeFormatter.format(response.posted))
    displayName.setText(owner.displayName getOrElse owner.nickname)
    setDropdownMenu(response, isDeletable)

    QualifierDisplay(response.qualifier, activity) match {
      case None => qualifier.setVisibility(View.GONE)
      case Some((backgroundColor, translatedName)) =>
        qualifier.setBackgroundColor(backgroundColor)
        qualifier.setText(translatedName)
        qualifier.setVisibility(View.VISIBLE)
    }

    avatar.setImageResource(R.drawable.default_avatar)
    AvatarCache.getAvatarBitmapFromCache(activity, owner) match {
      case Some(avatarBitmap) => setAvatarFromCache(avatarBitmap)
      case None => setAvatarFromNetwork(activity, owner)
    }

    this
  }

  def setAvatarFromCache(avatarBitmap: Bitmap) {
    avatar.setImageBitmap(avatarBitmap)
  }

  def setAvatarFromNetwork(context: Context, user: User) {
    val avatarFuture = AvatarCache.getAvatarBitmapFromNetwork(activity, user)
    avatarFuture.onSuccessInUI { case(userID, bitmap) =>
      // Prevent race condition that cause display incorrect avatar for
      // recylced row view.
      if (userID == owner.id) {
        avatar.setImageBitmap(bitmap)
      }
    }
  }
}

