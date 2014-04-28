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

object PlurkView {

  private var plurkCommentCount: Map[Long, Int] = Map.empty
  private var plurkReadStatus: Map[Long, Boolean] = Map.empty
  private var plurkMutedStatus: Map[Long, Boolean] = Map.empty
  private var plurkFavoriteInfo: Map[Long, FavoriteInfo] = Map.empty
  private var plurkReplurkInfo: Map[Long, ReplurkInfo] = Map.empty

  def updatePlurkCommentInfo(plurkID: Long, newCount: Int, newReadStatus: Boolean) {
    plurkCommentCount += (plurkID -> newCount)
    plurkReadStatus += (plurkID -> newReadStatus)
  }

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

class PlurkView(isInResponseList: Boolean = false)(implicit val activity: Activity)
                extends LinearLayout(activity) {

  private val inflater = activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE).
                                  asInstanceOf[LayoutInflater]

  initView()

  lazy val dateTimeFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
  
  lazy val avatar = this.findView(TR.itemPlurkAvatar)
  lazy val content = this.findView(TR.itemPlurkText)
  lazy val displayName = this.findView(TR.itemPlurkDisplayName)
  lazy val qualifier = this.findView(TR.itemPlurkQualifier)
  lazy val postedDate = this.findView(TR.itemPlurkPostedDate)
  lazy val commentCount = this.findView(TR.itemPlurkCommentCount)
  lazy val replurk = this.findView(TR.itemPlurkReplurk)
  lazy val mute = this.findView(TR.itemPlurkMute)
  lazy val favorite = this.findView(TR.itemPlurkFavorite)
  lazy val replurkerName = this.findView(TR.itemPlurkReplurkerName)
  lazy val replurkerBlock = this.findView(TR.itemPlurkReplurkerBlock)

  private var ownerID: Long = 0
  private var owner: User = _
  private var replurker: Option[User] = None
  private def plurkAPI = PlurkAPIHelper.getPlurkAPI(activity)

  private def initView() {
    inflater.inflate(R.layout.item_plurk, this, true)
    content.setMovementMethod(LinkMovementMethod.getInstance())
  }

  private def setReplurkInfo(plurk: Plurk) {

    def replurkInfo = PlurkView.getPlurkReplurkInfo(plurk.plurkID).getOrElse(plurk.replurkInfo)
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
        PlurkView.updatePlurkReplurkInfo(plurk.plurkID, newReplurkInfo)
        updateReplurkButtonState()
      }
    }

  }

  private def setFavoriteInfo(plurk: Plurk) {

    def favoriteInfo = PlurkView.getPlurkFavoriteInfo(plurk.plurkID).
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
        PlurkView.updatePlurkFavoriteInfo(plurk.plurkID, newFavoriteInfo)
        updateFavoriteButtonState()
      }
    }

  }

  private def setMuteInfo(plurk: Plurk) {

    var isMuted: Boolean = false
    val isMinePlurk = plurk.ownerID == plurk.userID
    def currentMuteState = PlurkView.getPlurkMutedStatus(plurk.plurkID).
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
          case 'Muted => PlurkView.updatePlurkMutedStatus(plurk.plurkID, isMuted = true)
          case 'Unmuted => PlurkView.updatePlurkMutedStatus(plurk.plurkID, isMuted = false)
        }

        updateMuteButtonState()
        mute.setEnabled(true)
        
      }

      newMutedStatusFuture.onFailureInUI { 
        case e: Exception => DebugLog("[failed] ====> failed:" + e, e)
      }

    }
  }

  private def setReplurkerInfo(plurk: Plurk) {

    replurker match {
      case None => replurkerBlock.setVisibility(View.GONE)
      case Some(user) =>
        replurkerName.setText(user.displayName)
        replurkerBlock.setVisibility(View.VISIBLE)
    }
  }

  private def setCommentInfo(plurk: Plurk) {

    val plurkCommentCount = PlurkView.plurkCommentCount.get(plurk.plurkID).getOrElse(plurk.responseCount).toString
    val isRead = PlurkView.plurkReadStatus.get(plurk.plurkID).getOrElse(plurk.readStatus != Some(Unread))

    commentCount.setText(plurkCommentCount)

    isRead match {
      case true  => commentCount.setBackgroundResource(R.drawable.rounded_gray)
      case false => commentCount.setBackgroundResource(R.drawable.rounded_red)
    }

    if (isInResponseList) {
      commentCount.setVisibility(View.GONE)
    }
  }

  def update(plurk: Plurk, owner: User, replurker: Option[User], imageGetter: PlurkImageGetter): View = {
    this.ownerID = owner.id
    this.owner = owner
    this.replurker = replurker

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

    avatar.setImageResource(R.drawable.default_avatar)
    AvatarCache.getAvatarBitmap(owner) match {
      case Some(avatarBitmap) => setAvatarFromCache(avatarBitmap)
      case None => setAvatarFromNetwork(owner)
    }

    setCommentInfo(plurk)
    setReplurkInfo(plurk)
    setReplurkerInfo(plurk)
    setFavoriteInfo(plurk)
    setMuteInfo(plurk)
    this
  }

  def setAvatarFromCache(avatarBitmap: Bitmap) {
    avatar.setImageBitmap(avatarBitmap)
  }

  def setAvatarFromNetwork(user: User) {
    val avatarFuture = AvatarCache.getAvatarBitmapFromNetwork(user)
    avatarFuture.onSuccessInUI { case(userID, bitmap) =>
      // Prevent race condition that cause display incorrect avatar for
      // recylced row view.
      if (userID == ownerID) {
        avatar.setImageBitmap(bitmap)
      }
    }
  }

  def setOnCommentCountClickListener(callback: => Any) {
    commentCount.setOnClickListener { view: View => callback }
  }
}

