package idv.brianhsu.maidroid.plurk.adapter

import idv.brianhsu.maidroid.plurk._
import idv.brianhsu.maidroid.plurk.activity._
import idv.brianhsu.maidroid.plurk.fragment._
import idv.brianhsu.maidroid.plurk.util._
import idv.brianhsu.maidroid.plurk.view.PlurkView
import idv.brianhsu.maidroid.ui.util.AsyncUI._
import idv.brianhsu.maidroid.ui.util.CallbackConversions._

import scala.concurrent._

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.View
import android.view.ViewGroup
import android.view.LayoutInflater
import android.widget.BaseAdapter
import android.graphics.BitmapFactory

import org.bone.soplurk.api.PlurkAPI._
import org.bone.soplurk.model._

import java.net.URL

class PlurkAdapter(activity: Activity, isInResponseList: Boolean = false) extends BaseAdapter {
  private implicit val mActivity = activity
  private var plurks: Vector[Plurk] = Vector.empty
  private var users: Map[Long, User] = Map.empty
  private val textViewImageGetter = new PlurkImageGetter(activity, this)

  def getCount = plurks.size
  def getItem(position: Int) = plurks(position)
  def getItemId(position: Int) = plurks(position).plurkID
  
  def firstPlurkShow = plurks.headOption

  def getTimeline = new Timeline(users, plurks.toList)

  def getView(position: Int, convertView: View, parent: ViewGroup): View = {

    val itemView = convertView match {
      case view: PlurkView => view
      case _ => new PlurkView(Some(this), isInResponseList)
    }

    val plurk = plurks(position)
    val owner = users(plurk.ownerID)
    val replurker = for {
      replurkerID <- plurk.replurkInfo.replurkerID
      replurkUser <- users.get(replurkerID)
    } yield replurkUser

    itemView.update(plurk, owner, replurker, textViewImageGetter)
    itemView.setOnCommentCountClickListener { 
      val intent = new Intent(activity, classOf[PlurkResponse])
      PlurkResponse.plurk = plurk
      PlurkResponse.user = owner
      activity.startActivity(intent)
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

    val shouldSkip = (for {
      newTimelineFirst <- timeline.plurks.headOption.map(_.plurkID)
      oldTimelineFirst <- this.plurks.headOption.map(_.plurkID)
    } yield newTimelineFirst == oldTimelineFirst).getOrElse(false)

    if (!shouldSkip) {
      plurks ++= timeline.plurks
      users ++= newUsers
      notifyDataSetChanged
    }
  }

  def addOnlyOnePlurk(user: User, plurk: Plurk) {
    plurks = Vector(plurk)
    users = Map(user.id -> user)
  }

  def lastPlurkDate = plurks.lastOption.map(_.posted)

  def deletePlurk(plurkID: Long) {
    plurks = plurks.filterNot(_.plurkID == plurkID)
    notifyDataSetChanged()
  }

  def updatePlurk(plurkID: Long, newContent: String, newContentRaw: Option[String]) {
    val index = plurks.indexWhere(_.plurkID == plurkID)
    val newPlurk = plurks(index).copy(content = newContent, contentRaw = newContentRaw)
    plurks = plurks.updated(index, newPlurk)
    notifyDataSetChanged()
  }
}

