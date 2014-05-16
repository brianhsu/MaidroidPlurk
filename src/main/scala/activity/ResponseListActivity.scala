package idv.brianhsu.maidroid.plurk.activity

import idv.brianhsu.maidroid.ui.model._
import idv.brianhsu.maidroid.plurk._
import idv.brianhsu.maidroid.plurk.dialog._
import idv.brianhsu.maidroid.plurk.view._
import idv.brianhsu.maidroid.plurk.util._
import idv.brianhsu.maidroid.plurk.fragment._
import idv.brianhsu.maidroid.ui.util.AsyncUI._

import org.bone.soplurk.api.PlurkAPI._
import org.bone.soplurk.constant.CommentSetting
import org.bone.soplurk.model._

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.content.DialogInterface
import android.content.pm.ActivityInfo

import android.view.View
import android.view.Menu
import android.view.MenuItem
import android.support.v7.app.ActionBarActivity
import scala.util.{Try, Success, Failure}

import scala.concurrent._

object ResponseListActivity {
  var plurk: Plurk = _
  var user: User = _

  val RequestPostResponse = 1
  val RequestEditPlurk = 2

}


class ResponseListActivity extends ActionBarActivity with TypedViewHolder 
                           with ResponseListFragment.Listener
                           with ConfirmDialog.Listener
                           with PlurkView.Listener
{

  private implicit def activity = this
  private var showWelcomeMessage = true
  private lazy val dialogFrame = findView(TR.activityResponseListDialogFrame)
  private lazy val fragmentContainer = findView(TR.activityResponseListFragmentContainer)
  private lazy val plurkAPI = PlurkAPIHelper.getPlurkAPI(this)

  private var responseListFragment: Option[ResponseListFragment] = None

  override def onCreate(savedInstanceState: Bundle) {

    super.onCreate(savedInstanceState)

    setContentView(R.layout.activity_response_list)
    dialogFrame.setMessages(
      Message(MaidMaro.Half.Happy, "小鈴正在幫主人讀取噗浪上的回應，請主人稍候一下喲……", None) :: Nil
    )

    responseListFragment match {
      case Some(fragment) =>
        fragment.plurk = ResponseListActivity.plurk
        fragment.owner = ResponseListActivity.user
      case None => updateFragment()
    }
  }

  private def updateFragment() {
    val fragment = new ResponseListFragment

    fragment.plurk = ResponseListActivity.plurk
    fragment.owner = ResponseListActivity.user

    this.responseListFragment = Some(fragment)

    getSupportFragmentManager.
      beginTransaction.
      replace(R.id.activityResponseListFragmentContainer, fragment).
      commit()

  }

  override def onStart() {
    super.onStart()
  }

  private def hasResponsePermission = {
    (ResponseListActivity.plurk.ownerID == ResponseListActivity.plurk.userID)
    (ResponseListActivity.plurk.whoIsCommentable == CommentSetting.Everyone) ||
    (ResponseListActivity.plurk.whoIsCommentable == CommentSetting.OnlyFriends)
  }

  override def onCreateOptionsMenu(menu: Menu): Boolean = {
    val inflater = getMenuInflater
    inflater.inflate(R.menu.activity_response_list, menu)
    super.onCreateOptionsMenu(menu)
  }

  override def onPrepareOptionsMenu(menu: Menu): Boolean = {
    val isPostedByCurrentUser = 
      ResponseListActivity.plurk.ownerID == ResponseListActivity.plurk.userID

    val replyButton = menu.findItem(R.id.activityResponseListActionReply)
    replyButton.setEnabled(hasResponsePermission)
    replyButton.setVisible(hasResponsePermission)

    val editButton = menu.findItem(R.id.activityResponseListActionEdit)
    val deleteButton = menu.findItem(R.id.activityResponseListActionDelete)

    editButton.setEnabled(isPostedByCurrentUser)
    editButton.setVisible(isPostedByCurrentUser)

    deleteButton.setEnabled(isPostedByCurrentUser)
    deleteButton.setVisible(isPostedByCurrentUser)

    super.onPrepareOptionsMenu(menu)
  }

  override def onOptionsItemSelected(menuItem: MenuItem): Boolean = menuItem.getItemId match {
    case R.id.activityResponseListActionReply => startReplyActivity() ; false
    case R.id.activityResponseListActionEdit => startEditActivity() ; false
    case R.id.activityResponseListActionDelete => showConfirmDeleteDialog() ; false
    case R.id.activityResponseListActionLogout => logout(); false
    case _ => super.onOptionsItemSelected(menuItem)
  }

  private def logout() {
    Logout.logout(this)
  }

  private def showConfirmDeleteDialog() {
    val dialog = ConfirmDialog.createDialog(
      this,
      'DeletePlurkConfirm, 
      "確定要刪除嗎",
      "確定要刪除這則噗浪？這個動作無法回復喲！",
      "刪除",
      "取消"
    ) 
    dialog.show(getSupportFragmentManager, "DeletePlurkConfirm")
  }

  override def onDialogOKClicked(dialogName: Symbol, dialog: DialogInterface, data: Bundle) {
    dialogName match {
      case 'LogoutConfirm => 
        dialog.dismiss()
        this.finish()
        Logout.doLogout(this)
      case 'ExitConfirm =>
        deletePlurk()
        dialog.dismiss()
      case 'DeletePlurkConfirm =>
        deletePlurk()
      case 'DeleteResponseConfirm =>
        val plurkID = data.getLong("plurkID", -1)
        val responseID = data.getLong("responseID", -1)
        deleteResponse(plurkID, responseID)
      
    }
  }

  private def deleteResponse(plurkID: Long, responseID: Long) {

    dialogFrame.setMessages(
      Message(MaidMaro.Half.Smile, "要刪除這則回應嗎？好的，小鈴知道了，請主人稍等一下喔！") :: Nil
    )

    val deleteFuture = future {
      plurkAPI.Responses.responseDelete(plurkID, responseID).get
    }

    deleteFuture.onSuccessInUI { _ =>
      responseListFragment.foreach(_.deleteResponse(responseID))
      dialogFrame.setMessages(
        Message(MaidMaro.Half.Happy, "小鈴已經順利幫主把這則回應刪除了喲！") :: Nil
      )
    }

    deleteFuture.onFailureInUI { case e: Exception =>
      DebugLog("====> onDeleteResponseFailure....", e)
      dialogFrame.setMessages(
        Message(MaidMaro.Half.Normal, "真是對不起，小鈴沒辦刪除這則回應耶……", None) ::
        Message(MaidMaro.Half.Normal, s"系統說錯誤是：「${e.getMessage}」造成的說。", None) ::
        Message(MaidMaro.Half.Smile, "主人要不要檢查網路狀態後重新讀取一次試試看呢？") :: Nil
      )
    }
  }

  private def deletePlurk() {

    dialogFrame.setMessages(
      Message(MaidMaro.Half.Smile, "要刪除這則噗浪嗎？好的，小鈴知道了，請主人稍等一下喔！") :: Nil
    )

    val progressDialogFragment = new ProgressDialogFragment("刪除中", "請稍候……")
    progressDialogFragment.show(
      getSupportFragmentManager.beginTransaction, 
      "deletePlurkProgress"
    )
    val oldRequestedOrientation = getRequestedOrientation
    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED)


    val deleteFuture = future {
      plurkAPI.Timeline.plurkDelete(ResponseListActivity.plurk.plurkID).get
    }

    deleteFuture.onSuccessInUI { _ =>
      TimelineFragment.deletedPlurkIDHolder = Some(ResponseListActivity.plurk.plurkID)
      progressDialogFragment.dismiss()
      setRequestedOrientation(oldRequestedOrientation)
      finish()
    }

    deleteFuture.onFailureInUI { case e: Exception =>
      DebugLog("====> deletePlurkFailure....", e)
      dialogFrame.setMessages(
        Message(MaidMaro.Half.Normal, "真是對不起，小鈴沒辦刪除這則噗浪耶……", None) ::
        Message(MaidMaro.Half.Normal, s"系統說錯誤是：「${e.getMessage}」造成的說。", None) ::
        Message(MaidMaro.Half.Smile, "主人要不要檢查網路狀態後重新讀取一次試試看呢？") :: Nil
      )
      progressDialogFragment.dismiss()
      setRequestedOrientation(oldRequestedOrientation)
    }
  }

  private def startReplyActivity() {
    val intent = new Intent(this, classOf[PostResponseActivity])
    intent.putExtra(PostResponseActivity.PlurkIDBundle, ResponseListActivity.plurk.plurkID)
    startActivityForResult(intent, ResponseListActivity.RequestPostResponse)
  }

  override def startEditActivity(plurk: Plurk) {}

  private def startEditActivity() {
    val intent = new Intent(this, classOf[EditPlurkActivity])
    intent.putExtra(EditPlurkActivity.PlurkIDBundle, ResponseListActivity.plurk.plurkID)
    intent.putExtra(
      EditPlurkActivity.ContentRawBundle, 
      ResponseListActivity.plurk.contentRaw getOrElse ""
    )

    startActivityForResult(intent, ResponseListActivity.RequestEditPlurk)
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

  override def onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {

    super.onActivityResult(requestCode, resultCode, data)

    requestCode match {
      case ResponseListActivity.RequestPostResponse if resultCode == Activity.RESULT_OK => 
        showWelcomeMessage = false
        updateFragment()
        dialogFrame.setMessages(
          Message(MaidMaro.Half.Happy, "已經幫主人把回應發到噗浪上去囉！", None) ::
          Message(MaidMaro.Half.Smile, "有了主人的參與，這個討論一定會更有趣的。", None) ::
          Nil
        )
      case ResponseListActivity.RequestEditPlurk if resultCode == Activity.RESULT_OK => 
        DebugLog("====> XXXXXXX")
        val plurkID = data.getLongExtra(EditPlurkActivity.PlurkIDBundle, -1)
        val newContent = data.getStringExtra(EditPlurkActivity.EditedContentBundle)
        val newContentRaw = Option(data.getStringExtra(EditPlurkActivity.EditedContentRawBundle))

        if (plurkID != -1) {
          PlurkView.updatePlurk(plurkID, newContent, newContentRaw)
          ResponseListActivity.plurk = ResponseListActivity.plurk.copy(
            content = newContent, contentRaw = newContentRaw
          )
          updateFragment()
        }

      case _ => 
    }
  }

}
