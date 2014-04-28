package idv.brianhsu.maidroid.plurk.cache

import idv.brianhsu.maidroid.plurk._
import idv.brianhsu.maidroid.plurk.util._
import idv.brianhsu.maidroid.ui.util.AsyncUI._

import scala.concurrent._

import android.graphics.BitmapFactory
import android.graphics.Bitmap

import org.bone.soplurk.model._

import java.net.URL

object AvatarCache {

  private var avatarCache = new LRUCache[Long, Bitmap](50)

  def clearCache() {
    avatarCache = new LRUCache[Long, Bitmap](50)
  }

  def getAvatarBitmap(user: User) = avatarCache.get(user.id)

  def getAvatarBitmapFromNetwork(user: User): Future[(Long, Bitmap)] = {

    val avatarBitmapFuture = future {
      val avatarURLStream = new URL(user.bigAvatar).openStream()
      val avatarBitmap = BitmapFactory.decodeStream(avatarURLStream)
      avatarURLStream.close()
      (user.id, avatarBitmap)
    }

    avatarBitmapFuture.foreach { 
      case (userID, bitmap) => avatarCache += (user.id -> bitmap) 
    }
    avatarBitmapFuture
  }

}

