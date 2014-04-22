package idv.brianhsu.maidroid.plurk.fragment

import idv.brianhsu.maidroid.plurk._
import idv.brianhsu.maidroid.plurk.util._
import idv.brianhsu.maidroid.plurk.TypedResource._
import idv.brianhsu.maidroid.ui.util.AsyncUI._
import idv.brianhsu.maidroid.ui.util.CallbackConversions._

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
import java.text.SimpleDateFormat

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
    def onHideLoadingUI(): Unit
    def onShowTimelinePlurksFailure(e: Exception): Unit
    def onShowTimelinePlurksSuccess(timeline: Timeline): Unit
  }
}

import android.widget.BaseAdapter
import org.bone.soplurk.constant.Qualifier
import org.bone.soplurk.constant.Qualifier._
import org.bone.soplurk.constant.ReadStatus._
import org.bone.soplurk.model.FavoriteInfo
import org.bone.soplurk.model.ReplurkInfo

object QualifierDisplay {

  def apply(plurk: Plurk) = plurk.qualifier match {
    case Asks      => Some((0xff8361BC, plurk.qualifierTranslated.getOrElse(plurk.qualifier.name)))
    case Feels     => Some((0xff2D83BE, plurk.qualifierTranslated.getOrElse(plurk.qualifier.name)))
    case Gives     => Some((0xff620E0E, plurk.qualifierTranslated.getOrElse(plurk.qualifier.name)))
    case Has       => Some((0xff777777, plurk.qualifierTranslated.getOrElse(plurk.qualifier.name)))
    case Hates     => Some((0xff111111, plurk.qualifierTranslated.getOrElse(plurk.qualifier.name)))
    case Hopes     => Some((0xffE05BE9, plurk.qualifierTranslated.getOrElse(plurk.qualifier.name)))
    case Is        => Some((0xffE57C43, plurk.qualifierTranslated.getOrElse(plurk.qualifier.name)))
    case Likes     => Some((0xff8C8C8C, plurk.qualifierTranslated.getOrElse(plurk.qualifier.name)))
    case Loves     => Some((0xffB20C0C, plurk.qualifierTranslated.getOrElse(plurk.qualifier.name)))
    case Needs     => Some((0xff7A9A37, plurk.qualifierTranslated.getOrElse(plurk.qualifier.name)))
    case Says      => Some((0xffE2560B, plurk.qualifierTranslated.getOrElse(plurk.qualifier.name)))
    case Shares    => Some((0xffA74949, plurk.qualifierTranslated.getOrElse(plurk.qualifier.name)))
    case Thinks    => Some((0xff689CC1, plurk.qualifierTranslated.getOrElse(plurk.qualifier.name)))
    case Wants     => Some((0xff8DB241, plurk.qualifierTranslated.getOrElse(plurk.qualifier.name)))
    case Was       => Some((0xff525252, plurk.qualifierTranslated.getOrElse(plurk.qualifier.name)))
    case Whispers  => Some((0xff32007E, plurk.qualifierTranslated.getOrElse(plurk.qualifier.name)))
    case Will      => Some((0xffB46DB9, plurk.qualifierTranslated.getOrElse(plurk.qualifier.name)))
    case Wishes    => Some((0xff5BB017, plurk.qualifierTranslated.getOrElse(plurk.qualifier.name)))
    case Wonders   => Some((0xff2E4E9E, plurk.qualifierTranslated.getOrElse(plurk.qualifier.name)))
    case _ => None
  }

}

object ViewTag {

  private var plurkMutedStatus: Map[Long, Boolean] = Map.empty
  private var plurkFavoriteInfo: Map[Long, FavoriteInfo] = Map.empty
  private var plurkReplurkInfo: Map[Long, ReplurkInfo] = Map.empty

  def updatePlurkReplurkInfo(plurkID: Long, replurkInfo: ReplurkInfo) {
    plurkReplurkInfo += (plurkID -> replurkInfo)
  }

  def updatePlurkFavoriteInfo(plurkID: Long, favoriteInfo: FavoriteInfo) {
    plurkFavoriteInfo += (plurkID -> favoriteInfo)
  }

  def updatePlurkMutedStatus(plurkID: Long, isMuted: Boolean) {
    plurkMutedStatus += (plurkID -> isMuted)
  }

