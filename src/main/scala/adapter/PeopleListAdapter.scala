package idv.brianhsu.maidroid.plurk.adapter

import idv.brianhsu.maidroid.plurk._
import idv.brianhsu.maidroid.plurk.util._
import idv.brianhsu.maidroid.plurk.TypedResource._

import android.widget.BaseAdapter
import android.widget.TextView
import android.widget.CheckBox
import android.view.View
import android.view.ViewGroup
import android.view.LayoutInflater
import android.widget.AdapterView
import android.content.Context

class PeopleListAdapter(context: Context, 
                        cliques: Vector[String], 
                        usersCompletion: Vector[(Long, String)]) extends BaseAdapter 
{

  sealed abstract class RowItem(title: String)

  case object AllFriends extends RowItem("[所有好友]")
  case class Clique(name: String) extends RowItem(name)
  case class Friend(userID: Long, name: String) extends RowItem(name)

  case class ViewTag(checkbox: CheckBox, title: TextView) {
    def update(item: RowItem, isSelected: Boolean) {
      item match {
        case AllFriends => title.setText(s"[所有好友]")
        case Clique(cliqueTitle) => title.setText(s"[小圈圈] ${cliqueTitle}")
        case Friend(userID, name) => title.setText(s"${name}")
      }

      checkbox.setChecked(isSelected)
    }
  }

  private lazy val inflater = LayoutInflater.from(context)

  private var selectedCliques: Set[String] = Set.empty
  private var selectedUsers: Set[Long] = Set.empty

  def setSelectedCliques(cliques: Set[String]) = {
    selectedCliques = cliques
  }

  def setSelectedUsers(users: Set[Long]) = {
    selectedUsers = users
  }

  def getSelectedCliques = selectedCliques
  def getSelectedUsers = selectedUsers
  def getSelectedUsersWithTitle = {
    selectedUsers.map { userID => usersCompletion.filter(_._1 == userID).head }
  }

  private def getIsSelected(item: RowItem): Boolean = item match {
    case AllFriends => selectedCliques.contains("[所有好友]")
    case Clique(cliqueName) => selectedCliques.contains(cliqueName)
    case Friend(userID, title) => selectedUsers.contains(userID)
  }

  override def getCount = cliques.size + usersCompletion.size + 1
  override def getItem(position: Int): RowItem = {

    DebugLog("====> position:" + position)
    if (position <= 0) {
      AllFriends
    } else {

      (position < (cliques.size + 1)) match {
        case true  => Clique(cliques(position-1))
        case false => 
          val user = usersCompletion(position - cliques.size - 1)
          Friend(user._1, user._2)
      }
    }
  }

  override def getItemId(position: Int) = position

  override def getView(position: Int, convertView: View, parent: ViewGroup): View = {
    val item = getItem(position)
    val view = convertView match {
      case oldView: View => 
        oldView.getTag.asInstanceOf[ViewTag].update(item, getIsSelected(item))
        oldView
      case _ => createNewRowView(item, parent)
    }

    view
  }

  private def createNewRowView(item: RowItem, parent: ViewGroup): View = {

    val view = inflater.inflate(R.layout.item_people, parent, false)
    val title = view.findView(TR.itemPeopleTitle)
    val checkBox = view.findView(TR.itemPeopleCheckBox)
    val viewTag = ViewTag(checkBox, title)
    viewTag.update(item, getIsSelected(item))
    view.setTag(viewTag)
    view
  }

  private def setSelected(item: RowItem, isSelected: Boolean) {
    item match {
      case AllFriends if isSelected => selectedCliques += "[所有好友]"
      case AllFriends if !isSelected => selectedCliques -= "[所有好友]"
      case Clique(cliqueName) if isSelected    => selectedCliques += cliqueName
      case Clique(cliqueName) if !isSelected   => selectedCliques -= cliqueName
      case Friend(userID, _) if isSelected  => selectedUsers += userID
      case Friend(userID, _) if !isSelected => selectedUsers -= userID
    }
  }

  def onItemClick(position: Int, rowView: View) {
    val item = getItem(position)
    setSelected(item, !getIsSelected(item))
    rowView.getTag.asInstanceOf[ViewTag].update(item, getIsSelected(item))
  }
}

