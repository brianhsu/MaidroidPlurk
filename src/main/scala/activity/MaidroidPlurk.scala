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
import scala.concurrent._

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity

trait FragmentFinder {
  this: FragmentActivity =>

  def findFragment[T <: Fragment](id: Int): Option[T] = { 
    Option(getSupportFragmentManager().findFragmentById(id).asInstanceOf[T])
  }
}

class MaidroidPlurk extends ActionBarActivity with TypedViewHolder with FragmentFinder 
                    with ErrorNotice.Listener with Login.Listener with TimeLine.Listener
{
  implicit val activity = this

  private lazy val dialogFrame = findView(TR.dialogFrame)
  private lazy val loadingIndicator = findView(TR.moduleLoadingIndicator)
  private lazy val errorNoticeFragment = findFragment[ErrorNotice](R.id.activityMaidroidPlurkErrorNotice).get
  private def loginFragmentHolder = findFragment[Login](R.id.activityMaidroidPlurkFragmentContainer)

  val onGetPlurkAPI = PlurkAPI.withCallback(
    appKey = "6T7KUTeSbwha", 
    appSecret = "AZIpUPdkTARzbDmdKBsu4kpxhHUJ3eWX", 
    callbackURL = "http://localhost/auth"
  )

  def onShowAuthorizationPage(url: String) {
    loadingIndicator.setVisibility(View.GONE)
    errorNoticeFragment.setVisibility(View.GONE)
  }

  def onGetAuthURLFailure(error: Exception) {
    DebugLog("====> onGetAuthURLFailure", error)
    errorNoticeFragment.setVisibility(View.VISIBLE)
    errorNoticeFragment.showMessage("無法取得噗浪登入網址", error)
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
    errorNoticeFragment.showMessage("無法正確登入噗浪", error)
    dialogFrame.setMessages(
      Message(MaidMaro.Half.Normal, "好像怪怪的，沒辦法正常登入噗浪耶……", None) :: 
      Message(MaidMaro.Half.Normal, "該不會是小鈴太沒用了，所以才一直出錯吧？", None) :: 
      Message(MaidMaro.Half.Normal, s"對了，系統說這個錯誤是：「${error.getMessage}」造成的說") :: Nil

    )


  }

  def onLoginSuccess() {
    dialogFrame.setMessages(
      Message(MaidMaro.Half.Happy, "成功登入噗浪了呢", None) :: Nil
    )
  }

  override def onStart() {
    super.onStart()
    loginFragmentHolder.foreach { _.startAuthorization() }
    errorNoticeFragment.setVisibility(View.GONE)
  }

  override def onCreate(savedInstanceState: Bundle) {

    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_maidroid_plurk)

    if (savedInstanceState == null) {
      setupLoginFragment()
    }

    dialogFrame.setMessages(
      Message(MaidMaro.Half.Smile, "お帰りなさいませ、ご主人様。歡迎使用 Maidroid Plurk，把對話框網左拉的話，就可以繼續對話喔！", None) :: 
      Message(MaidMaro.Half.Happy, "沒錯，就是這樣。我是女僕小鈴，很高興可以在這裡服務主人喲。", None) :: 
      Message(MaidMaro.Half.Normal, "看起來主人還沒登入噗浪呢，可以請主人先登入嗎？這樣小鈴才有辦法幫主人整理大家的聊天的說。", None) :: Nil
    )
  }

  override def onHideOtherUI() {
    loadingIndicator.setVisibility(View.GONE)
    loginFragmentHolder.foreach { _.setVisibility(View.GONE) }
  }

  def setupLoginFragment() {
    getSupportFragmentManager.
        beginTransaction.
        replace(R.id.activityMaidroidPlurkFragmentContainer, new Login).
        commit()
  }

}
