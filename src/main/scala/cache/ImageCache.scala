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


  def getBitmapFromNetwork(context: Context, url: String, thumbnailSize: Int): Future[Bitmap] = Future {

    val fixedURL = fixPlurkURL(url)
    val ResizeFactor(originWidth, originHeight, factor) = ImageSampleFactor(fixedURL, thumbnailSize, thumbnailSize)
    val options = new BitmapFactory.Options
    options.inSampleSize = factor

    if (originWidth <= 48 || originHeight <= 48) {
      options.inDensity = 160
      options.inScaled = false
      options.inTargetDensity = 160
    }

    val imgStream = new URL(fixedURL).openConnection().getInputStream
    val imgBitmap = BitmapFactory.decodeStream(imgStream, null, options)
    imgStream.close()

    if (imgBitmap != null) {
      imageCache += (fixedURL -> imgBitmap)
      Future {
        DiskCacheHelper.writeBitmapToCache(context, fixedURL, imgBitmap)
      }
    }

    imgBitmap
  }

  private def fixPlurkURL(url: String) = {
    url.contains("images.plurk.com/tx_") match {
      case true  => url.replace("/tx_", "/").replace(".gif", ".jpg")
      case false => url
    }
  }

  def getBitmapFromCache(context: Context, url: String) = {
    val fixedURL = fixPlurkURL(url)
    imageCache.get(fixedURL) orElse DiskCacheHelper.readBitmapFromCache(context, fixedURL)
  }

  def clearCache() {
    imageCache = new LRUCache[String, Bitmap](5)
  }
}


