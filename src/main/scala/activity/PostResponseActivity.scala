package idv.brianhsu.maidroid.plurk.activity

import idv.brianhsu.maidroid.ui.model._
import idv.brianhsu.maidroid.plurk._
import idv.brianhsu.maidroid.plurk.util._
import idv.brianhsu.maidroid.plurk.fragment._

import org.bone.soplurk.api.PlurkAPI._
import org.bone.soplurk.model._

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.view.View
import android.view.Menu
import android.view.MenuItem
import android.support.v7.app.ActionBarActivity
import android.app.AlertDialog
import android.content.DialogInterface

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
    /*
    case R.id.postPlurkActionSend => postPlurk(); false
    */
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

}
