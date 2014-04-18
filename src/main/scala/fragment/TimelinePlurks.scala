package idv.brianhsu.maidroid.plurk.fragment

import idv.brianhsu.maidroid.plurk._
import idv.brianhsu.maidroid.plurk.util._
import idv.brianhsu.maidroid.plurk.TypedResource._
import idv.brianhsu.maidroid.ui.util.AsyncUI._

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.net.Uri
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.View
import android.webkit.WebViewClient
import android.webkit.WebView
import android.view.LayoutInflater
import android.widget.ImageView

import org.bone.soplurk.api._
import org.bone.soplurk.api.PlurkAPI.Timeline
import org.bone.soplurk.model._
import scala.concurrent._
import java.net.URL
import scala.util.Try
import android.text.Html

object TimelinePlurksFragment {
  trait Listener {
    def onGetPlurkAPI: PlurkAPI
  }
}

import android.widget.BaseAdapter

class ViewTag(var userID: Long, itemView: View) {
  lazy val avatar = itemView.findView(TR.itemPlurkAvatar)
  lazy val content = itemView.findView(TR.itemPlurkText)
}

class PlurkAdapter(context: Activity) extends BaseAdapter {
  private implicit val activity = context
  private var plurks: Vector[Plurk] = Vector.empty
  private var users: Map[Long, User] = Map.empty
  private var avatarCache: Map[Long, Bitmap] = Map.empty

  private val layoutInflater = LayoutInflater.from(context)

  def getCount = plurks.size
  def getItem(position: Int) = plurks(position)
  def getItemId(position: Int) = plurks(position).plurkID
  
  def createNewView(plurk: Plurk, owner: User, parent: ViewGroup): View = {

    val view = layoutInflater.inflate(R.layout.item_plurk, parent, false)
    val viewTag = new ViewTag(owner.id, view)

    viewTag.content.setText(Html.fromHtml(plurk.content))
    loadAvatar(owner, viewTag.avatar)
    view.setTag(viewTag)
    view
  }

  def getView(position: Int, convertView: View, parent: ViewGroup): View = {

    val plurk = plurks(position)
    val owner = users(plurk.ownerID)

    convertView match {
      case null => createNewView(plurk, owner, parent)
      case view => 
        val viewTag = view.getTag.asInstanceOf[ViewTag]
        viewTag.content.setText(plurk.content)

        if (viewTag.userID != owner.id) {
          loadAvatar(owner, viewTag.avatar)
          viewTag.userID = owner.id
        }
        view
    }
  }

  def loadAvatarBitmapFromNetwork(user: User): Try[Bitmap] = Try {
    val avatarURLStream = new URL(user.bigAvatar).openStream()
    val avatarBitmap = BitmapFactory.decodeStream(avatarURLStream)
    avatarCache += (user.id -> avatarBitmap)
    avatarBitmap
  }

  def loadAvatar(user: User, imageView: ImageView) {
    val avatarBitmapFuture = future { loadAvatarBitmapFromNetwork(user).get }
    avatarBitmapFuture.onSuccessInUI { bitmap =>
      DebugLog(s"====> [ok] avatar url of ${user.displayName}: ${user.bigAvatar}")
      imageView.setImageBitmap(bitmap)
    }

    avatarBitmapFuture.onFailureInUI { case e: Exception =>
      DebugLog(s"====> [failed] avatar url of ${user.displayName}: ${user.bigAvatar}")
    }

  }

  def appendTimeline(timeline: Timeline) {
    val newUsers = timeline.users.filterKeys(userID => !(users.keySet contains userID))
    plurks ++= timeline.plurks
    users ++= newUsers
    notifyDataSetChanged
  }
}

class TimelinePlurksFragment extends Fragment {

  private implicit def activity = getActivity
  private var activityCallback: TimelinePlurksFragment.Listener = _
  private def plurkAPI = activityCallback.onGetPlurkAPI

  private lazy val listView = getView.findView(TR.fragmentTimelinePlurksListView)
  private lazy val adapter = new PlurkAdapter(activity)

  override def onAttach(activity: Activity) {
    super.onAttach(activity)
    try {
      activityCallback = activity.asInstanceOf[TimelinePlurksFragment.Listener]
    } catch {
      case e: ClassCastException => 
        throw new ClassCastException(s"${activity} must mixed with TimelinePlurksFragment.Listener")
    }
  }

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, 
                            savedInstanceState: Bundle): View = {
    inflater.inflate(R.layout.fragment_timeline_plurks, container, false)
  }

  override def onViewCreated(view: View, savedInstanceState: Bundle) {
    listView.setEmptyView(view.findView(TR.fragmentTimelinePlurksEmptyNotice))
    listView.setAdapter(adapter)
    setupPlurkList()
  }

  private def setupPlurkList() {

    val plurksFuture = future { plurkAPI.Timeline.getPlurks().get }

    plurksFuture.onSuccessInUI { timeline => 
      DebugLog("==> plurksFuture.onSuccessInUI")
      adapter.appendTimeline(timeline)
    }

    plurksFuture.onFailureInUI { case e: Exception =>
      DebugLog("==> plurksFuture.onSuccessInUI")
      DebugLog("==> exception:" + e)
    }
  }

}

