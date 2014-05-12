package idv.brianhsu.maidroid.plurk.activity

import idv.brianhsu.maidroid.plurk._
import idv.brianhsu.maidroid.plurk.TypedResource._
import idv.brianhsu.maidroid.plurk.adapter._
import idv.brianhsu.maidroid.plurk.cache._
import idv.brianhsu.maidroid.plurk.fragment._
import idv.brianhsu.maidroid.plurk.util._
import idv.brianhsu.maidroid.ui.util.AsyncUI._
import idv.brianhsu.maidroid.ui.model._

import org.bone.soplurk.model.Icon

import android.app.Activity
import android.app.ProgressDialog
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.graphics.drawable.Drawable
import android.provider.MediaStore
import android.view.View
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.Menu
import android.view.MenuItem
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.content.DialogInterface

import android.support.v7.app.ActionBarActivity
import android.support.v7.app.ActionBar
import android.support.v7.app.ActionBar.TabListener
import android.support.v7.app.ActionBar.Tab
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentTransaction
import android.support.v4.view.ViewPager
import android.support.v4.view.ViewPager.OnPageChangeListener

import java.util.UUID

import scala.concurrent._
import android.net.Uri
import java.io.File
import android.text.Editable

object PostPlurkActivity {
  val REQUEST_PHOTO_PICKER = 1
}

