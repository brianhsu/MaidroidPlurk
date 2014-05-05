package idv.brianhsu.maidroid.plurk.view

import idv.brianhsu.maidroid.plurk._
import idv.brianhsu.maidroid.plurk.TypedResource._
import idv.brianhsu.maidroid.plurk.cache.ImageCache
import idv.brianhsu.maidroid.ui.util.AsyncUI._

import org.bone.soplurk.model.Icon

import android.app.Activity
import android.util.AttributeSet
import android.widget.LinearLayout
import android.view.LayoutInflater
import android.graphics.Bitmap

import scala.concurrent._

class IconView(activity: Activity) extends LinearLayout(activity) {

  private implicit def mActivity = activity
  private lazy val inflater = LayoutInflater.from(activity)
  private lazy val imageView = this.findView(TR.item_icon_image)

  private var iconName: String = _

  inflater.inflate(R.layout.item_icon, this, true)

  def update(icon: Icon) {
    this.iconName = icon.name
    ImageCache.getBitmapFromCache(activity, icon.url) match {
      case Some(bitmap) => setIconFromCacheBitmap(bitmap)
      case None =>
    }
  }

  def setIconFromCacheBitmap(iconBitmap: Bitmap) {
    imageView.setImageBitmap(iconBitmap)
  }

  def setIconFromCacheNetwork(icon: Icon) {
    val futureIconBitmap = ImageCache.getBitmapFromNetwork(activity, icon.url, 1000)
    futureIconBitmap.onSuccessInUI { bitmap =>
      if (icon.name == iconName) {
        imageView.setImageBitmap(bitmap)
      }
    }
  }

}
