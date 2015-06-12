package idv.brianhsu.maidroid.plurk.adapter

import idv.brianhsu.maidroid.plurk.view.CliqueListItem
import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView

import org.bone.soplurk.model._

class CliqueListAdapter(activity: Activity, originList: Vector[String]) extends BaseAdapter {

  private var cliqueList = originList
  private def sortedList = cliqueList.sortWith(_ < _)

  override def getCount = cliqueList.size
  override def getItem(position: Int) = sortedList(position)
  override def getItemId(position: Int) = sortedList(position).hashCode

  def addClique(cliqueName: String) {
    if (!cliqueList.contains(cliqueName)) {
      cliqueList = cliqueList :+ cliqueName
    }
    notifyDataSetChanged()
  }

  def removeClique(cliqueName: String) {
    cliqueList = cliqueList.filterNot(_ == cliqueName)
    notifyDataSetChanged()
  }

  def getView(position: Int, convertView: View, parent: ViewGroup): View = {

    val cliqueName = getItem(position)
    val itemView = convertView match {
      case view: CliqueListItem => view
      case _ => new CliqueListItem(activity)
    }

    itemView.update(cliqueName)
    itemView
  }


}



