package idv.brianhsu.maidroid.plurk.activity

import idv.brianhsu.maidroid.ui.model._
import idv.brianhsu.maidroid.plurk._
import idv.brianhsu.maidroid.plurk.dialog._
import idv.brianhsu.maidroid.plurk.util._
import idv.brianhsu.maidroid.plurk.cache._
import idv.brianhsu.maidroid.plurk.fragment._
import idv.brianhsu.maidroid.ui.util.AsyncUI._
import idv.brianhsu.maidroid.ui.DialogFrame

import org.bone.soplurk.api.PlurkAPI

import android.app.Activity
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.provider.MediaStore
import android.content.pm.ActivityInfo

import android.support.v7.app.ActionBarActivity

import java.io.File
import java.util.UUID

import scala.concurrent._

object SelectImageActivity {
  val RequestPhotoPicker = 1
}

trait SelectImageActivity {
  this: ActionBarActivity =>

  protected implicit def activity = this

  protected def plurkAPI: PlurkAPI
  protected def getCurrentEditor: PlurkEditor
  protected def dialogFrame: DialogFrame

  protected def processImage(resultCode: Int, data: Intent) {
    if (resultCode == Activity.RESULT_OK) {
      uploadFile(getFileFromUri(data.getData))
    }
  }

  protected def startCamera() {
    val intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE)
    startActivityForResult(intent, SelectImageActivity.RequestPhotoPicker)
  }

  protected def startPhotoPicker() {
    val photoPickerIntent = new Intent(Intent.ACTION_GET_CONTENT)
    photoPickerIntent.addCategory(Intent.CATEGORY_OPENABLE)
    photoPickerIntent.setType("image/*")
    val photoChooserIntent = Intent.createChooser(photoPickerIntent, "選擇一張圖片")
    startActivityForResult(photoChooserIntent, SelectImageActivity.RequestPhotoPicker)
  }

  protected def getFileFromUri(uri: Uri) = {
    val column = Array(android.provider.MediaStore.MediaColumns.DATA)
    val cursor = getContentResolver().query(uri, column, null, null, null)
    cursor.moveToFirst()
    val file = new File(cursor.getString(0))
    cursor.close()
    file
  }

  protected def uploadFiles(fileList: List[File]) {

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
        this.runOnUIThread {
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

  protected def uploadToPlurk(file: File) = {
    val resizedBitmap = ImageSampleFactor.resizeImageFile(file, 800, true)
    val thumbnailBitmap = ImageSampleFactor.resizeImageFile(file, 100, true)

    val randomUUID = UUID.randomUUID.toString
    val newFile = DiskCacheHelper.writeBitmapToCache(this, randomUUID, resizedBitmap).get
      
    val bitmapDrawable = new BitmapDrawable(getResources, thumbnailBitmap)
    val imageURL = plurkAPI.Timeline.uploadPicture(newFile).get._1
    (imageURL, bitmapDrawable)
  }

  protected def uploadFile(file: File) {

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

}
