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
      Message(MaidMaro.Half.Normal, "主人不滿意這則噗嗎？小鈴知道了，請主人編輯完成後再通知我一聲喲！", None) ::
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
      this, 'ExitConfirm, "取消", "確定要退出嗎？這會造成目前的內容永遠消失喲！", "是", "否"
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
        Message(MaidMaro.Half.Normal, "主人還沒有填寫內容呢，這樣小鈴沒辦法幫主人發到噗浪上喲……", None) :: 
        Nil
      )

    } else if (contentLength > 210) {

      dialogFrame.setMessages(
        Message(MaidMaro.Half.Normal, "對不起，字數超過噗浪的 210 個字元的上限了呢……", None) ::
        Message(MaidMaro.Half.Smile, "主人要不要先刪除一些贅字，或試著寫得精鍊一些呢？", None) ::
        Nil
      )

    } else {

      val progressDialogFragment = new ProgressDialogFragment("編輯中", "請稍候……")
      progressDialogFragment.show(getSupportFragmentManager.beginTransaction, "editPlurkProgress")
      val oldRequestedOrientation = getRequestedOrientation
      setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED)

      val editedPlurkFuture = future {
        val newContent = editorFragment.getEditorContent.map(_._1.toString) getOrElse this.rawContent
        val newPlurk = plurkAPI.Timeline.plurkEdit(plurkID, newContent).get
        newPlurk
      }

      editedPlurkFuture.onSuccessInUI { case plurk =>
        val intent = new Intent
        intent.putExtra(EditPlurkActivity.PlurkIDBundle, plurk.plurkID)
        intent.putExtra(EditPlurkActivity.EditedContentBundle, plurk.content)
        intent.putExtra(EditPlurkActivity.EditedContentRawBundle, plurk.contentRaw getOrElse null)
        setResult(Activity.RESULT_OK, intent)
        progressDialogFragment.dismiss()
        setRequestedOrientation(oldRequestedOrientation)
        Toast.makeText(this, "已成功編輯此噗", Toast.LENGTH_LONG).show()
        finish()
      }

      editedPlurkFuture.onFailureInUI { case e =>
        setResult(Activity.RESULT_CANCELED)
        progressDialogFragment.dismiss()
        setRequestedOrientation(oldRequestedOrientation)
        dialogFrame.setMessages(
          Message(MaidMaro.Half.Panic, "對不起！小鈴太沒用了，沒辦法順利幫主更新這則噗浪……", None) :: 
          Message(MaidMaro.Half.Normal, s"系統說錯誤的原因是：${e}，可不可以請主人檢查一次之後再重新按發送鍵一次呢？") ::
          Nil
        )
      }
    }

  }

}
