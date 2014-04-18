package idv.brianhsu.maidroid.plurk.activity

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.support.v7.app.ActionBarActivity

import idv.brianhsu.maidroid.plurk._
import idv.brianhsu.maidroid.plurk.fragment._
import idv.brianhsu.maidroid.plurk.util._
import idv.brianhsu.maidroid.ui.model._
import idv.brianhsu.maidroid.ui.util.AsyncUI._

import org.bone.soplurk.api._
import org.bone.soplurk.api.PlurkAPI.Timeline

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity

import scala.concurrent._
import scala.util.Try

class MaidroidPlurk extends ActionBarActivity with TypedViewHolder
                    with ErrorNotice.Listener with Login.Listener with TimelinePlurksFragment.Listener
{
  implicit val activity = this

  private lazy val dialogFrame = findView(TR.dialogFrame)
  private lazy val loadingIndicator = findView(TR.moduleLoadingIndicator)
  private lazy val errorNoticeFragment = getSupportFragmentManager().findFragmentById(R.id.activityMaidroidPlurkErrorNotice).asInstanceOf[ErrorNotice]
  private lazy val fragmentLogin = new Login
  private lazy val fragmentTimelinePlurks = new TimelinePlurksFragment

  val onGetPlurkAPI = PlurkAPI.withCallback(
    appKey = "6T7KUTeSbwha", 
    appSecret = "AZIpUPdkTARzbDmdKBsu4kpxhHUJ3eWX", 
    callbackURL = "http://localhost/auth"
  )

  def onHideLoadingUI() {
    loadingIndicator.setVisibility(View.GONE)
  }

  def onShowAuthorizationPage(url: String) {
    loadingIndicator.setVisibility(View.GONE)
    errorNoticeFragment.setVisibility(View.GONE)
  }

  def onGetAuthURLFailure(error: Exception) {
    DebugLog("====> onGetAuthURLFailure", error)
    errorNoticeFragment.setVisibility(View.VISIBLE)
    errorNoticeFragment.showMessageWithRetry("無法取得噗浪登入網址", error) { 
      DebugLog("====> Retry in onGetAuthURLFailure")
      retryLogin()
    }

    dialogFrame.setMessages(
      Message(MaidMaro.Half.Panic, "咦咦咦咦？！怎麼會這樣，沒辦法找到噗浪的登入網頁耶……", None) :: 
      Message(MaidMaro.Half.Panic, "可不可以請主人檢查一下網路的狀態，看是不是網路不穩定或者忘了開網路呢？", None) :: 
      Message(MaidMaro.Half.Normal, "還是說，是小鈴太沒用了……", None) :: 
      Message(MaidMaro.Half.Normal, s"對了，系統說這個錯誤是：「${error.getMessage}」造成的說") :: Nil
    )

  }

  def onLoginFailure(error: Exception) {
    DebugLog("====> onLoginFailure", error)
    errorNoticeFragment.setVisibility(View.VISIBLE)
    errorNoticeFragment.showMessageWithRetry("無法正確登入噗浪", error) {
      DebugLog("====> Retry in onLoginFailure")
      retryLogin()
    }

    dialogFrame.setMessages(
      Message(MaidMaro.Half.Normal, "好像怪怪的，沒辦法正常登入噗浪耶……", None) :: 
      Message(MaidMaro.Half.Normal, "該不會是小鈴太沒用了，所以才一直出錯吧？", None) :: 
      Message(MaidMaro.Half.Normal, s"對了，系統說這個錯誤是：「${error.getMessage}」造成的說") :: Nil

    )
  }

  def onLoginSuccess() {
    errorNoticeFragment.setVisibility(View.GONE)
    loadingIndicator.setVisibility(View.VISIBLE)
    switchToFragment(fragmentTimelinePlurks)
    dialogFrame.setMessages(
      Message(MaidMaro.Half.Happy, "成功登入噗浪了呢！", None) :: 
      Message(MaidMaro.Half.Smile, "小鈴正在幫主人整理河道上的資料，請主人稍候一下喲。", None) ::
      Nil
    )

  }

  def onShowTimelinePlurksFailure(error: Exception) {
    DebugLog("====> onShowTimelinePlurksFailure", error)
    errorNoticeFragment.setVisibility(View.VISIBLE)
    errorNoticeFragment.showMessageWithRetry("無法讀取河道", error) {
      loadingIndicator.setVisibility(View.VISIBLE)
      errorNoticeFragment.setVisibility(View.GONE)
      fragmentTimelinePlurks.updateTimeline()
    }

    dialogFrame.setMessages(
      Message(MaidMaro.Half.Panic, "糟糕，讀不到河道上的資料啊！", None) :: 
      Message(MaidMaro.Half.Normal, "會不會是網路有問題呢？", None) ::
      Message(MaidMaro.Half.Normal, s"對了，系統說這個錯誤是：「${error.getMessage}」造成的說") :: Nil
    )
  }

  def onShowTimelinePlurksSuccess(timeline: Timeline) {
    dialogFrame.setMessages(
      Message(MaidMaro.Half.Happy, "好像順利讀到噗浪上的資料了喲，不知道最近有沒有什麼有趣的事發生呢？", None) :: 
      Message(MaidMaro.Half.Smile, "如果有好玩的事，記得要和小鈴分享一下喲！", None) :: Nil
    )
  }

  override def onStart() {
    super.onStart()
    fragmentLogin.startAuthorization()
    errorNoticeFragment.setVisibility(View.GONE)
  }

  override def onCreate(savedInstanceState: Bundle) {

    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_maidroid_plurk)

    if (savedInstanceState == null) {
      switchToFragment(fragmentLogin)
    }

    dialogFrame.setMessages(
      Message(MaidMaro.Half.Smile, "お帰りなさいませ、ご主人様。歡迎使用 Maidroid Plurk，把對話框網左拉的話，就可以繼續對話喔！", None) :: 
      Message(MaidMaro.Half.Happy, "沒錯，就是這樣。我是女僕小鈴，很高興可以在這裡服務主人喲。", None) :: 
      Message(MaidMaro.Half.Normal, "看起來主人還沒登入噗浪呢，可以請主人先登入嗎？這樣小鈴才有辦法幫主人整理大家的聊天的說。", None) :: Nil
    )
  }

  override def onHideOtherUI() {
    loadingIndicator.setVisibility(View.GONE)
    fragmentLogin.setVisibility(View.GONE)
  }

  private def switchToFragment(fragment: Fragment) {
    loadingIndicator.setVisibility(View.VISIBLE)
    getSupportFragmentManager.
        beginTransaction.
        replace(R.id.activityMaidroidPlurkFragmentContainer, fragment).
        commit()
  }

  private def retryLogin() {
    loadingIndicator.setVisibility(View.VISIBLE)
    errorNoticeFragment.setVisibility(View.GONE)
    fragmentLogin.startAuthorization()
  }



}
