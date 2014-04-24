package idv.brianhsu.maidroid.plurk.adapter

import idv.brianhsu.maidroid.plurk._
import idv.brianhsu.maidroid.plurk.fragment._
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
import android.graphics.BitmapFactory

import org.bone.soplurk.api.PlurkAPI._
import org.bone.soplurk.model._

import java.net.URL

object PlurkAdapter {
  trait Listener {
    def onPlurkSelected(plurk: Plurk, user: User) {}
  }
  type OnSelectedCallback = (Plurk, User) => Unit
}

class PlurkAdapter(activity: Activity, isInResponseList: Boolean = false, callbackHolder: Option[PlurkAdapter.OnSelectedCallback] = None) extends BaseAdapter {
  private implicit val mActivity = activity
  private var plurks: Vector[Plurk] = Vector.empty
  private var users: Map[Long, User] = Map.empty
  private val textViewImageGetter = new PlurkImageGetter(activity, this)

  def getCount = plurks.size
  def getItem(position: Int) = plurks(position)
  def getItemId(position: Int) = plurks(position).plurkID
  
  def firstPlurkShow = plurks.headOption

  def getView(position: Int, convertView: View, parent: ViewGroup): View = {

    val itemView = convertView match {
      case view: PlurkView => view
      case _ => new PlurkView(isInResponseList)
    }

    val plurk = plurks(position)
    val owner = users(plurk.ownerID)

    itemView.update(plurk, owner, textViewImageGetter)

    callbackHolder.foreach { callback =>
      itemView.setOnCommentCountClickListener { callback(plurk, owner) }
    }
    itemView
  }

  def prependTimeline(newPlurks: Timeline) {
    val newUsers = newPlurks.users.filterKeys(userID => !(users.keySet contains userID))
    plurks ++:= newPlurks.plurks
    users ++= newUsers
    notifyDataSetChanged
  }

  def appendTimeline(timeline: Timeline) {
    val newUsers = timeline.users.filterKeys(userID => !(users.keySet contains userID))
    plurks ++= timeline.plurks
    users ++= newUsers
    notifyDataSetChanged
  }

  def addOnlyOnePlurk(user: User, plurk: Plurk) {
    plurks = Vector(plurk)
    users = Map(user.id -> user)
  }

  def lastPlurkDate = plurks.last.posted
}

