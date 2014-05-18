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
    val photoChooserIntent = Intent.createChooser(
      photoPickerIntent, 
      getString(R.string.activitySelectImageTitle)
    )
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
      getString(R.string.activitySelectImageUploading), 
      getString(R.string.pleaseWait),
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
        Message(MaidMaro.Half.Happy, getString(R.string.activitySelectImageUploadMultipleOK)) :: 
        Nil
      )
    }

    imageListFuture.onFailureInUI { case e: Exception => 
      DebugLog(s"====> upload image list failed:$e", e)
      progressDialogFragment.dismiss()
      setRequestedOrientation(oldRequestedOrientation)
      dialogFrame.setMessages(
        Message(MaidMaro.Half.Normal, getString(R.string.activitySelectImageUploadMultipleFailure01)) :: 
        Message(MaidMaro.Half.Normal, getString(R.string.activitySelectImageUploadMultipleFailure02).format(e.getMessage)) ::
        Message(MaidMaro.Half.Smile, getString(R.string.activitySelectImageUploadMultipleFailure03)) ::
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

    val progressDialogFragment = new ProgressDialogFragment(
      getString(R.string.activitySelectImageUploading),
      getString(R.string.pleaseWait)
    )
    progressDialogFragment.show(getSupportFragmentManager.beginTransaction, "uploadFileProgress")
    val oldRequestedOrientation = getRequestedOrientation
    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED)

    val imageURLFuture = future { uploadToPlurk(file) }

    imageURLFuture.onSuccessInUI { case (imageURL, bitmapDrawable) => 
      getCurrentEditor.insertDrawable(s" ${imageURL} ", bitmapDrawable) 
      progressDialogFragment.dismiss()
      setRequestedOrientation(oldRequestedOrientation)
      dialogFrame.setMessages(
        Message(MaidMaro.Half.Smile, getString(R.string.activitySelectImageUploadOK)) :: 
        Nil
      )
    }

    imageURLFuture.onFailureInUI { case e: Exception => 
      DebugLog(s"====> upload image failed:$e", e)
      progressDialogFragment.dismiss()
      setRequestedOrientation(oldRequestedOrientation)
      dialogFrame.setMessages(
        Message(MaidMaro.Half.Normal, getString(R.string.activitySelectImageUploadFailure01)) :: 
        Message(MaidMaro.Half.Normal, getString(R.string.activitySelectImageUploadFailure02).format(e.getMessage)) ::
        Message(MaidMaro.Half.Smile, getString(R.string.activitySelectImageUploadFailure03)) ::
        Nil
      )

    }
  }

}
