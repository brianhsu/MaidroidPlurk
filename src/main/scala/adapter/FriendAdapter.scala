package idv.brianhsu.maidroid.plurk.adapter

import idv.brianhsu.maidroid.plurk.view.FriendView
import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import org.bone.soplurk.model._

class FriendListAdapter(activity: Activity, friends: Vector[ExtendedUser]) extends BaseAdapter {

  private var userList = friends
  override def getCount = userList.size
  override def getItem(position: Int) = userList(position)
  override def getItemId(position: Int) = userList(position).basicInfo.id

  def removeUser(userID: Long) {
    userList = userList.filterNot(_.basicInfo.id == userID)
    notifyDataSetChanged()
  }

  def getView(position: Int, convertView: View, parent: ViewGroup): View = {

    val user = getItem(position)
    val itemView = convertView match {
      case view: FriendView => view
      case _ => new FriendView(activity)
    }

    itemView.update(user)
    itemView
  }

}