class PostPlurkActivity extends ActionBarActivity 
                        with TabListener with OnPageChangeListener
                        with TypedViewHolder with EmoticonFragment.Listener
                        with SelectLimitedToDialog.Listener
                        with SelectBlockPeopleDialog.Listener
                        with PostPublicFragment.Listener
{

  private implicit def activity = this

  private lazy val viewPager = findView(TR.activityPostPlurkViewPager)
  private lazy val plurkAPI = PlurkAPIHelper.getPlurkAPI(this)
  private lazy val adapter = new PlurkEditorAdapter(getSupportFragmentManager)
  private lazy val dialogFrame = findView(TR.activityPostPlurkDialogFrame)

  private var prevEditorContentHolder: Option[(Editable, Int)] = None
  private var isSliding: Boolean = false

  override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_post_plurk)
    val actionbar = getSupportActionBar

    actionbar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS)
    actionbar.addTab(actionbar.newTab().setText("一般").setTabListener(this))
    actionbar.addTab(actionbar.newTab().setText("私噗").setTabListener(this))
    actionbar.addTab(actionbar.newTab().setText("背刺").setTabListener(this))
 
    viewPager.setAdapter(adapter)
    viewPager.setOnPageChangeListener(this)

    dialogFrame.setMessages(
      Message(MaidMaro.Half.Happy, "主人有有趣的事情想要和大家分享嗎？請告訴小鈴，小鈴會幫主人發到噗浪上喔！", None) :: 
      Message(MaidMaro.Half.Smile, "如果主人只想和某些特定的好友分享的話，也可以切換到「私噗」標籤，選擇要和誰分享喲。") ::
      Nil
    )
  }


  override def onCreateOptionsMenu(menu: Menu): Boolean = {
    val inflater = getMenuInflater
    inflater.inflate(R.menu.post_plurk, menu)
    super.onCreateOptionsMenu(menu)
  }

  override def onOptionsItemSelected(menuItem: MenuItem): Boolean = menuItem.getItemId match {
    case R.id.postPlurkActionEmoticon => toggleEmoticonSelector(); false
    case R.id.postPlurkActionPhotoFromGallery => startPhotoPicker(); false
    case R.id.postPlurkActionPhotoFromCamera => startCamera(); false
    case R.id.postPlurkActionSend => postPlurk(); false
    case _ => super.onOptionsItemSelected(menuItem)
  }

  override def onTabReselected(tab: Tab, ft: FragmentTransaction) {}
  override def onTabUnselected(tab: Tab, ft: FragmentTransaction) {
  }

  override def onPageScrolled(
    position: Int, positionOffset: Float, 
    positionOffsetPixels: Int) {}

  override def onTabSelected(tab: Tab, ft: FragmentTransaction) {

    if (!isSliding) {
      for {
        editor <- Option(getCurrentEditor)
      } {
        prevEditorContentHolder = editor.getEditorContent
      }
    }

    viewPager.setCurrentItem(tab.getPosition, true)

    if (!isSliding) {
      prevEditorContentHolder.foreach { content =>
        getCurrentEditor.setEditorContent(content)
      }
    }

  }

  override def onPageScrollStateChanged(state: Int) {
    if (state == ViewPager.SCROLL_STATE_IDLE) {
      prevEditorContentHolder.foreach { content =>
        getCurrentEditor.setEditorContent(content)
      }
    } else if (state == ViewPager.SCROLL_STATE_DRAGGING ) {
      prevEditorContentHolder = getCurrentEditor.getEditorContent
    }
  }

  override def onPageSelected(position: Int) {
    isSliding = true
    getSupportActionBar.setSelectedNavigationItem(position)
    isSliding = false
  }

  override def onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {

    super.onActivityResult(requestCode, resultCode, data)

    requestCode match {
      case PostPlurkActivity.REQUEST_PHOTO_PICKER => processImage(resultCode, data)
      case _ => 
    }
  }

  override def onIconSelected(icon: Icon, drawable: Option[Drawable]) {
    toggleEmoticonSelector()
    getCurrentEditor.insertIcon(icon, drawable)
  }

  override def onPeopleSelected(selectedCliques: Set[String], 
                                selectedUsers: Set[(Long, String)]) {
    val editor = getCurrentEditor
    editor.setSelected(selectedCliques, selectedUsers)
  }

  override def onBlockSelected(selectedCliques: Set[String], 
                                selectedUsers: Set[(Long, String)]) {
    val editor = getCurrentEditor
    editor.setBlocked(selectedCliques, selectedUsers)
  }

  override def onActionSendImage(uri: Uri) {
    uploadFile(getFileFromUri(uri))
  }

  def onActionSendMultipleImage(uriList: List[Uri]) {
    uploadFiles(uriList.map(getFileFromUri))
  }

  private def toggleEmoticonSelector() {
    val fm = getSupportFragmentManager
    val selectorHolder = Option(fm.findFragmentById(R.id.activityPostPlurkEmtoicon))

    selectorHolder match {
      case Some(selector) if selector.isHidden =>
        fm.beginTransaction.
          setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN).
          show(selector).commit()

      case Some(selector)  =>
        fm.beginTransaction.
          setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN).
          hide(selector).commit()

      case None =>
        fm.beginTransaction.
          setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN).
          replace(R.id.activityPostPlurkEmtoicon, new EmoticonFragment).
          commit()
    }
  }

  private def getCurrentEditor = {
    val tag = s"android:switcher:${R.id.activityPostPlurkViewPager}:${viewPager.getCurrentItem}"
    getSupportFragmentManager().findFragmentByTag(tag).asInstanceOf[PlurkEditor]
  }

  private def postPlurk() {
    val progressDialogFragment = new ProgressDialogFragment("發噗中", "請稍候……")
    progressDialogFragment.show(getSupportFragmentManager.beginTransaction, "uploadFileProgress")
    val oldRequestedOrientation = getRequestedOrientation
    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED)

    val postedPlurkFuture = getCurrentEditor.postPlurk()

    postedPlurkFuture.onSuccessInUI { case content =>
      setResult(Activity.RESULT_OK)
      progressDialogFragment.dismiss()
      finish()
    }

    postedPlurkFuture.onFailureInUI { case e =>
      setResult(Activity.RESULT_CANCELED)
      progressDialogFragment.dismiss()
      dialogFrame.setMessages(
        Message(MaidMaro.Half.Panic, "對不起！小鈴太沒用了，沒辦法順利幫主人把這則噗放到噗浪上……", None) :: 
        Message(MaidMaro.Half.Normal, s"系統說錯誤的原因是：${e}，可不可以請主人檢查一次之後再重新按發送鍵一次呢？") ::
        Nil
      )
    }
  }

  private def startCamera() {
    val intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE)
    startActivityForResult(intent, PostPlurkActivity.REQUEST_PHOTO_PICKER)
  }

  private def startPhotoPicker() {
    val photoPickerIntent = new Intent(Intent.ACTION_GET_CONTENT)
    photoPickerIntent.addCategory(Intent.CATEGORY_OPENABLE)
    photoPickerIntent.setType("image/*")
    val photoChooserIntent = Intent.createChooser(photoPickerIntent, "選擇一張圖片")
    startActivityForResult(photoChooserIntent, PostPlurkActivity.REQUEST_PHOTO_PICKER)
  }

  private def getFileFromUri(uri: Uri) = {
    val column = Array(android.provider.MediaStore.MediaColumns.DATA)
    val cursor = getContentResolver().query(uri, column, null, null, null)
    cursor.moveToFirst()
    val file = new File(cursor.getString(0))
    cursor.close()
    file
  }

  private def uploadFiles(fileList: List[File]) {

    val progressDialogFragment = new ProgressDialogFragment(
      "上傳圖檔", "請稍候……", 
      Some(fileList.size)
    )

    progressDialogFragment.show(
      getSupportFragmentManager.beginTransaction, 
      "uploadFileProgress"
    )

    val oldRequestedOrientation = getRequestedOrientation
    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED)

    val imageListFuture = future {
      fileList.zipWithIndex.foreach { case(file, index) =>
        val (imageURL, bitmapDrawable) = uploadToPlurk(file)
        activity.runOnUIThread {
          getCurrentEditor.insertDrawable(s" ${imageURL} ", bitmapDrawable) 
          progressDialogFragment.setProgress(index + 1)
        }
      }
    }

    imageListFuture.onSuccessInUI { _ => 
      progressDialogFragment.dismiss()
      setRequestedOrientation(oldRequestedOrientation)
      dialogFrame.setMessages(
        Message(MaidMaro.Half.Happy, "小鈴已經幫主人把這些照片上傳了喲，主人的照片好多喲，難道說主人是照片松鼠嗎？") :: 
        Nil
      )
    }

    imageListFuture.onFailureInUI { case e: Exception => 
      DebugLog(s"====> upload image list failed:$e", e)
      progressDialogFragment.dismiss()
      setRequestedOrientation(oldRequestedOrientation)
      dialogFrame.setMessages(
        Message(MaidMaro.Half.Normal, "咦？為什麼照片沒辦法順利傳到噗浪上面呢……", None) :: 
        Message(MaidMaro.Half.Normal, s"系統說錯誤的原因是 ${e} 的說。") ::
        Message(MaidMaro.Half.Smile, "主人要不要檢查一下之後再重試一次呢？") ::
        Nil
      )
    }

  }

  private def uploadToPlurk(file: File) = {
    val resizedBitmap = ImageSampleFactor.resizeImageFile(file, 800)
    val thumbnailBitmap = ImageSampleFactor.resizeImageFile(file, 100, true)

    val randomUUID = UUID.randomUUID.toString
    val newFile = DiskCacheHelper.writeBitmapToCache(
      PostPlurkActivity.this, randomUUID, resizedBitmap
    ).get
      
    val bitmapDrawable = new BitmapDrawable(getResources, thumbnailBitmap)
    val imageURL = plurkAPI.Timeline.uploadPicture(newFile).get._1
    (imageURL, bitmapDrawable)
  }

  private def uploadFile(file: File) {

    val progressDialogFragment = new ProgressDialogFragment("上傳圖檔", "請稍候……")
    progressDialogFragment.show(getSupportFragmentManager.beginTransaction, "uploadFileProgress")
    val oldRequestedOrientation = getRequestedOrientation
    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED)

    val imageURLFuture = future { uploadToPlurk(file) }

    imageURLFuture.onSuccessInUI { case (imageURL, bitmapDrawable) => 
      getCurrentEditor.insertDrawable(s" ${imageURL} ", bitmapDrawable) 
      progressDialogFragment.dismiss()
      setRequestedOrientation(oldRequestedOrientation)
      dialogFrame.setMessages(
        Message(MaidMaro.Half.Smile, "照片已經上傳到噗浪上了，主人快點把它分享給大家吧！") :: 
        Nil
      )
    }

    imageURLFuture.onFailureInUI { case e: Exception => 
      DebugLog(s"====> upload image failed:$e", e)
      progressDialogFragment.dismiss()
      setRequestedOrientation(oldRequestedOrientation)
      dialogFrame.setMessages(
        Message(MaidMaro.Half.Normal, "咦？為什麼照片沒辦法順利傳到噗浪上面呢……", None) :: 
        Message(MaidMaro.Half.Normal, s"系統說錯誤的原因是 ${e} 的說。") ::
        Message(MaidMaro.Half.Smile, "主人要不要檢查一下之後再重試一次呢？") ::
        Nil
      )

    }
  }


  private def showWarningDialog() {
    val alertDialog = new AlertDialog.Builder(this).setCancelable(true).setTitle("取消").setMessage("確定要取消發噗嗎？這會造成目前的內容永遠消失喲！")
    alertDialog.setPositiveButton("是", new DialogInterface.OnClickListener() {
      override def onClick(dialog: DialogInterface, which: Int) {
        setResult(Activity.RESULT_CANCELED)
        dialog.dismiss()
        PostPlurkActivity.this.finish()
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

    val isEmoticonSelectedShown = Option(
      getSupportFragmentManager.findFragmentById(R.id.activityPostPlurkEmtoicon)
    ).exists(_.isVisible)

    if (isEmoticonSelectedShown) {
      toggleEmoticonSelector()
    } else {
      showWarningDialog()
    }

  }

  private def processImage(resultCode: Int, data: Intent) {
    if (resultCode == Activity.RESULT_OK) {
      uploadFile(getFileFromUri(data.getData))
    }
  }
}
