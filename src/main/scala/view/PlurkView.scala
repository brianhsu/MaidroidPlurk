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

object PlurkView {

  private var plurkCommentCount: Map[Long, Int] = Map.empty
  private var plurkReadStatus: Map[Long, Boolean] = Map.empty
  private var plurkMutedStatus: Map[Long, Boolean] = Map.empty
  private var plurkFavoriteInfo: Map[Long, FavoriteInfo] = Map.empty
  private var plurkReplurkInfo: Map[Long, ReplurkInfo] = Map.empty
  private var plurkContentInfo: Map[Long, (String, Option[String])] = Map.empty

  def getNewPlurkContents = plurkContentInfo

  def updatePlurk(plurkID: Long, content: String, contentRaw: Option[String]) {
    plurkContentInfo += (plurkID -> (content, contentRaw))
  }

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

  trait Listener {
    def startEditActivity(plurk: Plurk)
    def contentCopied()
    def linkCopied()
  }
}

class PlurkView(adapterHolder: Option[PlurkAdapter] = None, 
                isInResponseList: Boolean = false,
                isInUserProfile: Boolean = false)
               (implicit val activity: FragmentActivity with PlurkView.Listener with ConfirmDialog.Listener)
                extends LinearLayout(activity) {

  private val inflater = LayoutInflater.from(activity)
  
  initView()

  lazy val dateTimeFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
  lazy val dateFormatter = new SimpleDateFormat("MM-dd")
 
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
  lazy val lockIcon = this.findView(TR.itemPlurkLockIcon)
  lazy val dropdownMenu = this.findView(TR.itemPlurkDropdownMenu)
  lazy val cakeIcon = this.findView(TR.itemPlurkCake)


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
      val isMinePlurk = PlurkAPIHelper.isMinePlurk(plurk)
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

      replurk.setText(R.string.viewPlurkViewProcessing)
      replurk.setEnabled(false)

      val isReplurkedFuture = replurkInfo.isReplurked match {
        case false => Future {
          plurkAPI.Timeline.replurk(List(plurk.plurkID)).get
          true
        }
        case true => Future {
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
      favorite.setEnabled(true)
    }

    updateFavoriteButtonState()

    favorite.setOnClickListener { view: View =>

      favorite.setText(R.string.viewPlurkViewProcessing)
      favorite.setEnabled(false)

      val isFavoriteFuture = favoriteInfo.isFavorite match {
        case false => Future {
          plurkAPI.Timeline.favoritePlurks(List(plurk.plurkID)).get
          true
        }
        case true => Future {
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
    val isMinePlurk = PlurkAPIHelper.isMinePlurk(plurk)
    def currentMuteState = PlurkView.getPlurkMutedStatus(plurk.plurkID).
                                   getOrElse(plurk.readStatus == Some(Muted))

    def updateMuteButtonState() {
      currentMuteState match {
        case true =>
          mute.setBackgroundResource(R.drawable.rounded_blue)
          mute.setText(R.string.viewPlurkViewUnmute)
          isMuted = true
        case false =>
          mute.setBackgroundResource(R.drawable.rounded_gray)
          mute.setText(R.string.viewPlurkViewMute)
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

        case true => Future { 
          plurkAPI.Timeline.unmutePlurks(List(plurk.plurkID)).get
          'Unmuted  
        }

        case false => Future { 
          plurkAPI.Timeline.mutePlurks(List(plurk.plurkID)).get
          'Muted 
        }
      }


      mute.setEnabled(false)
      mute.setText(R.string.viewPlurkViewProcessing)
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
        replurkerName.setText(user.displayName getOrElse user.nickname)
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

  def update(plurk: Plurk, owner: User, replurker: Option[User], 
             imageGetter: PlurkImageGetter): View = {

    this.ownerID = owner.id
    this.owner = owner
    this.replurker = replurker

    content.setText(Html.fromHtml(plurk.content, imageGetter, StrikeTagHandler))
    postedDate.setText(dateTimeFormatter.format(plurk.posted))
    displayName.setText(owner.displayName getOrElse owner.nickname)

    QualifierDisplay(plurk) match {
      case None => qualifier.setVisibility(View.GONE)
      case Some((backgroundColor, translatedName)) =>
        qualifier.setBackgroundColor(backgroundColor)
        qualifier.setText(translatedName)
        qualifier.setVisibility(View.VISIBLE)
    }


    avatar.setImageResource(R.drawable.default_avatar)
    avatar.setOnClickListener { view: View => UserTimelineActivity.startActivity(activity, owner) }
    displayName.setOnClickListener { view: View => UserTimelineActivity.startActivity(activity, owner) }

    AvatarCache.getAvatarBitmapFromCache(activity, owner) match {
      case Some(avatarBitmap) => setAvatarFromCache(avatarBitmap)
      case None => setAvatarFromNetwork(activity, owner)
    }

    if (plurk.plurkType == PlurkType.Private || 
        plurk.plurkType == PlurkType.PrivateResponded) {

      lockIcon.setVisibility(View.VISIBLE)
    } else {
      lockIcon.setVisibility(View.GONE)
    }

    setCommentInfo(plurk)
    setReplurkInfo(plurk)
    setReplurkerInfo(plurk)
    setFavoriteInfo(plurk)
    setMuteInfo(plurk)
    setDropdownMenu(plurk)
    setCakeIcon(owner)
    this
  }

  private def setCakeIcon(user: User) {
   
    val shouldDisplay = user.birthday match {
      case Some(birthday) => dateFormatter.format(birthday.getTime) == dateFormatter.format(new Date)
      case None => false
    }

    val visibility = if (shouldDisplay) View.VISIBLE else View.GONE
    cakeIcon.setVisibility(visibility)

  }

  private def showDeleteConfirmDialog(plurk: Plurk) {
    val data = new Bundle
    data.putLong("plurkID", plurk.plurkID)
    val alertDialog = ConfirmDialog.createDialog(
      activity,
      'DeletePlurkConfirm, 
      activity.getString(R.string.viewPlurkViewDeleteConfirmTitle),
      activity.getString(R.string.viewPlurkViewDeleteConfirm),
      activity.getString(R.string.delete), 
      activity.getString(R.string.cancel),
      Some(data)
    )
      
    val fm = activity.getSupportFragmentManager
    alertDialog.show(fm, "DeletePlurkConfirm")
  }

  private def copyPlurkContent(plurk: Plurk) {
    val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE).asInstanceOf[ClipboardManager]
    val clipData = ClipData.newPlainText(s"PlurkContent(${plurk.plurkID}", plurk.contentRaw.getOrElse(plurk.content))
    clipboard.setPrimaryClip(clipData)
    Toast.makeText(activity, R.string.contentCopied, Toast.LENGTH_SHORT).show()
    activity.contentCopied()
  }

  private def copyPlurkLink(plurk: Plurk) {
    val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE).asInstanceOf[ClipboardManager]
    val clipData = ClipData.newPlainText(s"PlurkLink(${plurk.plurkID}", plurk.plurkURL)
    clipboard.setPrimaryClip(clipData)
    Toast.makeText(activity, R.string.linkCopied, Toast.LENGTH_SHORT).show()
    activity.linkCopied()
  }

  private def setDropdownMenu(plurk: Plurk) {

    val isAnonymous = {
      plurk.plurkType == PlurkType.Anonymous || 
      plurk.plurkType == PlurkType.AnonymousResponded
    }

    dropdownMenu.setOnClickListener { button: View =>
      val popupMenu = new MyPopupMenu(activity, button) {
        override def onMenuItemSelected(menu: MenuBuilder, item: MenuItem): Boolean = {
          item.getItemId match {
            case R.id.popup_plurk_delete => showDeleteConfirmDialog(plurk); true
            case R.id.popup_plurk_edit => startEditActivity(plurk); true
            case R.id.popup_plurk_copy_content => copyPlurkContent(plurk); true
            case R.id.popup_plurk_copy_link => copyPlurkLink(plurk); true
            case _ => true
          }
        }
      }

      val shouldShowEditDelete = !isInResponseList && (PlurkAPIHelper.isMinePlurk(plurk) || plurk.myAnonymous.getOrElse(false))
      popupMenu.getMenuInflater.inflate(R.menu.popup_plurk, popupMenu.getMenu)

      if (!shouldShowEditDelete) {
        val deleteMenuItem = popupMenu.getMenu.findItem(R.id.popup_plurk_delete)
        val editMenuItem = popupMenu.getMenu.findItem(R.id.popup_plurk_edit)
        deleteMenuItem.setVisible(false)
        editMenuItem.setVisible(false)
      }

      popupMenu.show()
    }

  }

  private def startEditActivity(plurk: Plurk) {
    activity.asInstanceOf[PlurkView.Listener].startEditActivity(plurk)
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

  def setOnCommentCountClickListener(callback: => Any) {
    if (!isInResponseList) {
      content.setOnClickListener { view: View => callback }
    }
    commentCount.setOnClickListener { view: View => callback }
  }
}

