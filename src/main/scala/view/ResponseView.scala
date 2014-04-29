package idv.brianhsu.maidroid.plurk.view

import idv.brianhsu.maidroid.plurk._
import idv.brianhsu.maidroid.plurk.fragment._
import idv.brianhsu.maidroid.plurk.TypedResource._
import idv.brianhsu.maidroid.plurk.cache._
import idv.brianhsu.maidroid.plurk.util._
import idv.brianhsu.maidroid.ui.util.AsyncUI._
import idv.brianhsu.maidroid.ui.util.CallbackConversions._

import scala.concurrent._

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.text.method.LinkMovementMethod
import android.text.Html
import android.view.View
import android.view.LayoutInflater
import android.widget.LinearLayout

import org.bone.soplurk.api.PlurkAPI._
import org.bone.soplurk.constant.ReadStatus._
import org.bone.soplurk.model._

import java.text.SimpleDateFormat
import java.net.URL

class ResponseView(implicit val activity: Activity) extends LinearLayout(activity) {

  private val inflater = activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE).
                                  asInstanceOf[LayoutInflater]

  initView()

  lazy val dateTimeFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
  
  lazy val avatar = this.findView(TR.itemResponseAvatar)
  lazy val content = this.findView(TR.itemResponseText)
  lazy val displayName = this.findView(TR.itemResponseDisplayName)
  lazy val qualifier = this.findView(TR.itemResponseQualifier)
  lazy val postedDate = this.findView(TR.itemResponsePostedDate)

  private var owner: User = _

  private def initView() {
    inflater.inflate(R.layout.item_response, this, true)
    content.setMovementMethod(LinkMovementMethod.getInstance())
  }

  def update(response: Response, owner: User, imageGetter: PlurkImageGetter): View = {
    this.owner = owner
    content.setText(Html.fromHtml(response.content, imageGetter, null))
    postedDate.setText(dateTimeFormatter.format(response.posted))
    displayName.setText(owner.displayName)

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

