package idv.brianhsu.maidroid.plurk.util

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

import java.net.URL

class PlurkImageGetter(activity: Activity, adapter: BaseAdapter, loadingImage: Bitmap) extends Html.ImageGetter {

  private implicit val implicitActivity = activity
  private val imageCache = new LRUCache[String, Bitmap](5)

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

  private def openStream(imageURL: String) = {
    if (imageURL.contains("images.plurk.com/tx_")) {
      val newURL = imageURL.replace("/tx_", "/").replace(".gif", ".jpg")
      val connection = new URL(newURL).openConnection()
      connection.getInputStream()
    } else {
      val connection = new URL(imageURL).openConnection()
      connection.getInputStream()
    }
  }

  private def calculateOriginSize(url: String): (Int, Int) = {
    val imgStream = openStream(url)
    val options = new BitmapFactory.Options
    options.inJustDecodeBounds = true
    BitmapFactory.decodeStream(imgStream, null, options)
    imgStream.close()
    (options.outWidth, options.outHeight)
  }

  private def downloadImageFromNetwork(url: String): Bitmap = {
    val (originWidth, originHeight) = calculateOriginSize(url)
    val inSampleSize = calculateInSampleSize(originWidth, originHeight, 150, 150)
    val imgStream = openStream(url)
    val options = new BitmapFactory.Options
    options.inSampleSize = inSampleSize

    if (originWidth <= 48 && originHeight <= 48) {
      options.inDensity = 160
      options.inScaled = false
      options.inTargetDensity = 160
    }

    val imgBitmap = BitmapFactory.decodeStream(imgStream, null, options)
    imgStream.close()
    imageCache += (url -> imgBitmap)
    imgBitmap
  }

  private def calculateInSampleSize(originWidth: Int, originHeight: Int, 
                                    requiredWidth: Int, requiredHeight: Int): Int = {
    var inSampleSize = 1

    if (originHeight > requiredHeight || originWidth > requiredWidth) {

        val halfHeight = originHeight / 2
        val halfWidth = originWidth / 2

        // Calculate the largest inSampleSize value that is a power of 2 and keeps both
        // height and width larger than the requested height and width.
        while ((halfHeight / inSampleSize) > requiredHeight && 
               (halfWidth / inSampleSize) > requiredWidth) {
            inSampleSize *= 2
        }
    }

    inSampleSize
  }

  private def getDrawableFromNetwork(source: String) = {
    val urlDrawable = new URLDrawable(activity.getResources, loadingImage)
    val bitmapFuture = future { 
      downloadImageFromNetwork(source) 
    }

    bitmapFuture.onSuccessInUI { bitmap =>
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
    val drawableFromMemory = imageCache.get(source).map(getDrawableFromBitmap)
    drawableFromMemory getOrElse (getDrawableFromNetwork(source))
  }

  def getLRUCache = imageCache

}

