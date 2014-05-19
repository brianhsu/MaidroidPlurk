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
import java.io.InputStream

object DiskCacheHelper {

  private val noop: Long => Any = (x: Long) => {}

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

  def writeUriToCache(context: Context, uri: android.net.Uri, 
                      callback: Long => Any = noop): Option[File] = {
    try {
      val resolver = context.getContentResolver
      val filename = java.util.UUID.randomUUID.toString + ".tmp"
      val cacheFile = new File(getCacheFolder(context), filename)
      val fileOutputStream = new FileOutputStream(cacheFile)
      val stream = context.getContentResolver.openInputStream(uri)

      val buffer = new Array[Byte](1024 * 200)
      var length = stream.read(buffer)
      var totalBytes: Long = 0

      while (length != -1) {
        //DebugLog("====> writing length:" + length)
        fileOutputStream.write(buffer, 0, length)
        totalBytes += length
        callback(totalBytes)
        length = stream.read(buffer)
      }

      fileOutputStream.flush()
      fileOutputStream.close()
      Some(cacheFile)
    } catch {
      case e: Exception => 
        DebugLog(s"====> cannot write $uri to file", e)
        None
    }
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

  private def readBitmap(cacheFile: File) = {
    val ResizeFactor(originWidth, originHeight, factor) = ImageSampleFactor(cacheFile, 1000, 1000)
    val options = new BitmapFactory.Options

    if (originWidth <= 48 || originHeight <= 48) {
      options.inDensity = 160
      options.inScaled = false
      options.inTargetDensity = 160
    }

    Option(BitmapFactory.decodeFile(cacheFile.getAbsolutePath, options))
  }

  def readBitmapFromCache(context: Context, url: String): Option[Bitmap] = {
    val filename = getMD5Hash(url) + ".jpg"
    val cacheFile = new File(getCacheFolder(context), filename)

    cacheFile.exists match {
      case true  => readBitmap(cacheFile)
      case false => None
    }
  }


}

