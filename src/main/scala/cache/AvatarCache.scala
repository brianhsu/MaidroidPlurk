package idv.brianhsu.maidroid.plurk.cache

import idv.brianhsu.maidroid.plurk._
import idv.brianhsu.maidroid.plurk.util._
import idv.brianhsu.maidroid.ui.util.AsyncUI._

import scala.concurrent._

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Bitmap

import org.bone.soplurk.model._

import java.net.URL

object AvatarCache {

  private var avatarCache = new LRUCache[Long, Bitmap](50)

  def clearCache() {
    avatarCache = new LRUCache[Long, Bitmap](50)
  }

  def getAvatarBitmapFromCache(context: Context, user: User) = {
    avatarCache.get(user.id) orElse 
    DiskCacheHelper.readBitmapFromCache(context, user.bigAvatar)
  }

  def getAvatarBitmapFromNetwork(context: Context, user: User): Future[(Long, Bitmap)] = {

    val avatarURL = user.bigAvatar
    val avatarBitmapFuture = Future {
      val avatarURLStream = new URL(avatarURL).openStream()
      val avatarBitmap = BitmapFactory.decodeStream(avatarURLStream)
      avatarURLStream.close()
      if (avatarBitmap == null) {
        val message = context.getString(R.string.cannotGetAvatar).format(avatarURL)
        throw new Exception(message)
      }
      (user.id, avatarBitmap)
    }

    avatarBitmapFuture.foreach { 
      case (userID, bitmap) => 
        avatarCache += (user.id -> bitmap) 
        Future {
          DiskCacheHelper.writeBitmapToCache(context, avatarURL, bitmap)
        }
    }

    avatarBitmapFuture
  }

}

