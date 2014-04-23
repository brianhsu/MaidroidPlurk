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
import android.graphics.BitmapFactory

import org.bone.soplurk.api.PlurkAPI._
import org.bone.soplurk.model._

import java.net.URL

class PlurkAdapter(activity: Activity) extends BaseAdapter {
  private implicit val mActivity = activity
  private var plurks: Vector[Plurk] = Vector.empty
  private var users: Map[Long, User] = Map.empty
  private val textViewImageGetter = new PlurkImageGetter(activity, this)

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

  def appendTimeline(timeline: Timeline) {
    val newUsers = timeline.users.filterKeys(userID => !(users.keySet contains userID))
    plurks ++= timeline.plurks
    users ++= newUsers
    notifyDataSetChanged
  }

  def lastPlurkDate = plurks.last.posted
}

