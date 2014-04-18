package idv.brianhsu.maidroid.plurk.fragment

import idv.brianhsu.maidroid.plurk._
import idv.brianhsu.maidroid.plurk.util._
import idv.brianhsu.maidroid.plurk.TypedResource._
import idv.brianhsu.maidroid.ui.util.AsyncUI._

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.net.Uri
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.View
import android.webkit.WebViewClient
import android.webkit.WebView
import android.view.LayoutInflater
import android.widget.ImageView

import org.bone.soplurk.api._
import org.bone.soplurk.api.PlurkAPI.Timeline
import org.bone.soplurk.model._
import scala.concurrent._
import java.net.URL
import scala.util.{Try, Success}
import android.text.Html
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.text.method.LinkMovementMethod

object TimelinePlurksFragment {
  trait Listener {
    def onGetPlurkAPI: PlurkAPI
    def onHideLoadingUI(): Unit
    def onShowTimelinePlurksFailure(e: Exception): Unit
    def onShowTimelinePlurksSuccess(timeline: Timeline): Unit
  }
}

import android.widget.BaseAdapter

class ViewTag(var userID: Long, itemView: View) {
  lazy val avatar = itemView.findView(TR.itemPlurkAvatar)
  lazy val content = itemView.findView(TR.itemPlurkText)

  content.setMovementMethod(LinkMovementMethod.getInstance())
}

class PlurkAdapter(context: Activity) extends BaseAdapter {
  private implicit val activity = context
  private var plurks: Vector[Plurk] = Vector.empty
  private var users: Map[Long, User] = Map.empty
  private var avatarCache: Map[Long, Bitmap] = Map.empty
  private var imageCache: Map[String, Bitmap] = Map.empty

  private val layoutInflater = LayoutInflater.from(context)

  private def downloadImageFromNetwork(url: String): Try[Bitmap] = Try {
    val imgStream = new URL(url).openStream()
    val imgBitmap = BitmapFactory.decodeStream(imgStream)
    imgStream.close()
    imageCache += (url -> imgBitmap)
    imgBitmap
  }

  private val textViewImageGetter = new Html.ImageGetter() {
    override def getDrawable(source: String): Drawable = {

      def bitmapFromHTTPOrCache = imageCache.get(source) match {
        case Some(bitmap) => Success(bitmap)
        case None => downloadImageFromNetwork(source)
      }

      val drawableHolder = 
        source.startsWith("http") match {
          case true => bitmapFromHTTPOrCache.map(new BitmapDrawable(context.getResources, _))
          case false => Try(Drawable.createFromPath(source))
        }

      drawableHolder.foreach { drawable =>
        drawable.setBounds(
          0, 0, 
          drawable.getIntrinsicWidth, drawable.getIntrinsicHeight()
        )
      }

      drawableHolder.getOrElse(null)
    }
  }

  def getCount = plurks.size
  def getItem(position: Int) = plurks(position)
  def getItemId(position: Int) = plurks(position).plurkID
  
  def createNewView(plurk: Plurk, owner: User, parent: ViewGroup): View = {

    val view = layoutInflater.inflate(R.layout.item_plurk, parent, false)
    val viewTag = new ViewTag(owner.id, view)

    viewTag.content.setText(Html.fromHtml(plurk.content, textViewImageGetter, null))
    loadAvatar(owner, viewTag.avatar)
    view.setTag(viewTag)
    view
  }

  def getView(position: Int, convertView: View, parent: ViewGroup): View = {

    val plurk = plurks(position)
    val owner = users(plurk.ownerID)

    convertView match {
      case null => createNewView(plurk, owner, parent)
      case view => 
        val viewTag = view.getTag.asInstanceOf[ViewTag]
        viewTag.content.setText(Html.fromHtml(plurk.content, textViewImageGetter, null))

        if (viewTag.userID != owner.id) {
          loadAvatar(owner, viewTag.avatar)
          viewTag.userID = owner.id
        }
        view
    }
  }

  def loadAvatarBitmapFromNetwork(user: User): Try[Bitmap] = Try {
    val avatarURLStream = new URL(user.bigAvatar).openStream()
    val avatarBitmap = BitmapFactory.decodeStream(avatarURLStream)
    avatarURLStream.close()
    avatarCache += (user.id -> avatarBitmap)
    avatarBitmap
  }

  def loadAvatar(user: User, imageView: ImageView) {
    val avatarBitmapFuture = future { loadAvatarBitmapFromNetwork(user).get }
    avatarBitmapFuture.onSuccessInUI { bitmap =>
      DebugLog(s"====> [ok] avatar url of ${user.displayName}: ${user.bigAvatar}")
      imageView.setImageBitmap(bitmap)
    }

    avatarBitmapFuture.onFailureInUI { case e: Exception =>
      DebugLog(s"====> [failed] avatar url of ${user.displayName}: ${user.bigAvatar}")
    }

  }

  def appendTimeline(timeline: Timeline) {
    val newUsers = timeline.users.filterKeys(userID => !(users.keySet contains userID))
    plurks ++= timeline.plurks
    users ++= newUsers
    notifyDataSetChanged
  }
}

class TimelinePlurksFragment extends Fragment {

  private implicit def activity = getActivity
  private var activityCallback: TimelinePlurksFragment.Listener = _
  private def plurkAPI = activityCallback.onGetPlurkAPI

  private lazy val listView = getView.findView(TR.fragmentTimelinePlurksListView)
  private lazy val adapter = new PlurkAdapter(activity)

  override def onAttach(activity: Activity) {
    super.onAttach(activity)
    try {
      activityCallback = activity.asInstanceOf[TimelinePlurksFragment.Listener]
    } catch {
      case e: ClassCastException => 
        throw new ClassCastException(s"${activity} must mixed with TimelinePlurksFragment.Listener")
    }
  }

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, 
                            savedInstanceState: Bundle): View = {
    inflater.inflate(R.layout.fragment_timeline_plurks, container, false)
  }

  override def onViewCreated(view: View, savedInstanceState: Bundle) {
    listView.setEmptyView(view.findView(TR.fragmentTimelinePlurksEmptyNotice))
    listView.setAdapter(adapter)
    updateTimeline()
  }

  def updateTimeline() {

    val plurksFuture = future { 
      Thread.sleep(10 * 1000)
      plurkAPI.Timeline.getPlurks().get 
    }

    plurksFuture.onSuccessInUI { timeline => 
      DebugLog("==> plurksFuture.onSuccessInUI")
      adapter.appendTimeline(timeline)
      activityCallback.onHideLoadingUI()
      activityCallback.onShowTimelinePlurksSuccess(timeline)
    }

    plurksFuture.onFailureInUI { case e: Exception =>
      DebugLog("==> plurksFuture.onFailureInUI" + e, e)
      activityCallback.onShowTimelinePlurksFailure(e)
    }
  }

}

