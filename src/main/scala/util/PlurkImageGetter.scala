package idv.brianhsu.maidroid.plurk.util

import idv.brianhsu.maidroid.plurk._
import idv.brianhsu.maidroid.plurk.cache._
import idv.brianhsu.maidroid.ui.util.AsyncUI._

import scala.concurrent._

import android.app.Activity
import android.widget.BaseAdapter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.content.res.Resources
import android.text.Html
import android.util.DisplayMetrics

import java.net.URL

class PlurkImageGetter(activity: Activity, adapter: BaseAdapter) extends Html.ImageGetter {

  private implicit val implicitActivity = activity
  private val metrics = new DisplayMetrics
  private var mPlaceHolder: Option[Bitmap] = Option(BitmapFactory.decodeResource(activity.getResources, R.drawable.placeholder))
  private lazy val thumbnailSize = (metrics.widthPixels * 0.2).toInt

  activity.getWindowManager.getDefaultDisplay.getMetrics(metrics)

  private def placeHolder = {
    mPlaceHolder.filterNot(_.isRecycled) match {
      case Some(bitmap) => bitmap
      case None =>
        val newBitmap = BitmapFactory.decodeResource(
          activity.getResources, R.drawable.placeholder
        )
        mPlaceHolder = Option(newBitmap)
        newBitmap
    }
  }

  class URLDrawable(resources: Resources, loadingBitmap: Bitmap) extends 
        BitmapDrawable(resources, loadingBitmap) 
  {
    private var drawableHolder: Option[Drawable] = None

    this.setBounds(0, 0, super.getIntrinsicWidth, super.getIntrinsicHeight)

    override def draw(canvas: android.graphics.Canvas) {
      drawableHolder match {
        case Some(drawable) => drawable draw canvas
        case None => 
          super.draw(canvas)
      }
    }

    def updateDrawable(bitmap: Bitmap) {
      val drawable = new BitmapDrawable(activity.getResources, bitmap)
      drawable.setBounds(0, 0, drawable.getIntrinsicWidth, drawable.getIntrinsicHeight)
      drawableHolder = Some(drawable)
    }
  }

  private def getDrawableFromNetwork(source: String) = {

    val urlDrawable = new URLDrawable(activity.getResources, placeHolder)

    ImageCache.getBitmapFromNetwork(activity, source, thumbnailSize).onSuccessInUI { bitmap =>
      urlDrawable.updateDrawable(bitmap)
      adapter.notifyDataSetChanged()
    }

    urlDrawable
  }

  private def getDrawableFromBitmap(bitmap: Bitmap) = {
    val drawable = new BitmapDrawable(activity.getResources, bitmap)
    drawable.setBounds(0, 0, drawable.getIntrinsicWidth, drawable.getIntrinsicHeight)
    drawable
  }

  override def getDrawable(source: String): Drawable = {
    ImageCache.getBitmapFromCache(activity, source) match {
      case Some(bitmap) => getDrawableFromBitmap(bitmap)
      case None => getDrawableFromNetwork(source)
    }
  }

}