  def getPlurkReplurkInfo(plurkID: Long) = plurkReplurkInfo.get(plurkID)
  def getPlurkFavoriteInfo(plurkID: Long) = plurkFavoriteInfo.get(plurkID)
  def getPlurkMutedStatus(plurkID: Long) = plurkMutedStatus.get(plurkID)
}

class ViewTag(itemView: View)(implicit val activity: Activity) {
  lazy val dateTimeFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
  lazy val avatar = itemView.findView(TR.itemPlurkAvatar)
  lazy val content = itemView.findView(TR.itemPlurkText)
  lazy val displayName = itemView.findView(TR.itemPlurkDisplayName)
  lazy val qualifier = itemView.findView(TR.itemPlurkQualifier)
  lazy val postedDate = itemView.findView(TR.itemPlurkPostedDate)
  lazy val commentCount = itemView.findView(TR.itemPlurkCommentCount)
  lazy val replurk = itemView.findView(TR.itemPlurkReplurk)
  lazy val mute = itemView.findView(TR.itemPlurkMute)
  lazy val favorite = itemView.findView(TR.itemPlurkFavorite)

  private def plurkAPI = PlurkAPIHelper.getPlurkAPI

  content.setMovementMethod(LinkMovementMethod.getInstance())

  private def setReplurkInfo(plurk: Plurk) {

    def replurkInfo = ViewTag.getPlurkReplurkInfo(plurk.plurkID).getOrElse(plurk.replurkInfo)
    def updateReplurkButtonState() {
      val isMinePlurk = plurk.ownerID == plurk.userID
      replurk.setEnabled(!isMinePlurk)

      replurkInfo.isReplurked match {
        case true =>  replurk.setBackgroundResource(R.drawable.rounded_blue)
        case false => replurk.setBackgroundResource(R.drawable.rounded_gray)
      }

      replurkInfo.isReplurkable match {
        case true => replurk.setVisibility(View.VISIBLE)
        case false => replurk.setVisibility(View.GONE)
      }

      replurk.setText(replurkInfo.replurkersCount.toString)
    }

    updateReplurkButtonState()

    replurk.setOnClickListener { view: View =>

      replurk.setText("設定中")
      replurk.setEnabled(false)

      val isReplurkedFuture = replurkInfo.isReplurked match {
        case false => future {
          plurkAPI.Timeline.replurk(List(plurk.plurkID)).get
          true
        }
        case true => future {
          plurkAPI.Timeline.unreplurk(List(plurk.plurkID)).get
          false
        }
      }

      isReplurkedFuture.onSuccessInUI { isReplurked: Boolean =>
        val oldReplurkInfo = replurkInfo
        val newReplurkInfo = isReplurked match {
          case true  => oldReplurkInfo.copy(
            isReplurked = true, 
            replurkersCount = oldReplurkInfo.replurkersCount + 1
          )
          case false => oldReplurkInfo.copy(
            isReplurked = false, 
            replurkersCount = oldReplurkInfo.replurkersCount - 1
          )
        }
        ViewTag.updatePlurkReplurkInfo(plurk.plurkID, newReplurkInfo)
        updateReplurkButtonState()
      }
    }

  }

  private def setFavoriteInfo(plurk: Plurk) {

    def favoriteInfo = ViewTag.getPlurkFavoriteInfo(plurk.plurkID).
                               getOrElse(plurk.favoriteInfo)

    def updateFavoriteButtonState() {

      favorite.setText(favoriteInfo.favoriteCount.toString)
      favoriteInfo.isFavorite match {
        case true =>  favorite.setBackgroundResource(R.drawable.rounded_blue)
        case false => favorite.setBackgroundResource(R.drawable.rounded_gray)
      }
    }

    updateFavoriteButtonState()

    favorite.setOnClickListener { view: View =>

      favorite.setText("設定中")
      favorite.setEnabled(false)

      val isFavoriteFuture = favoriteInfo.isFavorite match {
        case false => future {
          plurkAPI.Timeline.favoritePlurks(List(plurk.plurkID)).get
          true
        }
        case true => future {
          plurkAPI.Timeline.unfavoritePlurks(List(plurk.plurkID)).get
          false
        }
      }

      isFavoriteFuture.onSuccessInUI { isFavorite: Boolean =>
        val oldFavoriteInfo = favoriteInfo
        val newFavoriteInfo = isFavorite match {
          case true  => oldFavoriteInfo.copy(
            isFavorite = true, 
            favoriteCount = oldFavoriteInfo.favoriteCount + 1
          )
          case false => oldFavoriteInfo.copy(
            isFavorite = false, 
            favoriteCount = oldFavoriteInfo.favoriteCount - 1
          )
        }
        ViewTag.updatePlurkFavoriteInfo(plurk.plurkID, newFavoriteInfo)
        updateFavoriteButtonState()
      }
    }

  }

