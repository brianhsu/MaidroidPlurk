package idv.brianhsu.maidroid.plurk.activity

import idv.brianhsu.maidroid.ui.model._
import idv.brianhsu.maidroid.plurk._
import idv.brianhsu.maidroid.plurk.util._
import idv.brianhsu.maidroid.plurk.fragment._
import idv.brianhsu.maidroid.ui.util.AsyncUI._

import org.bone.soplurk.api.PlurkAPI._
import org.bone.soplurk.model._

import android.app.Activity
import android.content.pm.ActivityInfo

import android.os.Bundle
import android.content.Intent
import android.view.View
import android.view.Menu
import android.view.MenuItem
import android.support.v7.app.ActionBarActivity
import android.app.AlertDialog
import android.content.DialogInterface
import android.widget.Toast

object PostResponseActivity {
  val PlurkIDBundle = "idv.brianhsu.maidroid.plurk.PostResponseActivity.plurkID"
}

class PostResponseActivity extends ActionBarActivity 
                           with SelectImageActivity 
                           with SelectEmoticonActivity
                           with EmoticonFragment.Listener
                           with TypedViewHolder 
{
  protected val emoticonFragmentHolderResID = R.id.activityPostResponseEmtoicon
  protected lazy val editorFragment = new PostResponseFragment
  protected lazy val dialogFrame = findView(TR.activityPostResponseDialogFrame)
  protected lazy val plurkAPI = PlurkAPIHelper.getPlurkAPI(this)
  private lazy val plurkID = getIntent.getLongExtra(PostResponseActivity.PlurkIDBundle, -1)

  def getCurrentEditor = getSupportFragmentManager.
    findFragmentById(R.id.activityPostResponseFragmentContainer).asInstanceOf[PlurkEditor]

  override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_post_response)

    if (getCurrentEditor == null) {
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

    dialogFrame.setMessages(
      Message(MaidMaro.Half.Smile, "小鈴已經準備好幫主人回應訊息了，如果主人準備好要發送的話，請再告訴我一聲喲！", None) ::
      Nil
    )
  }

  override def onCreateOptionsMenu(menu: Menu): Boolean = {
    val inflater = getMenuInflater
    inflater.inflate(R.menu.post_plurk, menu)
    super.onCreateOptionsMenu(menu)
  }

  override def onOptionsItemSelected(menuItem: MenuItem): Boolean = menuItem.getItemId match {
    case R.id.postPlurkActionPhotoFromGallery => startPhotoPicker(); false
    case R.id.postPlurkActionPhotoFromCamera => startCamera(); false
    case R.id.postPlurkActionEmoticon => toggleEmoticonSelector(); false
    case R.id.postPlurkActionSend => postResponse(); false
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
    val alertDialog = new AlertDialog.Builder(this).
                        setCancelable(true).
                        setTitle("取消").
                        setMessage("確定要取消回應嗎？這會造成目前的內容永遠消失喲！")

    alertDialog.setPositiveButton("是", new DialogInterface.OnClickListener() {
      override def onClick(dialog: DialogInterface, which: Int) {
        setResult(Activity.RESULT_CANCELED)
        dialog.dismiss()
        PostResponseActivity.this.finish()
      }
    })

    alertDialog.setNegativeButton("否", new DialogInterface.OnClickListener() {
      override def onClick(dialog: DialogInterface, which: Int) {
        dialog.dismiss()
      }
    })
    alertDialog.show()
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
    val contentLength = getCurrentEditor.getContentLength

    if (contentLength == 0) {
      dialogFrame.setMessages(
        Message(MaidMaro.Half.Normal, "主人沒有寫任何東西呢，主人是不是不小心按到發送鍵了啊？ ", None) ::
        Message(MaidMaro.Half.Happy, "不過沒關係的，等主人準備好了之後歡迎隨時告訴小鈴喲！") ::
        Nil
      )
    } else if (contentLength > 210) {
      dialogFrame.setMessages(
        Message(MaidMaro.Half.Normal, "對不起，主人的發言超過噗浪上限的 210 字元呢……", None) ::
        Message(MaidMaro.Half.Normal, "可能要麻煩主人稍微刪除一些內容，或是分成兩篇回應喲。") ::
        Nil
      )
    } else if (plurkID != -1) {
      val progressDialogFragment = new ProgressDialogFragment("發噗中", "請稍候……")
      progressDialogFragment.show(getSupportFragmentManager.beginTransaction, "uploadFileProgress")
      val oldRequestedOrientation = getRequestedOrientation
      setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED)

      val responseFuture = getCurrentEditor.postResponse(plurkID)
      responseFuture.onSuccessInUI { _ =>
        setResult(Activity.RESULT_OK)
        progressDialogFragment.dismiss()
        setRequestedOrientation(oldRequestedOrientation)
        Toast.makeText(this, "已成功發送至噗浪", Toast.LENGTH_LONG).show()
        this.finish()
      }

      responseFuture.onFailureInUI { case e: Exception =>
        progressDialogFragment.dismiss()
        setRequestedOrientation(oldRequestedOrientation)
        if (e.getMessage.contains("No permissions")) {
          dialogFrame.setMessages(
            Message(MaidMaro.Half.Normal, "主人真是對不起，對方好像把這則噗設成只有朋友能夠發文呢……", None) ::
            Message(MaidMaro.Half.Normal, "都怪小鈴沒事先提醒主人，真是抱歉。", None) :: 
            Nil
          )
        } else {
          dialogFrame.setMessages(
            Message(MaidMaro.Half.Panic, "對不起！小鈴太沒用了，沒辦法順利幫主人把這則回應發到噗浪上……", None) :: 
            Message(MaidMaro.Half.Normal, s"系統說錯誤的原因是：${e}，可不可以請主人檢查一次之後再重新按發送鍵一次呢？", None) ::
            Nil
          )
        }
      }
    }
  }

}
