package idv.brianhsu.maidroid.plurk.cache

import idv.brianhsu.maidroid.plurk._
import idv.brianhsu.maidroid.plurk.util._
import idv.brianhsu.maidroid.ui.util.AsyncUI._

import scala.concurrent._

import android.graphics.BitmapFactory
import android.graphics.Bitmap

import org.bone.soplurk.model._

import java.net.URL
import android.content.Context

object ImageCache {

  private var imageCache = new LRUCache[String, Bitmap](5)

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

  private def calculateInSampleSize(originWidth: Int, originHeight: Int, 
                                    requiredWidth: Int, requiredHeight: Int): Int = {
    var inSampleSize = 1

    if (originHeight > requiredHeight && originWidth > requiredWidth) {

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

  def getBitmapFromNetwork(context: Context, url: String, thumbnailSize: Int): Future[Bitmap] = future {
    val (originWidth, originHeight) = calculateOriginSize(url)
    val inSampleSize = calculateInSampleSize(originWidth, originHeight, thumbnailSize, thumbnailSize)
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

    if (imgBitmap != null) {
      imageCache += (url -> imgBitmap)
      future {
        DiskCacheHelper.writeBitmapToCache(context, url, imgBitmap)
      }
    }

    imgBitmap
  }

  def getBitmapFromCache(context: Context, url: String) = {
    imageCache.get(url) orElse DiskCacheHelper.readBitmapFromCache(context, url)
  }

  def clearCache() {
    imageCache = new LRUCache[String, Bitmap](5)
  }
}


