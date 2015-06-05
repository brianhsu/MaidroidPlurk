package idv.brianhsu.maidroid.plurk.activity

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.ActionBarActivity
import android.view.Menu
import android.view.MenuItem
import idv.brianhsu.maidroid.plurk._
import idv.brianhsu.maidroid.plurk.adapter._
import idv.brianhsu.maidroid.plurk.dialog._
import idv.brianhsu.maidroid.plurk.fragment._
import idv.brianhsu.maidroid.plurk.TypedResource._
import idv.brianhsu.maidroid.plurk.util.Logout
import idv.brianhsu.maidroid.plurk.view.ToggleView
import idv.brianhsu.maidroid.plurk.view.PlurkView
import idv.brianhsu.maidroid.ui.model._
import idv.brianhsu.maidroid.ui.util.CallbackConversions._
import org.bone.soplurk.model.User
import org.bone.soplurk.model.Plurk
import org.bone.soplurk.api.PlurkAPI.Timeline


object UserTimelineActivity {

  val ExtraUserID = "ExtraUserID"
  val ExtraUserDisplayName = "ExtraDisplayName"
  val ExtraUserNickname = "ExtraUserNickname"

  def startActivity(context: Context, user: User) {
    val intent = new Intent(context, classOf[UserTimelineActivity])
    intent.putExtra(ExtraUserID, user.id)
    intent.putExtra(ExtraUserNickname, user.nickname)
    user.displayName.foreach(intent.putExtra(ExtraUserDisplayName, _))
    context.startActivity(intent)
  }

  trait Listener {
    def setTimelineWelecomeMessage(): Unit
  }
}

class UserTimelineActivity extends ActionBarActivity 
                           with TypedViewHolder 
                           with ConfirmDialog.Listener 
                           with UserTimelineActivity.Listener
                           with PlurkView.Listener
                           with UserTimelineFragment.Listener
                           with UserProfileFragment.Listener
{
  import UserTimelineActivity._

  private lazy val userID = getIntent.getLongExtra(ExtraUserID, -1)
  private lazy val dialogFrame = ToggleView.setupAngryBehavior(this, findView(TR.activityUserTimelineDialogFrame))
  private lazy val viewPager = findView(TR.activityUserTimelineViewPager)
  private lazy val pagerIndicator = findView(TR.activityUserTimelinePagerIndicator)
  private lazy val pageAdapter = new UserTimelineAdapter(this, getSupportFragmentManager, userID)
  private lazy val titleName = {
    val intent = getIntent
    val nickname = Option(intent.getStringExtra(ExtraUserNickname)).filterNot(_.trim.isEmpty)
    val displayName = Option(intent.getStringExtra(ExtraUserDisplayName)).filterNot(_.trim.isEmpty)
    val titleName = (displayName orElse nickname).getOrElse(userID)
    titleName
  }

  override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)

    val activityTitle = this.getString(R.string.titleUserTimeline).format(titleName)

    setContentView(R.layout.activity_user_timeline)
    setTitle(activityTitle)
    setTimelineWelecomeMessage()
    viewPager.setAdapter(pageAdapter)
    pagerIndicator.setViewPager(viewPager)

  }

  override def onCreateOptionsMenu(menu: Menu): Boolean = {
    val inflater = getMenuInflater
    inflater.inflate(R.menu.activity_user_timeline, menu)
    super.onCreateOptionsMenu(menu)
  }

  override def onOptionsItemSelected(menuItem: MenuItem): Boolean = menuItem.getItemId match {
    case R.id.activityUserTimelineActionAbout => AboutActivity.startActivity(this); false
    case R.id.activityUserTimelineActionLogout => Logout.logout(this); false
    case R.id.activityUserTimelineActionToggleMaid => ToggleView(this, dialogFrame) ; false
    case _ => super.onOptionsItemSelected(menuItem)
  }

  override def onResume() {
    super.onResume()
    ToggleView.syncDialogVisibility(this, dialogFrame)
  }

  override def onDialogOKClicked(dialogName: Symbol, dialog: DialogInterface, data: Bundle) {
    dialogName match {
      case 'LogoutConfirm =>
        dialog.dismiss()
        this.finish()
        Logout.doLogout(this)
    }
  }

  override def startEditActivity(plurk: Plurk) {}

  def setTimelineWelecomeMessage() = {
    dialogFrame.setMessages(
      Message(MaidMaro.Half.Smile, getString(R.string.activityUserTimelineWelcome01).format(titleName)) ::
      Message(MaidMaro.Half.Normal, getString(R.string.activityUserTimelineWelcome02)) ::
      Nil
    )
  }

  def onShowTimelinePlurksFailure(e: Exception): Unit = {
    dialogFrame.setMessages(
      Message(MaidMaro.Half.Normal, getString(R.string.activityUserTimelineFailure01).format(titleName)) ::
      Message(MaidMaro.Half.Panic, getString(R.string.activityUserTimelineFailure02)) ::
      Message(MaidMaro.Half.Normal, getString(R.string.activityUserTimelineFailure03).format(e.getMessage)) ::
      Nil
    )
  }

  def onShowTimelinePlurksSuccess(timeline: Timeline): Unit = {
    dialogFrame.setMessages(
      Message(MaidMaro.Half.Smile, getString(R.string.activityUserTimelineFetchOK01).format(titleName)) ::
      Message(MaidMaro.Half.Smile, getString(R.string.activityUserTimelineFetchOK02).format(titleName)) ::
      Nil
    )
  }

  def onShowTimelinePlurksPrivate(displayName: String): Unit = {
    dialogFrame.setMessages(
      Message(MaidMaro.Half.Panic, getString(R.string.activityUserTimelineFetchPrivate01).format(titleName)) ::
      Message(MaidMaro.Half.Smile, getString(R.string.activityUserTimelineFetchPrivate02)) ::
      Nil
    )

  }

  def onPostPrivateMessageOK(): Unit = {
    dialogFrame.setMessages(
      Message(MaidMaro.Half.Happy, getString(R.string.activityUserTimelineSendPrivateMessageOK01)) ::
      Message(MaidMaro.Half.Normal, getString(R.string.activityUserTimelineSendPrivateMessageOK02)) ::
      Nil
    )
  }

  def onPostPrivateMessageToNotFriend(): Unit = {
    dialogFrame.setMessages(
      Message(MaidMaro.Half.Normal, getString(R.string.activityUserTimelineSendPMToNotFriend)) ::
      Nil
    )
  }


}
