package idv.brianhsu.maidroid.plurk.adapter

import idv.brianhsu.maidroid.plurk._
import idv.brianhsu.maidroid.plurk.util._
import idv.brianhsu.maidroid.plurk.view.PlurkView
import idv.brianhsu.maidroid.ui.util.AsyncUI._
import idv.brianhsu.maidroid.ui.util.CallbackConversions._

import scala.concurrent._

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.view.LayoutInflater
import android.widget.BaseAdapter
import android.widget.ImageView
import android.graphics.BitmapFactory
import android.graphics.Bitmap

import org.bone.soplurk.api.PlurkAPI._
import org.bone.soplurk.model._

import java.net.URL

class PlurkAdapter(activity: Activity) extends BaseAdapter {
  private implicit val mActivity = activity
  private var plurks: Vector[Plurk] = Vector.empty
  private var users: Map[Long, User] = Map.empty
  private val avatarCache = new LRUCache[Long, Bitmap](50)
  private val textViewImageGetter = new PlurkImageGetter(activity, this, loadingImage)
  private val loadingImage = BitmapFactory.decodeResource(activity.getResources, R.drawable.default_avatar)

  def getCount = plurks.size
  def getItem(position: Int) = plurks(position)
  def getItemId(position: Int) = plurks(position).plurkID
  
  def getView(position: Int, convertView: View, parent: ViewGroup): View = {

    val itemView = convertView match {
      case view: PlurkView => view
      case _ => new PlurkView
    }

    val plurk = plurks(position)
    val onwer = users(plurk.ownerID)
    itemView.update(plurk, onwer, textViewImageGetter)
  }

  def loadAvatarBitmapFromNetwork(user: User, imageView: ImageView) {

    val avatarBitmapFuture = future {
      val avatarURLStream = new URL(user.bigAvatar).openStream()
      val avatarBitmap = BitmapFactory.decodeStream(avatarURLStream)
      avatarURLStream.close()
      avatarBitmap
    }

    avatarBitmapFuture.onSuccessInUI { avatarBitmap =>
      avatarCache += (user.id -> avatarBitmap)
      imageView.setImageBitmap(avatarBitmap)
      notifyDataSetChanged()
    }

    avatarBitmapFuture.onFailureInUI { case e: Exception =>
      DebugLog(s"====> [failed] avatar url of ${user.displayName}: ${user.bigAvatar}")
    }

  }

  def loadAvatar(user: User, imageView: ImageView) {
    avatarCache.get(user.id) match {
      case Some(bitmap) => imageView.setImageBitmap(bitmap)
      case None => loadAvatarBitmapFromNetwork(user, imageView)
    }
  }

  def appendTimeline(timeline: Timeline) {
    val newUsers = timeline.users.filterKeys(userID => !(users.keySet contains userID))
    plurks ++= timeline.plurks
    users ++= newUsers
    notifyDataSetChanged
  }

  def lastPlurkDate = plurks.last.posted
}