  private def setMuteInfo(plurk: Plurk) {

    var isMuted: Boolean = false
    val isMinePlurk = plurk.ownerID == plurk.userID
    def currentMuteState = ViewTag.getPlurkMutedStatus(plurk.plurkID).
                                   getOrElse(plurk.readStatus == Some(Muted))

    def updateMuteButtonState() {
      currentMuteState match {
        case true =>
          mute.setBackgroundResource(R.drawable.rounded_blue)
          mute.setText("解除消音")
          isMuted = true
        case false =>
          mute.setBackgroundResource(R.drawable.rounded_gray)
          mute.setText("消音")
          isMuted = false
      }

      isMinePlurk match {
        case true  => mute.setVisibility(View.GONE)
        case false => mute.setVisibility(View.VISIBLE)
      }
    }


    updateMuteButtonState()
    mute.setOnClickListener { view: View =>

      val newMutedStatusFuture = isMuted match {

        case true => future { 
          plurkAPI.Timeline.unmutePlurks(List(plurk.plurkID)).get
          'Unmuted  
        }

        case false => future { 
          plurkAPI.Timeline.mutePlurks(List(plurk.plurkID)).get
          'Muted 
        }
      }


      mute.setEnabled(false)
      mute.setText("設定中")
      newMutedStatusFuture.onSuccessInUI { status: Symbol =>

        status match {
          case 'Muted => ViewTag.updatePlurkMutedStatus(plurk.plurkID, isMuted = true)
          case 'Unmuted => ViewTag.updatePlurkMutedStatus(plurk.plurkID, isMuted = false)
        }

        updateMuteButtonState()
        mute.setEnabled(true)
        
      }

      newMutedStatusFuture.onFailureInUI { 
        case e: Exception => DebugLog("[failed] ====> failed:" + e, e)
      }

    }
  }

  private def setCommentInfo(plurk: Plurk) {
    commentCount.setText(plurk.responseCount.toString)

    plurk.readStatus match {
      case Some(Unread) => commentCount.setBackgroundResource(R.drawable.rounded_red)
      case _ => commentCount.setBackgroundResource(R.drawable.rounded_gray)
    }
  }

  def update(owner: User, plurk: Plurk, imageGetter: PlurkImageGetter) {
    content.setText(Html.fromHtml(plurk.content, imageGetter, null))
    postedDate.setText(dateTimeFormatter.format(plurk.posted))
    displayName.setText(owner.displayName)
    QualifierDisplay(plurk) match {
      case None => qualifier.setVisibility(View.GONE)
      case Some((backgroundColor, translatedName)) =>
        qualifier.setBackgroundColor(backgroundColor)
        qualifier.setText(translatedName)
        qualifier.setVisibility(View.VISIBLE)
    }

    setCommentInfo(plurk)
    setReplurkInfo(plurk)
    setFavoriteInfo(plurk)
    setMuteInfo(plurk)
  }
}

class PlurkImageGetter(activity: Activity, adapter: BaseAdapter, loadingImage: Bitmap) extends Html.ImageGetter {

  private implicit val implicitActivity = activity
  private val imageCache = new LRUCache[String, Bitmap](5)

  class URLDrawable(resources: Resources, loadingBitmap: Bitmap) extends 
        BitmapDrawable(resources, loadingBitmap) 
  {
    private var drawableHolder: Option[Drawable] = None

    this.setBounds(0, 0, super.getIntrinsicWidth, super.getIntrinsicHeight)

    override def draw(canvas: android.graphics.Canvas) {
      drawableHolder match {
        case Some(drawable) => drawable draw canvas
        case None => 
          super.draw(canvas)
      }
    }

    def updateDrawable(bitmap: Bitmap) {
      val drawable = new BitmapDrawable(activity.getResources, bitmap)
      drawable.setBounds(0, 0, drawable.getIntrinsicWidth, drawable.getIntrinsicHeight)
      drawableHolder = Some(drawable)
    }
  }

