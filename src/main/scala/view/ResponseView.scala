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
import android.graphics.Bitmap
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.support.v7.internal.view.menu.MenuBuilder
import android.support.v7.widget.PopupMenu
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast

import org.bone.soplurk.api.PlurkAPI._
import org.bone.soplurk.constant.ReadStatus._
import org.bone.soplurk.model._

import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date

class ResponseView(adapter: ResponseAdapter)
                  (implicit val activity: FragmentActivity with ConfirmDialog.Listener with ResponseListFragment.Listener)  
                  extends LinearLayout(activity) {

  private val inflater = LayoutInflater.from(activity)

  initView()

  lazy val dateTimeFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
  lazy val dateFormatter = new SimpleDateFormat("MM-dd")
 
  lazy val avatar = this.findView(TR.itemResponseAvatar)
  lazy val content = this.findView(TR.itemResponseText)
  lazy val displayName = this.findView(TR.itemResponseDisplayName)
  lazy val qualifier = this.findView(TR.itemResponseQualifier)
  lazy val postedDate = this.findView(TR.itemResponsePostedDate)
  lazy val dropdownMenu = this.findView(TR.itemResponseDropdownMenu)
  lazy val cakeIcon = this.findView(TR.itemResponseCake)
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

  private def showBlockConfirmDialog(response: Response) {
    val data = new Bundle
    data.putLong("plurkID", response.plurkID)
    data.putLong("responseID", response.id)
    data.putLong("ownerID", response.userID)
    val alertDialog = ConfirmDialog.createDialog(
      activity, 'BlockUserResponseConfirm, 
      activity.getString(R.string.viewResponseViewBlockConfirmTitle),
      activity.getString(R.string.viewResponseViewBlockConfirm),
      activity.getString(R.string.delete),
      activity.getString(R.string.cancel),
      Some(data)
    )
    
    val fm = activity.getSupportFragmentManager
    alertDialog.show(fm, "BlockUserResponseConfirm")
  }

  private def copyContent(response: Response) {
    val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE).asInstanceOf[ClipboardManager]
    val clipData = ClipData.newPlainText(s"PlurkResponse(${response.id}", response.contentRaw)
    clipboard.setPrimaryClip(clipData)
    Toast.makeText(activity, R.string.contentCopied, Toast.LENGTH_SHORT).show()
  }

  private def setDropdownMenu(response: Response, isDeletable: Boolean) {

    dropdownMenu.setOnClickListener { button: View =>
      val popupMenu = new MyPopupMenu(activity, button) {
        override def onMenuItemSelected(menu: MenuBuilder, item: MenuItem): Boolean = {
          item.getItemId match {
            case R.id.popup_comment_copy_content => copyContent(response); true
            case R.id.popup_comment_block => showBlockConfirmDialog(response); true
            case R.id.popup_comment_delete => showDeleteConfirmDialog(response); true
            case R.id.popup_comment_reply => activity.onReplyTo(owner.nickname, response.contentRaw); true
            case _ => true
          }
        }
      }

      popupMenu.getMenuInflater.inflate(R.menu.popup_comment, popupMenu.getMenu)

      val isMineResponse = response.myAnonymous getOrElse (PlurkAPIHelper.plurkUserID == response.userID)

      if (!isDeletable) {
        val deleteMenuItem = popupMenu.getMenu.findItem(R.id.popup_comment_delete)
        deleteMenuItem.setVisible(false)
      }

      if (!isDeletable || isMineResponse || response.userID == 99999) {
        val blockMenuItem = popupMenu.getMenu.findItem(R.id.popup_comment_block)
        blockMenuItem.setVisible(false)
      }

      popupMenu.show()
    }
  }

  private def setCakeIcon(user: User) {
   
    val shouldDisplay = user.birthday match {
      case Some(birthday) => dateFormatter.format(birthday.getTime) == dateFormatter.format(new Date)
      case None => false
    }

    val visibility = if (shouldDisplay) View.VISIBLE else View.GONE
    cakeIcon.setVisibility(visibility)

  }

  def update(response: Response, owner: User, isDeletable: Boolean, 
             imageGetter: PlurkImageGetter): View = {

    this.owner = owner
    content.setText(Html.fromHtml(response.content, imageGetter, StrikeTagHandler))
    postedDate.setText(dateTimeFormatter.format(response.posted))
    displayName.setText((response.handle orElse owner.displayName) getOrElse owner.nickname)
    displayName.setOnClickListener { view: View => UserTimelineActivity.startActivity(activity, owner) }
    avatar.setOnClickListener { view: View => UserTimelineActivity.startActivity(activity, owner) }
    setDropdownMenu(response, isDeletable)
    setCakeIcon(owner)

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

