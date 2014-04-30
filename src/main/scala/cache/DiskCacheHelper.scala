package idv.brianhsu.maidroid.plurk.cache

import idv.brianhsu.maidroid.plurk.util._

import android.os.Environment
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory

import java.io.File
import java.io.FileOutputStream
import java.math.BigInteger
import java.security.MessageDigest

object DiskCacheHelper {

  private def getCacheFolder(context: Context): File = {
    val cacheDir = Environment.getExternalStorageState match {
      case Environment.MEDIA_MOUNTED|
           Environment.MEDIA_MOUNTED_READ_ONLY => context.getExternalCacheDir()
      case _ =>  context.getCacheDir()
    }

    val imageCacheDir = new File(cacheDir, "images")

    if (!imageCacheDir.exists()) {
      imageCacheDir.mkdirs()
    }

    imageCacheDir
  }

  private def getMD5Hash(content: String) = {
    val md = MessageDigest.getInstance("MD5")
    val messageDigest = md.digest(content.getBytes())
    (new BigInteger(1, messageDigest)).toString
  }

  def writeBitmapToCache(context: Context, url: String, bitmap: Bitmap): Option[File] = {
    synchronized {

      try {
        val filename = getMD5Hash(url) + ".jpg"
        val cacheFile = new File(getCacheFolder(context), filename)
        val fileOutputStream = new FileOutputStream(cacheFile)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream)
        fileOutputStream.flush()
        fileOutputStream.close()
        Some(cacheFile)
      } catch {
        case e: Exception => 
          DebugLog(s"write [$url] failed: $e", e)
          None
      }
    }
  }

  def readBitmapFromCache(context: Context, url: String): Option[Bitmap] = {
    val filename = getMD5Hash(url) + ".jpg"
    val cacheFile = new File(getCacheFolder(context), filename).getAbsolutePath
    Option(BitmapFactory.decodeFile(cacheFile))
  }


}