  private def openStream(imageURL: String) = {
    if (imageURL.contains("images.plurk.com/tx_")) {
      val newURL = imageURL.replace("/tx_", "/").replace(".gif", ".jpg")
      val connection = new URL(newURL).openConnection()
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
    val inSampleSize = calculateInSampleSize(originWidth, originHeight, 150, 150)
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

    bitmapFuture.onSuccessInUI { bitmap =>
      urlDrawable.updateDrawable(bitmap)
      adapter.notifyDataSetChanged()
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

  def getLRUCache = imageCache

}

class PlurkAdapter(activity: Activity) extends BaseAdapter {
  private implicit val mActivity = activity
  private var plurks: Vector[Plurk] = Vector.empty
  private var users: Map[Long, User] = Map.empty
  private val avatarCache = new LRUCache[Long, Bitmap](50)
  private val layoutInflater = LayoutInflater.from(activity)
  private val textViewImageGetter = new PlurkImageGetter(activity, this, loadingImage)
  private val loadingImage = BitmapFactory.decodeResource(activity.getResources, R.drawable.default_avatar)

  def getCount = plurks.size
  def getItem(position: Int) = plurks(position)
  def getItemId(position: Int) = plurks(position).plurkID
  
  def createNewView(plurk: Plurk, owner: User, parent: ViewGroup): View = {

    val view = layoutInflater.inflate(R.layout.item_plurk, parent, false)
    val viewTag = new ViewTag(view)
    viewTag.update(owner, plurk, textViewImageGetter)
    view.setTag(viewTag)
    loadAvatar(owner, viewTag.avatar)
    view
  }

  def getView(position: Int, convertView: View, parent: ViewGroup): View = {

    val plurk = plurks(position)
    val owner = users(plurk.ownerID)

    convertView match {
      case null => createNewView(plurk, owner, parent)
      case view => 
        val viewTag = view.getTag.asInstanceOf[ViewTag]
        viewTag.update(owner, plurk, textViewImageGetter)
        loadAvatar(owner, viewTag.avatar)
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
      avatarCache += (user.id -> avatarBitmap)
      imageView.setImageBitmap(avatarBitmap)
      notifyDataSetChanged()
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
  private def plurkAPI = PlurkAPIHelper.getPlurkAPI

  private lazy val listView = getView.findView(TR.fragmentTimelinePlurksListView)
  private lazy val adapter = new PlurkAdapter(activity)
  private lazy val footer = activity.getLayoutInflater.
                                     inflate(R.layout.item_loading_footer, null, false)

  private var isLoadingMore: Boolean = false

  override def onAttach(activity: Activity) {
    super.onAttach(activity)
    try {
      activityCallback = activity.asInstanceOf[TimelinePlurksFragment.Listener]
    } catch {
      case e: ClassCastException => 
        throw new ClassCastException(
          s"${activity} must mixed with TimelinePlurksFragment.Listener"
        )
    }
  }

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, 
                            savedInstanceState: Bundle): View = {
    inflater.inflate(R.layout.fragment_timeline_plurks, container, false)
  }

  override def onViewCreated(view: View, savedInstanceState: Bundle) {

    listView.setEmptyView(view.findView(TR.fragmentTimelinePlurksEmptyNotice))
    listView.addFooterView(footer)
    listView.setAdapter(adapter)
    listView.setOnScrollListener(new OnScrollListener() {
      def onScroll(view: AbsListView, firstVisibleItem: Int, 
                   visibleItemCount: Int, totalItemCount: Int) {

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

      timeline.plurks.isEmpty match {
        case true   => footer.setVisibility(View.GONE)
        case faslse => footer.setVisibility(View.VISIBLE)
      }

      this.isLoadingMore = false
    }

  }

  def updateTimeline() {

    val plurksFuture = future { 
      plurkAPI.Timeline.getPlurks().get 
    }

    plurksFuture.onSuccessInUI { timeline => 
      adapter.appendTimeline(timeline)
      activityCallback.onHideLoadingUI()
      activityCallback.onShowTimelinePlurksSuccess(timeline)
    }

    plurksFuture.onFailureInUI { case e: Exception =>
      activityCallback.onShowTimelinePlurksFailure(e)
    }
  }

}

