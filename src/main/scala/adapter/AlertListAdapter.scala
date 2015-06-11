package idv.brianhsu.maidroid.plurk.adapter

import idv.brianhsu.maidroid.plurk.view.AlertListItem
import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import org.bone.soplurk.model._

class AlertListAdapter(activity: Activity, originList: Vector[Alert]) extends BaseAdapter {

  private var alertList = originList

  override def getCount = alertList.size
  override def getItem(position: Int) = alertList(position)
  override def getItemId(position: Int) = alertList(position).hashCode

  def removeUser(alert: Alert) {
    alertList = alertList.filterNot(_ == alert)
    notifyDataSetChanged()
  }

  def getView(position: Int, convertView: View, parent: ViewGroup): View = {

    val alert = getItem(position)
    val itemView = convertView match {
      case view: AlertListItem => view
      case _ => new AlertListItem(activity)
    }

    itemView.update(alert)
    itemView
  }


}



