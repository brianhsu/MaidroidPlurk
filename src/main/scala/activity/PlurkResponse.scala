package idv.brianhsu.maidroid.plurk.activity

import idv.brianhsu.maidroid.ui.model._
import idv.brianhsu.maidroid.plurk._
import idv.brianhsu.maidroid.plurk.util.DebugLog
import idv.brianhsu.maidroid.plurk.fragment._

import org.bone.soplurk.api.PlurkAPI._
import org.bone.soplurk.model._

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.support.v7.app.ActionBarActivity
import scala.util.{Try, Success, Failure}

object PlurkResponse {
  var plurk: Plurk = _
  var user: User = _
}

class PlurkResponse extends ActionBarActivity with TypedViewHolder 
                    with ResponseList.Listener
{

  private lazy val dialogFrame = findView(TR.activityPlurkResponseDialogFrame)
  private lazy val fragmentContainer = findView(TR.activityPlurkResponseFragmentContainer)

  override def onCreate(savedInstanceState: Bundle) {

    super.onCreate(savedInstanceState)

    setContentView(R.layout.activity_plurk_response)
    dialogFrame.setMessages(
      Message(MaidMaro.Half.Happy, "小鈴正在幫主人讀取噗浪上的回應，請主人稍候一下喲……", None) :: Nil
    )

    val responseListFragment = Try(getSupportFragmentManager.findFragmentById(R.id.activityPlurkResponseFragmentContainer).asInstanceOf[ResponseList]).filter(_ != null)

    responseListFragment match {
      case Success(fragment) =>
        fragment.plurk = PlurkResponse.plurk
        fragment.owner = PlurkResponse.user
      case Failure(e) =>
        val fragment = new ResponseList

        fragment.plurk = PlurkResponse.plurk
        fragment.owner = PlurkResponse.user

        getSupportFragmentManager.
          beginTransaction.
          replace(R.id.activityPlurkResponseFragmentContainer, fragment).
          commit()
    }


  }

  override def onStart() {
    super.onStart()
  }

  override def onGetResponseSuccess(responses: PlurkResponses) {

    val dialog = responses.responses.size match {
      case 0 => 
        Message(MaidMaro.Half.Normal, "咦？這則噗浪好像沒有什麼人留言耶……", None) :: 
        Message(MaidMaro.Half.Normal, "主人要不要參與討論，讓這則噗浪熱鬧一些呢？", None) :: Nil
      case n if n <= 50 =>
        Message(MaidMaro.Half.Smile, "已經幫主人把大家的回應整理好囉！主人也對這個話題有興趣嗎？", None) :: 
        Message(MaidMaro.Half.Smile, "如果主人想要回應，請告訴小鈴，小鈴會幫忙主人發佈回應喲！", None) :: Nil
      case n =>
        Message(MaidMaro.Half.Happy, s"哇，這則噗浪好熱鬧，總共有 $n 個回應耶，有這麼多回應，發噗的人一定很高興吧？", None) :: 
        Message(MaidMaro.Half.Happy, "請主人也加油，讓自己的河道和這個噗浪一樣熱鬧喲！", None) :: Nil

    }

    dialogFrame.setMessages(dialog)
  }

  override def onGetResponseFailure(e: Exception) {
    DebugLog("====> onGetResponseFailure....")
    dialogFrame.setMessages(
      Message(MaidMaro.Half.Normal, "好像怪怪的，沒辦法讀噗浪上的回應耶……", None) ::
      Message(MaidMaro.Half.Normal, s"系統說錯誤是：「${e.getMessage}」造成的說。", None) ::
      Message(MaidMaro.Half.Smile, "主人要不要檢查網路狀態後重新讀取一次試試看呢？") :: Nil
    )
  }


}
