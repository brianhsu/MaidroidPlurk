package idv.brianhsu.maidroid.plurk.activity

import idv.brianhsu.maidroid.ui.model._
import idv.brianhsu.maidroid.plurk._
import idv.brianhsu.maidroid.plurk.util.DebugLog
import idv.brianhsu.maidroid.plurk.fragment._

import org.bone.soplurk.api.PlurkAPI._
import org.bone.soplurk.constant.CommentSetting
import org.bone.soplurk.model._

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.view.View
import android.view.Menu
import android.view.MenuItem
import android.support.v7.app.ActionBarActivity
import scala.util.{Try, Success, Failure}

object PlurkResponse {
  var plurk: Plurk = _
  var user: User = _

  val RequestPostResponse = 1
}

class PlurkResponse extends ActionBarActivity with TypedViewHolder 
                    with ResponseList.Listener
{

  private var showWelcomeMessage = true
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
      case Failure(e) => updateFragment()
    }


  }

  private def updateFragment() {
    val fragment = new ResponseList

    fragment.plurk = PlurkResponse.plurk
    fragment.owner = PlurkResponse.user

    getSupportFragmentManager.
      beginTransaction.
      replace(R.id.activityPlurkResponseFragmentContainer, fragment).
      commit()

  }

  override def onStart() {
    super.onStart()
  }

  private def hasResponsePermission = {
    (PlurkResponse.plurk.ownerID == PlurkResponse.plurk.userID)
    (PlurkResponse.plurk.whoIsCommentable == CommentSetting.Everyone) ||
    (PlurkResponse.plurk.whoIsCommentable == CommentSetting.OnlyFriends)
  }

  override def onCreateOptionsMenu(menu: Menu): Boolean = {
    val inflater = getMenuInflater
    inflater.inflate(R.menu.response, menu)
    super.onCreateOptionsMenu(menu)
  }

  override def onPrepareOptionsMenu(menu: Menu): Boolean = {
    val replyButton = menu.findItem(R.id.responseActionReply)
    replyButton.setEnabled(hasResponsePermission)
    replyButton.setVisible(hasResponsePermission)
    super.onPrepareOptionsMenu(menu)
  }

  override def onOptionsItemSelected(menuItem: MenuItem): Boolean = menuItem.getItemId match {
    case R.id.responseActionReply => startReplyActivity() ; false
    case _ => super.onOptionsItemSelected(menuItem)
  }

  private def startReplyActivity() {
    val intent = new Intent(this, classOf[PostResponseActivity])
    intent.putExtra(PostResponseActivity.PlurkIDBundle, PlurkResponse.plurk.plurkID)
    startActivityForResult(intent, PlurkResponse.RequestPostResponse)
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

    if (showWelcomeMessage) {
      dialogFrame.setMessages(dialog)
    } else {
      showWelcomeMessage = true
    }
  }

  override def onGetResponseFailure(e: Exception) {
    DebugLog("====> onGetResponseFailure....", e)
    dialogFrame.setMessages(
      Message(MaidMaro.Half.Normal, "好像怪怪的，沒辦法讀噗浪上的回應耶……", None) ::
      Message(MaidMaro.Half.Normal, s"系統說錯誤是：「${e.getMessage}」造成的說。", None) ::
      Message(MaidMaro.Half.Smile, "主人要不要檢查網路狀態後重新讀取一次試試看呢？") :: Nil
    )
  }

  override def onDeleteResponseFailure(e: Exception) {
    DebugLog("====> onDeleteResponseFailure....", e)
    dialogFrame.setMessages(
      Message(MaidMaro.Half.Normal, "真是對不起，小鈴沒辦刪除這則回應耶……", None) ::
      Message(MaidMaro.Half.Normal, s"系統說錯誤是：「${e.getMessage}」造成的說。", None) ::
      Message(MaidMaro.Half.Smile, "主人要不要檢查網路狀態後重新讀取一次試試看呢？") :: Nil
    )
  }

  override def onDeleteResponseSuccess() {
    dialogFrame.setMessages(
      Message(MaidMaro.Half.Smile, "小鈴已經順利幫主把這則回應刪除了喲！") :: Nil
    )
  }

  override def onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {

    super.onActivityResult(requestCode, resultCode, data)

    requestCode match {
      case PlurkResponse.RequestPostResponse => 
        showWelcomeMessage = false
        updateFragment()
        dialogFrame.setMessages(
          Message(MaidMaro.Half.Happy, "已經幫主人把回應發到噗浪上去囉！", None) ::
          Message(MaidMaro.Half.Smile, "有了主人的參與，這個討論一定會更有趣的。", None) ::
          Nil
        )
      case _ => 
    }
  }

}
