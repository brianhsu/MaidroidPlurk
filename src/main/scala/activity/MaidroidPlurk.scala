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
      Message(MaidMaro.Half.Panic, "咦咦咦咦？！怎麼會這樣，沒辦法找到噗浪的登入網頁耶……", None) :: 
      Message(MaidMaro.Half.Panic, "可不可以請主人檢查一下網路的狀態，看是不是網路不穩定或者忘了開網路呢？", None) :: 
      Message(MaidMaro.Half.Normal, "還是說，是小鈴太沒用了……", None) :: 
      Message(MaidMaro.Half.Normal, s"對了，系統說這個錯誤是：「${error.getMessage}」造成的說") :: Nil
    )
  }

  def onLoginFailure(error: Exception) {
    dialogFrame.setMessages(
      Message(MaidMaro.Half.Normal, "好像怪怪的，沒辦法正常登入噗浪耶……", None) :: 
      Message(MaidMaro.Half.Normal, "該不會是小鈴太沒用了，所以才一直出錯吧？", None) :: 
      Message(MaidMaro.Half.Normal, s"對了，系統說這個錯誤是：「${error.getMessage}」造成的說") :: Nil
    )
  }

  def onLoginSuccess() {
    switchToFragment(new TimelineFragment, isForcing = true)
    dialogFrame.setMessages(
      Message(MaidMaro.Half.Happy, "成功登入噗浪了呢！", None) :: 
      Message(MaidMaro.Half.Smile, "小鈴正在幫主人整理河道上的資料，請主人稍候一下喲。", None) ::
      Nil
    )

  }

  def onShowTimelinePlurksFailure(error: Exception) {
    if (error.getMessage.contains("invalid access token")) {
      switchToFragment(new LoginFragment, isForcing = true)
    } else {
      dialogFrame.setMessages(
        Message(MaidMaro.Half.Panic, "糟糕，讀不到河道上的資料啊！", None) :: 
        Message(MaidMaro.Half.Normal, "會不會是網路有問題呢？", None) ::
        Message(MaidMaro.Half.Normal, s"對了，系統說這個錯誤是：「${error.getMessage}」造成的說") :: Nil
      )
    }
  }

  def onShowTimelinePlurksSuccess(timeline: Timeline, isNewFilter: Boolean, 
                                  filter: Option[Filter], isOnlyUnread: Boolean) {

    val unreadText = if (isOnlyUnread) "未讀" else ""
    val filterText = filter match {
      case Some(OnlyUser) => s"我發表的${unreadText}訊息"
      case Some(OnlyPrivate) => s"私人的${unreadText}訊息"
      case Some(OnlyResponded) => s"回應過的${unreadText}訊息"
      case Some(OnlyFavorite) => s"說讚或轉噗的${unreadText}訊息"
      case _ => s"全部的${unreadText}訊息"
    }

    dialogFrame.setMessages(
      Message(MaidMaro.Half.Smile, s"順利幫主讀到噗浪上的資料了喲，現在列出的是「${filterText}」喲。", None) ::
      Message(MaidMaro.Half.Happy, "不知道最近有沒有什麼有趣的事發生呢？", None) :: 
      Message(MaidMaro.Half.Smile, "如果有好玩的事，記得要和小鈴分享一下喲！", None) :: Nil
    )
  }

  override def onCreate(savedInstanceState: Bundle) {

    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_maidroid_plurk)

    dialogFrame.setMessages(
      Message(MaidMaro.Half.Smile, "お帰りなさいませ、ご主人様。歡迎使用 Maidroid Plurk，把對話框網左拉的話，就可以繼續對話喔！", None) :: 
      Message(MaidMaro.Half.Happy, "沒錯，就是這樣。我是女僕小鈴，很高興可以在這裡服務主人喲。", None) :: 
      Message(MaidMaro.Half.Normal, "看起來主人還沒登入噗浪呢，可以請主人先登入嗎？這樣小鈴才有辦法幫主人整理大家的聊天的說。", None) :: Nil
    )

    if (PlurkAPIHelper.isLoggedIn(this)) {
      switchToFragment(new TimelineFragment)
    } else {
      switchToFragment(new LoginFragment, isForcing = true)
    }
  }

  override def onRefreshTimelineFailure(e: Exception) {
    dialogFrame.setMessages(
      Message(MaidMaro.Half.Normal, "奇怪，怎麼沒辦法從噗浪拿到新的資料呢……", None) :: 
      Message(MaidMaro.Half.Panic, s"哇啊啊，「${e}」是怎麼回事啊？", None) :: 
      Message(MaidMaro.Half.Normal, "可不可以請主人檢查網路狀態後再重試一次？") :: Nil
    )
  }

  override def onRefreshTimelineSuccess(newTimeline: Timeline) {

    if (newTimeline.plurks.size > 0) {
      dialogFrame.setMessages(
        Message(MaidMaro.Half.Happy, s"已經幫主人把河道上最新的噗抓下來囉！總共有 ${newTimeline.plurks.size} 則新的噗喲。", None) :: 
        Message(MaidMaro.Half.Smile, "不知道主人有沒有在河道上發現什麼新的趣事呢？") :: Nil
      )
    } else {
      dialogFrame.setMessages(
        Message(MaidMaro.Half.Normal, "對不起，現在噗浪的河道上好像沒有什麼人發新的文章呢……", None) :: 
        Message(MaidMaro.Half.Smile, "主人要不要晚一點再試試看？") :: Nil
      )
    }
  }

  override def onDeletePlurkSuccess() {
    dialogFrame.setMessages(
      Message(MaidMaro.Half.Happy, "小鈴已經順利幫主把這則噗浪刪除了喲！") :: Nil
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
    }
  }

  private def deletePlurk(plurkID: Long) {
    val progressDialogFragment = new ProgressDialogFragment("刪除中", "請稍候……")
    progressDialogFragment.show(getSupportFragmentManager.beginTransaction, "deleteProgress")
    val oldRequestedOrientation = activity.getRequestedOrientation
    activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED)

    dialogFrame.setMessages(
      Message(MaidMaro.Half.Smile, "要刪除這則發文嗎？好的，小鈴知道了，請主人稍等一下喔！") :: Nil
    )

    val deleteFuture = future {
      val plurkAPI = PlurkAPIHelper.getPlurkAPI(this)
      plurkAPI.Timeline.plurkDelete(plurkID).get
    }

    deleteFuture.onSuccessInUI { _ =>
      timelineFragmentHolder.foreach { _.deletePlurk(plurkID) }
      dialogFrame.setMessages(
        Message(MaidMaro.Half.Happy, "小鈴已經順利幫主把這則噗浪刪除了喲！") :: Nil
      )
      progressDialogFragment.dismiss()
      activity.setRequestedOrientation(oldRequestedOrientation)
    }

    deleteFuture.onFailureInUI { case e: Exception =>
      progressDialogFragment.dismiss()
      activity.setRequestedOrientation(oldRequestedOrientation)
      DebugLog("====> onDeletePlurkFailure....", e)
      dialogFrame.setMessages(
        Message(MaidMaro.Half.Normal, "真是對不起，小鈴沒辦刪除這則噗浪耶……", None) ::
        Message(MaidMaro.Half.Normal, s"系統說錯誤是：「${e.getMessage}」造成的說。", None) ::
        Message(MaidMaro.Half.Smile, "主人要不要檢查網路狀態後重新讀取一次試試看呢？") :: Nil
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
