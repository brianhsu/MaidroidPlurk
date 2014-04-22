package idv.brianhsu.maidroid.plurk.adapter

import idv.brianhsu.maidroid.plurk._
import idv.brianhsu.maidroid.plurk.TypedResource._
import idv.brianhsu.maidroid.plurk.util._
import idv.brianhsu.maidroid.ui.util.AsyncUI._
import idv.brianhsu.maidroid.ui.util.CallbackConversions._

import scala.concurrent._

import android.app.Activity
import android.text.method.LinkMovementMethod
import android.text.Html
import android.view.View
import android.view.ViewGroup
import android.view.LayoutInflater
import android.widget.BaseAdapter
import android.widget.ImageView
import android.graphics.BitmapFactory
import android.graphics.Bitmap

import org.bone.soplurk.api.PlurkAPI._
import org.bone.soplurk.constant.ReadStatus._
import org.bone.soplurk.model._

import java.text.SimpleDateFormat
import java.net.URL

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

