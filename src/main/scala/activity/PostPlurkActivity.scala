package idv.brianhsu.maidroid.plurk.activity

import idv.brianhsu.maidroid.plurk._
import idv.brianhsu.maidroid.plurk.TypedResource._
import idv.brianhsu.maidroid.plurk.adapter._
import idv.brianhsu.maidroid.plurk.cache._
import idv.brianhsu.maidroid.plurk.fragment._
import idv.brianhsu.maidroid.plurk.util._
import idv.brianhsu.maidroid.ui.util.AsyncUI._

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.Menu
import android.view.MenuItem
import android.graphics.Bitmap

import android.support.v7.app.ActionBarActivity
import android.support.v7.app.ActionBar
import android.support.v7.app.ActionBar.TabListener
import android.support.v7.app.ActionBar.Tab
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentTransaction
import android.support.v4.view.ViewPager.OnPageChangeListener

import java.util.UUID

import scala.concurrent._
import android.net.Uri
import java.io.File

object PostPlurkActivity {
  val REQUEST_PHOTO_PICKER = 1
}

class PostPlurkActivity extends ActionBarActivity 
                        with TabListener with OnPageChangeListener
                        with TypedViewHolder 
{

  private implicit def activity = this
  private lazy val viewPager = findView(TR.activityPostPlurkViewPager)
  private lazy val plurkAPI = PlurkAPIHelper.getPlurkAPI(this)
  private lazy val adapter = new PlurkEditorAdapter(getSupportFragmentManager)

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
  }

  override def onCreateOptionsMenu(menu: Menu): Boolean = {
    val inflater = getMenuInflater
    inflater.inflate(R.menu.post_plurk, menu)
    super.onCreateOptionsMenu(menu)
  }

  override def onOptionsItemSelected(menuItem: MenuItem): Boolean = menuItem.getItemId match {
    case R.id.postPlurkActionPhotoFromGallery => startPhotoPicker(); false
    case R.id.postPlurkActionPhotoFromCamera => startCamera(); false
    case _ => super.onOptionsItemSelected(menuItem)
  }

  override def onTabReselected(tab: Tab, ft: FragmentTransaction) {  }
  override def onTabUnselected(tab: Tab, ft: FragmentTransaction) {}
  override def onPageScrollStateChanged(state: Int) {}
  override def onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

  override def onTabSelected(tab: Tab, ft: FragmentTransaction) {
    viewPager.setCurrentItem(tab.getPosition)
  }

  override def onPageSelected(position: Int) {
    getSupportActionBar.setSelectedNavigationItem(position)
  }

  override def onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {

    requestCode match {
      case PostPlurkActivity.REQUEST_PHOTO_PICKER => processImage(resultCode, data)
      case _ => super.onActivityResult(requestCode, resultCode, data)
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

  private def uploadFile(file: File) {

    val progressDialog = ProgressDialog.show(this, "上傳圖檔", "請稍候……")
    val oldRequestedOrientation = getRequestedOrientation
    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR)

    new Thread() {

      private def writeToCache(bitmap: Bitmap) = {
        DiskCacheHelper.writeBitmapToCache(PostPlurkActivity.this, UUID.randomUUID.toString, bitmap)
      }

      private def getCurrentEditor = {
        val tag = s"android:switcher:${R.id.activityPostPlurkViewPager}:${viewPager.getCurrentItem}"
        getSupportFragmentManager().findFragmentByTag(tag).asInstanceOf[PlurkEditor]
      }

      override def run() {
        try {
          val resizedBitmap = ImageSampleFactor.resizeImageFile(file, 800)

          for {
            newFile <- writeToCache(resizedBitmap)
            (imageURL, _) <- plurkAPI.Timeline.uploadPicture(newFile).toOption
          } {
            PostPlurkActivity.this.runOnUIThread { getCurrentEditor.insertImage(imageURL) }
          }

        } catch {
          case e: Exception => DebugLog(s"====> uploadFileError:$e", e)
        } finally {
          progressDialog.dismiss()
          setRequestedOrientation(oldRequestedOrientation)
        }
      }
    }.start
  }


  private def processImage(resultCode: Int, data: Intent) {
    if (resultCode == Activity.RESULT_OK) {
      uploadFile(getFileFromUri(data.getData))
    }
  }
}
