package idv.brianhsu.maidroid.plurk.activity

import idv.brianhsu.maidroid.ui.model._
import idv.brianhsu.maidroid.plurk._
import idv.brianhsu.maidroid.plurk.dialog._
import idv.brianhsu.maidroid.plurk.util._
import idv.brianhsu.maidroid.plurk.fragment._
import idv.brianhsu.maidroid.plurk.view._
import idv.brianhsu.maidroid.ui.util.AsyncUI._

import org.bone.soplurk.api.PlurkAPI._
import org.bone.soplurk.model._

import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.DialogInterface

import android.os.Bundle
import android.content.Intent
import android.view.View
import android.view.Menu
import android.view.MenuItem
import android.support.v7.app.ActionBarActivity
import android.widget.Toast

object PostResponseActivity {
  val PlurkIDBundle = "idv.brianhsu.maidroid.plurk.PostResponseActivity.plurkID"
  val NicknameBundle = "idv.brianhsu.maidroid.plurk.PostResponseActivity.nickname"
  val OriginContentBundle = "idv.brianhsu.maidroid.plurk.PostResponseActivity.originContent"

}

class PostResponseActivity extends ActionBarActivity 
                           with SelectImageActivity 
                           with SelectEmoticonActivity
                           with EmoticonFragment.Listener
                           with TypedViewHolder 
                           with ConfirmDialog.Listener
{
  protected val emoticonFragmentHolderResID = R.id.activityPostResponseEmtoicon
  protected lazy val editorFragment = new PostResponseFragment
  protected lazy val dialogFrame = ToggleView.setupAngryBehavior(this, findView(TR.activityPostResponseDialogFrame))
  protected lazy val plurkAPI = PlurkAPIHelper.getPlurkAPI(this)
  private lazy val plurkID = getIntent.getLongExtra(PostResponseActivity.PlurkIDBundle, -1)

  def getCurrentEditor = editorFragment

  override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_post_response)

    val isFragmentHolderEmpty = 
      getSupportFragmentManager.
        findFragmentById(R.id.activityPostResponseFragmentContainer) == null

    if (isFragmentHolderEmpty) {
      getSupportFragmentManager.
        beginTransaction.
        replace(R.id.activityPostResponseFragmentContainer, editorFragment).
        commit()
    }

    if (savedInstanceState != null) {
      val isEmoticonSelectorShown = 
        savedInstanceState.getBoolean(SelectEmoticonActivity.IsEmoticonSelectorShown, false)

      setSelectorVisibility(isEmoticonSelectorShown)
    }

    val originContentHolder = Option(getIntent.getStringExtra(PostResponseActivity.OriginContentBundle))
    val messages = originContentHolder match {
      case None => 
        Message(MaidMaro.Half.Smile, getString(R.string.activityPostResponseWelcome01)) :: 
        Nil
      case Some(originContent) =>
        Message(MaidMaro.Half.Smile, getString(R.string.activityPostResponseWelcome01)) :: 
        Message(MaidMaro.Half.Normal, getString(R.string.activityPostResponseWelcome02).format(originContent)):: 
        Nil
    }

    dialogFrame.setMessages(messages)
  }

  override def onCreateOptionsMenu(menu: Menu): Boolean = {
    val inflater = getMenuInflater
    inflater.inflate(R.menu.activity_post_response, menu)
    super.onCreateOptionsMenu(menu)
  }

  override def onOptionsItemSelected(menuItem: MenuItem): Boolean = menuItem.getItemId match {
    case R.id.activityPostResponseActionPhotoFromGallery => startPhotoPicker(); false
    case R.id.activityPostResponseActionPhotoFromCamera => startCamera(); false
    case R.id.activityPostResponseActionEmoticon => toggleEmoticonSelector(); false
    case R.id.activityPostResponseActionSend => postResponse(); false
    case R.id.activityPostResponseActionLogout => Logout.logout(this); false
    case R.id.activityPostResponseActionAbout => AboutActivity.startActivity(this); false
    case R.id.activityPostResponseActionToggleMaid => ToggleView(this, dialogFrame); false
    case _ => super.onOptionsItemSelected(menuItem)
  }


  override def onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {

    super.onActivityResult(requestCode, resultCode, data)

    requestCode match {
      case SelectImageActivity.RequestPhotoPicker => processImage(resultCode, data)
      case _ => 
    }
  }

  private def showWarningDialog() {

    val alertDialog = ConfirmDialog.createDialog(
      this, 'ExitConfirm, 
      getString(R.string.cancel),
      getString(R.string.activityPostResponseAbortConfirm),
      getString(R.string.yes), getString(R.string.no)
    ) 
    
    alertDialog.show(getSupportFragmentManager, "ExitConfirm")
  }

  override def onDialogOKClicked(dialogName: Symbol, dialog: DialogInterface, data: Bundle) {
    dialogName match {
      case 'LogoutConfirm => 
        dialog.dismiss()
        this.finish()
        Logout.doLogout(this)
      case 'ExitConfirm =>
        setResult(Activity.RESULT_CANCELED)
        dialog.dismiss()
        finish()
    }
  }

  override def onBackPressed() {

    val isConsumed = onBackPressedInEmoticonSelector()

    if (!isConsumed) {
      showWarningDialog()
    }
  }

  override def onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putBoolean(SelectEmoticonActivity.IsEmoticonSelectorShown, isSelectorShown)
  }

  private def postResponse() {
    val contentLength = editorFragment.getContentLength

    if (contentLength == 0) {
      dialogFrame.setMessages(
        Message(MaidMaro.Half.Normal, getString(R.string.utilPlurkEditorEmptyNoticeMessage01)) ::
        Message(MaidMaro.Half.Happy, getString(R.string.utilPlurkEditorEmptyNoticeMessage02)) ::
        Nil
      )
    } else if (contentLength > 210) {
      dialogFrame.setMessages(
        Message(MaidMaro.Half.Normal, getString(R.string.utilPlurkEditorOver210Message1)) ::
        Message(MaidMaro.Half.Normal, getString(R.string.utilPlurkEditorOver210Message2)) ::
        Nil
      )
    } else if (plurkID != -1) {
      val oldRequestedOrientation = getRequestedOrientation
      setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED)

      val progressDialogFragment = ProgressDialogFragment.createDialog(
        getString(R.string.activityPostResponsePosting),
        getString(R.string.pleaseWait)
      )
      progressDialogFragment.show(getSupportFragmentManager.beginTransaction, "respondProgress")

      val responseFuture = editorFragment.postResponse(plurkID)
      responseFuture.onSuccessInUI { _ =>
        setResult(Activity.RESULT_OK)
        progressDialogFragment.dismiss()
        setRequestedOrientation(oldRequestedOrientation)
        Toast.makeText(
          this, 
          getString(R.string.activityPostResponsePosted), 
          Toast.LENGTH_LONG
        ).show()
        this.finish()
      }

      responseFuture.onFailureInUI { case e: Exception =>
        progressDialogFragment.dismiss()
        setRequestedOrientation(oldRequestedOrientation)
        if (e.getMessage.contains("No permissions")) {
          dialogFrame.setMessages(
            Message(MaidMaro.Half.Normal, getString(R.string.activityPostResponseOnlyFriends01)) ::
            Message(MaidMaro.Half.Normal, getString(R.string.activityPostResponseOnlyFriends02)) :: 
            Nil
          )
        } else {
          dialogFrame.setMessages(
            Message(MaidMaro.Half.Panic, getString(R.string.activityPostResponseFailure01)) :: 
            Message(MaidMaro.Half.Normal, getString(R.string.activityPostResponseFailure02).format(e.getMessage)) ::
            Nil
          )
        }
      }
    }
  }


  override def onResume() {
    super.onResume()
    ToggleView.syncDialogVisibility(this, dialogFrame)
  }

}
