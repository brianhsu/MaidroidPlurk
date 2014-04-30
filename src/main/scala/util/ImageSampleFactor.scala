package idv.brianhsu.maidroid.plurk.util

import java.io.InputStream
import android.graphics.BitmapFactory
import java.net.URL
import java.io._


case class ResizeFactor(originWidth: Int, originHeight: Int, sampleSize: Int)

object ImageSampleFactor {

  def apply(url: String, requiredWidth: Int, requiredHeight: Int): ResizeFactor = {
    apply(new URL(url).openConnection.getInputStream, requiredWidth, requiredHeight)
  }

  def apply(file: File, requiredWidth: Int, requiredHeight: Int): ResizeFactor = {
    apply(new FileInputStream(file), requiredWidth, requiredHeight)
  }

  def resizeImageFile(file: File, size: Int) = {
    val ResizeFactor(_, _, sampleSize) = apply(file, size, size)
    val decodeOptions = new BitmapFactory.Options
    decodeOptions.inSampleSize = sampleSize
    BitmapFactory.decodeFile(file.getAbsolutePath, decodeOptions)
  }

  private def apply(imgStream: InputStream , requiredWidth: Int, requiredHeight: Int): ResizeFactor = {
    val (originWidth, originHeight) = calculateOriginSize(imgStream)
    val factor = calculateSampleFactor(originWidth, originHeight, requiredWidth, requiredHeight)
    ResizeFactor(originWidth, originHeight, factor)
  }

  private def calculateSampleFactor (originWidth: Int, originHeight: Int, 
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

  private def calculateOriginSize(imgStream: InputStream): (Int, Int) = {
    val options = new BitmapFactory.Options
    options.inJustDecodeBounds = true
    BitmapFactory.decodeStream(imgStream, null, options)
    imgStream.close()
    (options.outWidth, options.outHeight)
  }



}

