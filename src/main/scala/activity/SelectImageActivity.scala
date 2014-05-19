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
import android.widget.Toast

import android.support.v7.app.ActionBarActivity

import java.io.File
import java.util.UUID

import scala.concurrent._
import scala.util.{Try, Success, Failure}

object SelectImageActivity {
  val RequestPhotoPicker = 1
}

trait SelectImageActivity {
  this: ActionBarActivity =>

  object NoSuchFileException extends Exception(
    getString(R.string.activitySelectImageFileNotFound)
  )

  protected implicit def activity = this

  protected def plurkAPI: PlurkAPI
  protected def getCurrentEditor: PlurkEditor
  protected def dialogFrame: DialogFrame

  protected def processImage(resultCode: Int, data: Intent) {
    if (resultCode == Activity.RESULT_OK) {
      uploadFile(data.getData)
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

  protected def getLocalFile(uri: Uri): Try[File] = Try {
    val column = Array(android.provider.MediaStore.MediaColumns.DATA)
    val cursor = getContentResolver().query(uri, column, null, null, null)

    try {

      cursor.moveToFirst()

      val file = new File(cursor.getString(0))

      if (!file.exists) {
        throw NoSuchFileException
      }

      file
    } catch {
      case NoSuchFileException => throw NoSuchFileException
      case e: Exception => throw e
    }
  }

  protected def uploadFiles(uriList: List[Uri]) {

    val progressDialogFragment = new ProgressDialogFragment(
      getString(R.string.activitySelectImagePreparing), 
      getString(R.string.pleaseWait),
      Some(uriList.size)
    )

    progressDialogFragment.show(
      getSupportFragmentManager.beginTransaction, 
      "uploadFileProgress"
    )

    val oldRequestedOrientation = getRequestedOrientation
    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED)

    val imageListFuture = future {
      uriList.zipWithIndex.foreach { case(uri, index) =>

        val (imageURL, bitmapDrawable) = readAndUploadFile(progressDialogFragment, uri, Some(index))
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

  protected def uploadToPlurk(file: File, uploadingCallback: (Long, Long) => Any) = {
    val resizedBitmap = ImageSampleFactor.resizeImageFile(file, 800, true)
    val thumbnailBitmap = ImageSampleFactor.resizeImageFile(file, 100, true)

    val randomUUID = UUID.randomUUID.toString
    val newFile = DiskCacheHelper.writeBitmapToCache(this, randomUUID, resizedBitmap).get
      
    val bitmapDrawable = new BitmapDrawable(getResources, thumbnailBitmap)
    val imageURL = plurkAPI.Timeline.uploadPicture(newFile, uploadingCallback).get._1
    (imageURL, bitmapDrawable)
  }


  private def readAndUploadFile(dialog: ProgressDialogFragment, uri: Uri, 
                                photoCount: Option[Int] = None) = {

    def formatedByteCount(bytes: Long) = {
      val unit = 1024
      if (bytes < unit) {
        bytes + " B"
      } else {
        val exp = (Math.log(bytes) / Math.log(unit)).asInstanceOf[Int]
        val pre = "KMGTPE".charAt(exp-1)
        "%.1f %sB".format(bytes / Math.pow(unit, exp), pre)
      }
    }

    def updateWhenDownloading(bytes: Long) {
      activity.runOnUIThread {
        val title = getString(R.string.activitySelectImageDownloading)
        dialog.setTitle(title + formatedByteCount(bytes))
      }
    }

    def updateWhenUploading(totalSize: Long, bytes: Long) {
      activity.runOnUIThread {

        val title = getString(R.string.activitySelectImageUploading)

        photoCount match {
          case Some(count) =>
            dialog.setTitle(title + formatedByteCount(bytes))
            dialog.setProgress(count)
          case None =>
            val title = getString(R.string.activitySelectImageUploading)
            dialog.setTitle(title + formatedByteCount(bytes), bytes.toInt, totalSize.toInt)
        }
      }
    }

    def updateRemoteFile(uri: Uri) = {
      DiskCacheHelper.writeUriToCache(this, uri, updateWhenDownloading) match {
        case Some(file) => 
          this.runOnUIThread {
            dialog.setTitle(getString(R.string.activitySelectImagePreparing))
          }
          uploadToPlurk(file, updateWhenUploading)
        case None => 
          throw new Exception(getString(R.string.activitySelectImageFetchFileException))
      }
    }

    getLocalFile(uri) match {
      case Success(file) => 
        uploadToPlurk(file, updateWhenUploading)
      case _ => 
        updateRemoteFile(uri)
    }
  }

  protected def uploadFile(uri: Uri) {

    val progressDialogFragment = new ProgressDialogFragment(
      getString(R.string.activitySelectImagePreparing),
      getString(R.string.pleaseWait),
      Some(0)
    )

    progressDialogFragment.show(
      getSupportFragmentManager.beginTransaction, 
      "uploadFileProgress"
    )

    val oldRequestedOrientation = getRequestedOrientation
    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED)

    val imageURLFuture = future { readAndUploadFile(progressDialogFragment, uri) }

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
