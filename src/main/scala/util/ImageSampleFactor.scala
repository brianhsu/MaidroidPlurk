package idv.brianhsu.maidroid.plurk.util

import java.io.InputStream
import android.graphics.BitmapFactory
import java.net.URL
import java.io._
import android.media.ExifInterface
import android.graphics.Matrix
import android.graphics.Bitmap


case class ResizeFactor(originWidth: Int, originHeight: Int, sampleSize: Int)

object ImageSampleFactor {

  def apply(url: String, requiredWidth: Int, requiredHeight: Int): ResizeFactor = {
    apply(new URL(url).openConnection.getInputStream, requiredWidth, requiredHeight)
  }

  def apply(file: File, requiredWidth: Int, requiredHeight: Int): ResizeFactor = {
    apply(new FileInputStream(file), requiredWidth, requiredHeight)
  }

  def resizeImageFile(imageFile: File, size: Int, shouldRotate: Boolean = false) = {
    val ResizeFactor(_, _, sampleSize) = apply(imageFile, size, size)
    val decodeOptions = new BitmapFactory.Options
    decodeOptions.inSampleSize = sampleSize
    val bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath, decodeOptions)

    if (shouldRotate) {
      rotateBitmap(imageFile, bitmap)
    } else {
      bitmap
    }
  }

  private def rotateBitmap(imageFile: File, bitmap: Bitmap) = {
    val matrix = new Matrix
    val exif = new ExifInterface(imageFile.getAbsolutePath)
    val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1)

    orientation match {
      case ExifInterface.ORIENTATION_FLIP_HORIZONTAL =>
        matrix.setScale(-1, 1)
        createNewBitmap(bitmap, matrix)
      case ExifInterface.ORIENTATION_ROTATE_180 =>
        matrix.setRotate(180)
        createNewBitmap(bitmap, matrix)
      case ExifInterface.ORIENTATION_FLIP_VERTICAL =>
        matrix.setRotate(180)
        matrix.setScale(-1, 1)
        createNewBitmap(bitmap, matrix)
      case ExifInterface.ORIENTATION_TRANSPOSE =>
        matrix.setRotate(90)
        matrix.setScale(-1, 1)
        createNewBitmap(bitmap, matrix)
      case ExifInterface.ORIENTATION_ROTATE_90 =>
        matrix.setRotate(90)
        createNewBitmap(bitmap, matrix)
      case ExifInterface.ORIENTATION_TRANSVERSE =>
        matrix.setRotate(-90)
        matrix.setScale(-1, 1)
        createNewBitmap(bitmap, matrix)
      case ExifInterface.ORIENTATION_ROTATE_270  =>
        matrix.setRotate(-90)
        createNewBitmap(bitmap, matrix)
      case _ => 
        bitmap
    }
  }

  private def createNewBitmap(bitmap: Bitmap, matrix: Matrix) ={
    try {
      val newBitmap = Bitmap.createBitmap(
        bitmap, 0, 0, 
        bitmap.getWidth(), 
        bitmap.getHeight(), 
        matrix, true
      )
      newBitmap
    } catch {
      case e: OutOfMemoryError => bitmap
    }
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

