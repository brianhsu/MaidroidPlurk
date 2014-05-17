package idv.brianhsu.maidroid.plurk.activity

import idv.brianhsu.maidroid.ui.model._
import idv.brianhsu.maidroid.plurk._
import idv.brianhsu.maidroid.plurk.dialog._
import idv.brianhsu.maidroid.plurk.util._
import idv.brianhsu.maidroid.plurk.fragment._
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

import scala.concurrent._

object EditPlurkActivity {
  val PlurkIDBundle = "idv.brianhsu.maidroid.plurk.EditPlurkActivity.plurkID"
  val ContentRawBundle = "idv.brianhsu.maidroid.plurk.EditPlurkActivity.contentRaw"
  val EditedContentBundle = "idv.brianhsu.maidroid.plurk.EditPlurkActivity.editedContent"
  val EditedContentRawBundle = "idv.brianhsu.maidroid.plurk.EditPlurkActivity.editedContentRaw"
}

class EditPlurkActivity extends ActionBarActivity 
                           with SelectImageActivity 
                           with SelectEmoticonActivity
                           with EmoticonFragment.Listener
                           with TypedViewHolder 
                           with ConfirmDialog.Listener
{
 
  private lazy val plurkID = getIntent.getLongExtra(EditPlurkActivity.PlurkIDBundle, -1)
  private lazy val rawContent = getIntent.getStringExtra(EditPlurkActivity.ContentRawBundle)

  protected val emoticonFragmentHolderResID = R.id.activityEditPlurkEmtoicon
  protected lazy val editorFragment = new EditPlurkFragment(rawContent)
  protected lazy val dialogFrame = findView(TR.activityEditPlurkDialogFrame)
  protected lazy val plurkAPI = PlurkAPIHelper.getPlurkAPI(this)

  def getCurrentEditor = editorFragment

  override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_edit_plurk)

    val isFragmentHolderEmpty = 
      getSupportFragmentManager.
        findFragmentById(R.id.activityEditPlurkFragmentContainer) == null

    if (isFragmentHolderEmpty) {
      getSupportFragmentManager.
        beginTransaction.
        replace(R.id.activityEditPlurkFragmentContainer, editorFragment).
        commit()
    }

    if (savedInstanceState != null) {
      val isEmoticonSelectorShown = 
        savedInstanceState.getBoolean(SelectEmoticonActivity.IsEmoticonSelectorShown, false)

      setSelectorVisibility(isEmoticonSelectorShown)
    }

    dialogFrame.setMessages(
      Message(MaidMaro.Half.Normal, getString(R.string.ActivityEditPlurkWelecomeMessage)) ::
      Nil
    )
  }

  override def onCreateOptionsMenu(menu: Menu): Boolean = {
    val inflater = getMenuInflater
    inflater.inflate(R.menu.activity_edit_plurk, menu)
    super.onCreateOptionsMenu(menu)
  }

  override def onOptionsItemSelected(menuItem: MenuItem): Boolean = menuItem.getItemId match {
    case R.id.activityEditPlurkActionPhotoFromGallery => startPhotoPicker(); false
    case R.id.activityEditPlurkActionPhotoFromCamera => startCamera(); false
    case R.id.activityEditPlurkActionEmoticon => toggleEmoticonSelector(); false
    case R.id.activityEditPlurkActionSend => editPlurk(); false
    case R.id.activityEditPlurkActionLogout => Logout.logout(this); false
    case R.id.activityEditPlurkActionAbout => AboutActivity.startActivity(this); false
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
      getString(R.string.Cancel), getString(R.string.ActivityEditPlurkConfirmAbort),
      getString(R.string.Yes), getString(R.string.No)
    ) 

    alertDialog.show(getSupportFragmentManager(), "ExitConfirm")
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
        EditPlurkActivity.this.finish()
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

  private def editPlurk() {
    val contentLength = editorFragment.getContentLength

    if (contentLength == 0) {
      dialogFrame.setMessages(
        Message(MaidMaro.Half.Normal, getString(R.string.UtilPlurkEditorEmptyNoticeMessage)) ::
        Nil
      )

    } else if (contentLength > 210) {

      dialogFrame.setMessages(
        Message(MaidMaro.Half.Normal, getString(R.string.UtilPlurkEditorOver210Message1)) ::
        Message(MaidMaro.Half.Smile, getString(R.string.UtilPlurkEditorOver210Message2)) ::
        Nil
      )

    } else {

      val progressDialogFragment = new ProgressDialogFragment(
        getString(R.string.ActivityEditPlurkEditing), 
        getString(R.string.PleaseWait)
      )

      progressDialogFragment.show(
        getSupportFragmentManager.beginTransaction, 
        "editPlurkProgress"
      )

      val oldRequestedOrientation = getRequestedOrientation
      setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED)

      val editedPlurkFuture = future {
        val newContent = editorFragment.getEditorContent.map(_._1.toString).
                                        getOrElse(this.rawContent)

        val newPlurk = plurkAPI.Timeline.plurkEdit(plurkID, newContent).get
        newPlurk
      }

      editedPlurkFuture.onSuccessInUI { case plurk =>
        val intent = new Intent

        intent.putExtra(EditPlurkActivity.PlurkIDBundle, plurk.plurkID)
        intent.putExtra(EditPlurkActivity.EditedContentBundle, plurk.content)
        intent.putExtra(
          EditPlurkActivity.EditedContentRawBundle, 
          plurk.contentRaw getOrElse null
        )

        setResult(Activity.RESULT_OK, intent)

        progressDialogFragment.dismiss()
        setRequestedOrientation(oldRequestedOrientation)

        Toast.makeText(
          this, getString(R.string.ActivityEditPlurkEditSuccess), 
          Toast.LENGTH_LONG
        ).show()

        finish()
      }

      editedPlurkFuture.onFailureInUI { case e =>
        setResult(Activity.RESULT_CANCELED)
        progressDialogFragment.dismiss()
        setRequestedOrientation(oldRequestedOrientation)
        dialogFrame.setMessages(
          Message(MaidMaro.Half.Panic, getString(R.string.ActivityEditPlurkFailureMessage1)) :: 
          Message(MaidMaro.Half.Normal, getString(R.string.ActivityEditPlurkFailureMessage2).format(e)) ::
          Nil
        )
      }
    }

  }

}
