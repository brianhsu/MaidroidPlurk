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
import java.net.URLConnection

import scala.util.{Try, Success}
import android.text.Html
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.text.method.LinkMovementMethod
import android.content.res.Resources
import android.widget.AbsListView.OnScrollListener
import android.widget.AbsListView

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

  private val textViewImageGetter = new Html.ImageGetter() {

    class URLDrawable(resources: Resources, loadingBitmap: Bitmap) extends 
          BitmapDrawable(resources, loadingBitmap) 
    {
      private var drawableHolder: Option[Drawable] = None

      override def draw(canvas: android.graphics.Canvas) {
        drawableHolder match {
          case Some(drawable) => drawable draw canvas
          case None => 
            super.setBounds(0, 0, super.getIntrinsicWidth, super.getIntrinsicHeight)
            super.draw(canvas)
        }
      }

      def updateDrawable(bitmap: Bitmap) {
        val drawable = new BitmapDrawable(activity.getResources, bitmap)
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth, drawable.getIntrinsicHeight)
        drawableHolder = Some(drawable)
      }
    }

    private val loadingImage = BitmapFactory.decodeResource(activity.getResources, R.drawable.default_avatar)

    private def openStream(imageURL: String) = {
      if (imageURL.contains("images.plurk.com/tx_")) {
        val newURL = imageURL.replace("/tx_", "/").replace(".gif", ".jpg")
        val connection = new URL(newURL).openConnection()
        DebugLog("====> newURL:" + newURL)
        connection.getInputStream()
      } else {
        val connection = new URL(imageURL).openConnection()
        connection.getInputStream()
      }
    }

    private def calculateOriginSize(url: String): (Int, Int) = {
      val imgStream = openStream(url)
      val options = new BitmapFactory.Options
      options.inJustDecodeBounds = true
      BitmapFactory.decodeStream(imgStream, null, options)
      imgStream.close()
      (options.outWidth, options.outHeight)
    }

    private def downloadImageFromNetwork(url: String): Bitmap = {
      val (originWidth, originHeight) = calculateOriginSize(url)
      val inSampleSize = calculateInSampleSize(originWidth, originHeight, 240, 160)
      val imgStream = openStream(url)
      val options = new BitmapFactory.Options
      options.inSampleSize = inSampleSize

      if (originWidth <= 48 && originHeight <= 48) {
        options.inDensity = 160
        options.inScaled = false
        options.inTargetDensity = 160
      }

      val imgBitmap = BitmapFactory.decodeStream(imgStream, null, options)
      imgStream.close()
      imageCache += (url -> imgBitmap)
      imgBitmap
    }

    private def calculateInSampleSize(originWidth: Int, originHeight: Int, 
                                      requiredWidth: Int, requiredHeight: Int): Int = {
      var inSampleSize = 1

      if (originHeight > requiredHeight || originWidth > requiredWidth) {

          val halfHeight = originHeight / 2
          val halfWidth = originWidth / 2

          // Calculate the largest inSampleSize value that is a power of 2 and keeps both
          // height and width larger than the requested height and width.
          while ((halfHeight / inSampleSize) > requiredHeight && 
                 (halfWidth / inSampleSize) > requiredWidth) {
              inSampleSize *= 2
          }
      }

      inSampleSize
    }

    private def getDrawableFromNetwork(source: String) = {
      val urlDrawable = new URLDrawable(activity.getResources, loadingImage)
      val bitmapFuture = future { 
        downloadImageFromNetwork(source) 
      }

      bitmapFuture.onFailureInUI { case e: Exception =>
        DebugLog("====> " + e, e)
      }

      bitmapFuture.onSuccessInUI { bitmap =>
        urlDrawable.updateDrawable(bitmap)
        notifyDataSetChanged()
      }

      urlDrawable
    }

    private def getDrawableFromBitmap(bitmap: Bitmap) = {
      val drawable = new BitmapDrawable(activity.getResources, bitmap)
      drawable.setBounds(0, 0, drawable.getIntrinsicWidth, drawable.getIntrinsicHeight)
      drawable
    }

    override def getDrawable(source: String): Drawable = {
      val drawableFromMemory = imageCache.get(source).map(getDrawableFromBitmap)
      drawableFromMemory getOrElse (getDrawableFromNetwork(source))
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

  def loadAvatarBitmapFromNetwork(user: User, imageView: ImageView) {

    val avatarBitmapFuture = future {
      val avatarURLStream = new URL(user.bigAvatar).openStream()
      val avatarBitmap = BitmapFactory.decodeStream(avatarURLStream)
      avatarURLStream.close()
      avatarBitmap
    }

    avatarBitmapFuture.onSuccessInUI { avatarBitmap =>
      DebugLog(s"====> [ok] avatar url of ${user.displayName}: ${user.bigAvatar}")
      avatarCache += (user.id -> avatarBitmap)
      imageView.setImageBitmap(avatarBitmap)
    }

    avatarBitmapFuture.onFailureInUI { case e: Exception =>
      DebugLog(s"====> [failed] avatar url of ${user.displayName}: ${user.bigAvatar}")
    }

  }

  def loadAvatar(user: User, imageView: ImageView) {
    avatarCache.get(user.id) match {
      case Some(bitmap) => imageView.setImageBitmap(bitmap)
      case None => loadAvatarBitmapFromNetwork(user, imageView)
    }
  }

  def appendTimeline(timeline: Timeline) {
    val newUsers = timeline.users.filterKeys(userID => !(users.keySet contains userID))
    plurks ++= timeline.plurks
    users ++= newUsers
    notifyDataSetChanged
  }

  def lastPlurkDate = plurks.last.posted
}

class TimelinePlurksFragment extends Fragment {

  private implicit def activity = getActivity
  private var activityCallback: TimelinePlurksFragment.Listener = _
  private def plurkAPI = activityCallback.onGetPlurkAPI

  private lazy val listView = getView.findView(TR.fragmentTimelinePlurksListView)
  private lazy val adapter = new PlurkAdapter(activity)

  private var isLoadingMore: Boolean = false

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

    val footer = activity.getLayoutInflater.inflate(R.layout.item_loading_footer, null, false)

    listView.setEmptyView(view.findView(TR.fragmentTimelinePlurksEmptyNotice))
    listView.addFooterView(footer)
    listView.setAdapter(adapter)
    listView.setOnScrollListener(new OnScrollListener() {
      def onScroll(view: AbsListView, firstVisibleItem: Int, visibleItemCount: Int, totalItemCount: Int) {
        val isLastItem = (firstVisibleItem + visibleItemCount) == totalItemCount
        val shouldLoadingMore = isLastItem && !isLoadingMore
        if (shouldLoadingMore) {
          loadingMoreItem
        }
      }

      def onScrollStateChanged(view: AbsListView, scrollState: Int) { }

    })
    updateTimeline()
  }

  def loadingMoreItem() {
    this.isLoadingMore = true

    val olderTimelineFuture = future {
      plurkAPI.Timeline.getPlurks(offset = Some(adapter.lastPlurkDate)).get
    }

    olderTimelineFuture.onSuccessInUI { timeline => 
      adapter.appendTimeline(timeline)
      this.isLoadingMore = false
    }

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

