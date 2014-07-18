package idv.brianhsu.maidroid.plurk.activity

import idv.brianhsu.maidroid.ui.model._
import idv.brianhsu.maidroid.plurk._
import idv.brianhsu.maidroid.plurk.dialog._
import idv.brianhsu.maidroid.plurk.view._
import idv.brianhsu.maidroid.plurk.util._
import idv.brianhsu.maidroid.plurk.fragment._
import idv.brianhsu.maidroid.plurk.view._
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
  private lazy val dialogFrame = ToggleView.setupAngryBehavior(this, findView(TR.activityResponseListDialogFrame))
  private lazy val fragmentContainer = findView(TR.activityResponseListFragmentContainer)
  private lazy val plurkAPI = PlurkAPIHelper.getPlurkAPI(this)

  private var responseListFragment: Option[ResponseListFragment] = None

  override def onReplyTo(username: String) {
    startReplyActivity(Some(username))
  }

  override def onCreate(savedInstanceState: Bundle) {

    super.onCreate(savedInstanceState)

    setContentView(R.layout.activity_response_list)
    dialogFrame.setMessages(
      Message(MaidMaro.Half.Happy, getString(R.string.activityResponseListWelcome01)) :: Nil
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
    case R.id.activityResponseListActionAbout => AboutActivity.startActivity(this); false
    case R.id.activityResponseListActionToggleMaid => ToggleView(dialogFrame); false
    case _ => super.onOptionsItemSelected(menuItem)
  }

  private def logout() {
    Logout.logout(this)
  }

  private def showConfirmDeleteDialog() {
    val dialog = ConfirmDialog.createDialog(
      this,
      'DeletePlurkConfirm, 
      getString(R.string.activityResponseListConfirmDeleteTitle),
      getString(R.string.activityResponseListConfirmDelete),
      getString(R.string.delete), getString(R.string.cancel)
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

    val oldRequestedOrientation = getRequestedOrientation
    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED)

    val progressDialogFragment = ProgressDialogFragment.createDialog(
      getString(R.string.activityResponseListDeleteing), 
      getString(R.string.pleaseWait)
    )

    progressDialogFragment.show(
      getSupportFragmentManager.beginTransaction, 
      "deleteResponseProgress"
    )

    val deleteFuture = future {
      plurkAPI.Responses.responseDelete(plurkID, responseID).get
    }

    deleteFuture.onSuccessInUI { _ =>
      responseListFragment.foreach(_.deleteResponse(responseID))

      progressDialogFragment.dismiss()
      setRequestedOrientation(oldRequestedOrientation)

      dialogFrame.setMessages(
        Message(MaidMaro.Half.Happy, getString(R.string.activityResponseListDeleteResponseOK)) :: 
        Nil
      )
    }

    deleteFuture.onFailureInUI { case e: Exception =>
      progressDialogFragment.dismiss()
      setRequestedOrientation(oldRequestedOrientation)

      DebugLog("====> onDeleteResponseFailure....", e)
      dialogFrame.setMessages(
        Message(MaidMaro.Half.Normal, getString(R.string.activityResponseListDeleteResponseFailure01)) ::
        Message(MaidMaro.Half.Normal, getString(R.string.activityResponseListDeleteResponseFailure02).format(e.getMessage)) ::
        Message(MaidMaro.Half.Smile, getString(R.string.activityResponseListDeleteResponseFailure03)) :: 
        Nil
      )
    }
  }

  private def deletePlurk() {

    val oldRequestedOrientation = getRequestedOrientation
    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED)

    val progressDialogFragment = ProgressDialogFragment.createDialog(
      getString(R.string.activityResponseListDeleteing),
      getString(R.string.pleaseWait)
    )

    progressDialogFragment.show(
      getSupportFragmentManager.beginTransaction, 
      "deletePlurkProgress"
    )

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
        Message(MaidMaro.Half.Normal, getString(R.string.activityResponseListDeletePlurkFailure01)) ::
        Message(MaidMaro.Half.Normal, getString(R.string.activityResponseListDeletePlurkFailure02).format(e.getMessage)) ::
        Message(MaidMaro.Half.Smile, getString(R.string.activityResponseListDeletePlurkFailure03)) :: Nil
      )
      progressDialogFragment.dismiss()
      setRequestedOrientation(oldRequestedOrientation)
    }
  }

  private def startReplyActivity(nicknameHolder: Option[String] = None) {
    val intent = new Intent(this, classOf[PostResponseActivity])
    intent.putExtra(PostResponseActivity.PlurkIDBundle, ResponseListActivity.plurk.plurkID)
    nicknameHolder.foreach { nickname =>
      intent.putExtra(PostResponseActivity.NicknameBundle, nickname)
    }
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
        Message(MaidMaro.Half.Normal, getString(R.string.activityResponseListLow01)) :: 
        Message(MaidMaro.Half.Normal, getString(R.string.activityResponseListLow02)) :: 
        Nil
      case n if n <= 50 =>
        Message(MaidMaro.Half.Smile, getString(R.string.activityResponseListMid01)) :: 
        Message(MaidMaro.Half.Smile, getString(R.string.activityResponseListMid02)) :: 
        Nil
      case n =>
        Message(MaidMaro.Half.Happy, getString(R.string.activityResponseListHigh01).format(n)) :: 
        Message(MaidMaro.Half.Happy, getString(R.string.activityResponseListHigh02)) ::
        Nil
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
      Message(MaidMaro.Half.Normal, getString(R.string.activityResponseListGetResponseFailure01)) ::
      Message(MaidMaro.Half.Normal, getString(R.string.activityResponseListGetResponseFailure02).format(e.getMessage)) ::
      Message(MaidMaro.Half.Smile, getString(R.string.activityResponseListGetResponseFailure03)) :: 
      Nil
    )
  }

  override def onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {

    super.onActivityResult(requestCode, resultCode, data)

    requestCode match {
      case ResponseListActivity.RequestPostResponse if resultCode == Activity.RESULT_OK => 
        showWelcomeMessage = false
        updateFragment()
        dialogFrame.setMessages(
          Message(MaidMaro.Half.Happy, getString(R.string.activityResponseListPosted01)) ::
          Message(MaidMaro.Half.Smile, getString(R.string.activityResponseListPosted02)) ::
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
