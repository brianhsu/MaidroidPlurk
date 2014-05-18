package idv.brianhsu.maidroid.plurk.activity

import idv.brianhsu.maidroid.plurk.view._

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.DialogInterface
import android.view.View
import android.view.Menu
import android.view.MenuItem

import android.support.v7.app.ActionBarActivity
import android.support.v7.app.ActionBar

import idv.brianhsu.maidroid.plurk._
import idv.brianhsu.maidroid.plurk.fragment._
import idv.brianhsu.maidroid.plurk.util._
import idv.brianhsu.maidroid.plurk.adapter._
import idv.brianhsu.maidroid.plurk.dialog._
import idv.brianhsu.maidroid.ui.model._
import idv.brianhsu.maidroid.ui.util.AsyncUI._

import org.bone.soplurk.api.PlurkAPI.Timeline
import org.bone.soplurk.model.Plurk
import org.bone.soplurk.model.User
import org.bone.soplurk.constant.Filter
import org.bone.soplurk.constant.Filter._

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity

import scala.concurrent._
import scala.util.Try

import org.bone.soplurk.constant.Filter

class MaidroidPlurk extends ActionBarActivity with TypedViewHolder
                    with LoginFragment.Listener 
                    with TimelineFragment.Listener
                    with PlurkView.Listener
                    with ConfirmDialog.Listener
{
  implicit val activity = this

  private lazy val dialogFrame = findView(TR.dialogFrame)
  private lazy val fragmentContainer = findView(TR.activityMaidroidPlurkFragmentContainer)
  private var timelineFragmentHolder: Option[TimelineFragment] = None

  def onGetAuthURLFailure(error: Exception) {
    dialogFrame.setMessages(
      Message(MaidMaro.Half.Panic, getString(R.string.activityMaidroidPlurkGetAuthFailure01)) ::
      Message(MaidMaro.Half.Panic, getString(R.string.activityMaidroidPlurkGetAuthFailure02)) :: 
      Message(MaidMaro.Half.Normal, getString(R.string.activityMaidroidPlurkGetAuthFailure03)) :: 
      Message(MaidMaro.Half.Normal, getString(R.string.activityMaidroidPlurkGetAuthFailure04).format(error.getMessage)) :: Nil
    )
  }

  def onLoginFailure(error: Exception) {
    dialogFrame.setMessages(
      Message(MaidMaro.Half.Normal, getString(R.string.activityMaidroidPlurkLoginFailure01)) :: 
      Message(MaidMaro.Half.Normal, getString(R.string.activityMaidroidPlurkLoginFailure02)) :: 
      Message(MaidMaro.Half.Normal, getString(R.string.activityMaidroidPlurkLoginFailure03).format(error.getMessage)) :: Nil
    )
  }

  def onLoginSuccess() {
    switchToFragment(new TimelineFragment, isForcing = true)
    dialogFrame.setMessages(
      Message(MaidMaro.Half.Happy, getString(R.string.activityMaidroidPlurkLoginSuccess01)) :: 
      Message(MaidMaro.Half.Smile, getString(R.string.activityMaidroidPlurkLoginSuccess02)) ::
      Nil
    )

  }

  def onShowTimelinePlurksFailure(error: Exception) {
    if (error.getMessage.contains("invalid access token")) {
      switchToFragment(new LoginFragment, isForcing = true)
    } else {
      dialogFrame.setMessages(
        Message(MaidMaro.Half.Panic, getString(R.string.activityMaidroidPlurkTimelineFailure01)) :: 
        Message(MaidMaro.Half.Normal, getString(R.string.activityMaidroidPlurkTimelineFailure02)) ::
        Message(MaidMaro.Half.Normal, getString(R.string.activityMaidroidPlurkTimelineFailure03).format(error.getMessage)) :: 
        Nil
      )
    }
  }

  def onShowTimelinePlurksSuccess(timeline: Timeline, isNewFilter: Boolean, 
                                  filter: Option[Filter], isOnlyUnread: Boolean) {

    val unreadText = isOnlyUnread match {
      case true  => getString(R.string.activityMaidroidPlurkUnread)
      case false => ""
    }

    val filterText = filter match {
      case Some(OnlyUser) => 
        getString(R.string.activityMaidroidPlurkFilterUser).format(unreadText)
      case Some(OnlyPrivate) => 
        getString(R.string.activityMaidroidPlurkFilterPrivate).format(unreadText)
      case Some(OnlyResponded) => 
        getString(R.string.activityMaidroidPlurkFilterRespond).format(unreadText)
      case Some(OnlyFavorite) => 
        getString(R.string.activityMaidroidPlurkFilterFavorite).format(unreadText)
      case _ => 
        getString(R.string.activityMaidroidPlurkFilterAll).format(unreadText)
    }

    dialogFrame.setMessages(
      Message(MaidMaro.Half.Smile, getString(R.string.activityMaidroidPlurkTimelineSuccess01).format(filterText)) ::
      Message(MaidMaro.Half.Happy, getString(R.string.activityMaidroidPlurkTimelineSuccess02)) :: 
      Message(MaidMaro.Half.Smile, getString(R.string.activityMaidroidPlurkTimelineSuccess03)) :: 
      Nil
    )
  }

  override def onCreate(savedInstanceState: Bundle) {

    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_maidroid_plurk)

    val actionBar = getSupportActionBar
    actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD)
    actionBar.setDisplayShowTitleEnabled(true)

    dialogFrame.setMessages(
      Message(MaidMaro.Half.Smile, getString(R.string.activityMaidroidPlurkWelcome01)) :: 
      Message(MaidMaro.Half.Happy, getString(R.string.activityMaidroidPlurkWelcome02)) :: 
      Message(MaidMaro.Half.Normal, getString(R.string.activityMaidroidPlurkWelcome03)) :: Nil
    )

    if (PlurkAPIHelper.isLoggedIn(this)) {
      switchToFragment(new TimelineFragment)
    } else {
      switchToFragment(new LoginFragment, isForcing = true)
    }
  }

  override def onRefreshTimelineFailure(e: Exception) {
    dialogFrame.setMessages(
      Message(MaidMaro.Half.Normal, getString(R.string.activityMaidroidPlurkRefreshFailure01)) :: 
      Message(MaidMaro.Half.Panic, getString(R.string.activityMaidroidPlurkRefreshFailure02).format(e.getMessage)) :: 
      Message(MaidMaro.Half.Normal, getString(R.string.activityMaidroidPlurkRefreshFailure03)) :: Nil
    )
  }

  override def onRefreshTimelineSuccess(newTimeline: Timeline) {

    val count = newTimeline.plurks.size
    if (count > 0) {
      dialogFrame.setMessages(
        Message(MaidMaro.Half.Happy, getString(R.string.activityMaidroidPlurkTimelineOK01).format(count)) :: 
        Message(MaidMaro.Half.Smile, getString(R.string.activityMaidroidPlurkTimelineOK02)) :: 
        Nil
      )
    } else {
      dialogFrame.setMessages(
        Message(MaidMaro.Half.Normal, getString(R.string.activityMaidroidPlurkTimelineEmpty01)) :: 
        Message(MaidMaro.Half.Smile, getString(R.string.activityMaidroidPlurkTimelineEmpty02)) :: 
        Nil
      )
    }
  }

  override def onDeletePlurkSuccess() {
    dialogFrame.setMessages(
      Message(MaidMaro.Half.Happy, getString(R.string.activityMaidroidPlurkDeleteOK)) :: Nil
    )
  }

  private def switchToFragment[T <: Fragment](fragment: T, addToBackStack: Boolean = false, isForcing: Boolean = false) {


    val topFragment = Try(getSupportFragmentManager.findFragmentById(R.id.activityMaidroidPlurkFragmentContainer).asInstanceOf[T]).filter(_ != null)

    if (isForcing || topFragment.isFailure) {

      if (fragment.isInstanceOf[TimelineFragment]) {
        timelineFragmentHolder = Some(fragment.asInstanceOf[TimelineFragment])
      } else {
        timelineFragmentHolder = None
      }

      val transaction = getSupportFragmentManager.beginTransaction
      transaction.replace(R.id.activityMaidroidPlurkFragmentContainer, fragment)

      if (addToBackStack) {
        transaction.addToBackStack(null)
      }

      transaction.commit()
    }
  }

  override def startEditActivity(plurk: Plurk) {
    timelineFragmentHolder.foreach(_.startEditActivity(plurk))
  }

  override def onDialogOKClicked(dialogName: Symbol, dialog: DialogInterface, data: Bundle) {
    dialogName match {
      case 'LogoutConfirm => 
        dialog.dismiss()
        Logout.doLogout(this)
      case 'DeletePlurkConfirm =>
        val plurkID = data.getLong("plurkID")
        deletePlurk(plurkID)
      case 'MarkAllAsReadConfirm =>
        val filter = Option(data.getString("filterName")) match {
          case Some("only_user")      => Some(Filter.OnlyUser)
          case Some("only_responded") => Some(Filter.OnlyResponded)
          case Some("only_private")   => Some(Filter.OnlyPrivate)
          case Some("only_favorite")  => Some(Filter.OnlyFavorite)
          case _ => None
        }
        timelineFragmentHolder.foreach(_.markAllAsRead(filter))
    }
  }

  private def deletePlurk(plurkID: Long) {
    val progressDialogFragment = new ProgressDialogFragment(
      getString(R.string.activityMaidroidPlurkDeleting),
      getString(R.string.pleaseWait)
    )

    progressDialogFragment.show(getSupportFragmentManager.beginTransaction, "deleteProgress")

    val oldRequestedOrientation = activity.getRequestedOrientation
    activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED)

    val deleteFuture = future {
      val plurkAPI = PlurkAPIHelper.getPlurkAPI(this)
      plurkAPI.Timeline.plurkDelete(plurkID).get
    }

    deleteFuture.onSuccessInUI { _ =>
      timelineFragmentHolder.foreach { _.deletePlurk(plurkID) }
      onDeletePlurkSuccess()
      progressDialogFragment.dismiss()
      activity.setRequestedOrientation(oldRequestedOrientation)
    }

    deleteFuture.onFailureInUI { case e: Exception =>
      progressDialogFragment.dismiss()
      activity.setRequestedOrientation(oldRequestedOrientation)
      DebugLog("====> onDeletePlurkFailure....", e)
      dialogFrame.setMessages(
        Message(MaidMaro.Half.Normal, getString(R.string.activityMaidroidPlurkDeleteFailure01)) ::
        Message(MaidMaro.Half.Normal, getString(R.string.activityMaidroidPlurkDeleteFailure02).format(e.getMessage)) ::
        Message(MaidMaro.Half.Smile, getString(R.string.activityMaidroidPlurkDeleteFailure03)) :: 
        Nil
      )
    }
  }

  override def onCreateOptionsMenu(menu: Menu): Boolean = {
    val inflater = getMenuInflater
    inflater.inflate(R.menu.activity_maidroid_plurk, menu)
    super.onCreateOptionsMenu(menu)
  }

  override def onOptionsItemSelected(menuItem: MenuItem): Boolean = menuItem.getItemId match {
    case R.id.activityMaidroidPlurkActionAbout => AboutActivity.startActivity(this); false
    case _ => super.onOptionsItemSelected(menuItem)
  }

}
