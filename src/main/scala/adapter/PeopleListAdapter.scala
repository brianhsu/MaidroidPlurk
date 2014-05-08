package idv.brianhsu.maidroid.plurk.adapter

import idv.brianhsu.maidroid.plurk._
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

  private type RowItem = Either[String, (Long, String)]

  case class ViewTag(checkbox: CheckBox, title: TextView, var isClique: Boolean) {
    def update(item: RowItem, isSelected: Boolean) {
      item match {
        case Left(cliqueTitle) => title.setText(s"[小圈圈] ${cliqueTitle}")
        case Right((userID, userTitle)) => title.setText(s"${userTitle}")
      }

      checkbox.setChecked(isSelected)
      isClique = item.isLeft
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
    case Left(cliqueName) => selectedCliques.contains(cliqueName)
    case Right((userID, title)) => selectedUsers.contains(userID)
  }

  override def getCount = cliques.size + usersCompletion.size
  override def getItem(position: Int): RowItem = {
    (position < cliques.size) match {
      case true  => Left(cliques(position))
      case false => Right(usersCompletion(position - cliques.size))
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
    val viewTag = ViewTag(checkBox, title, item.isLeft)
    viewTag.update(item, getIsSelected(item))
    view.setTag(viewTag)
    view
  }

  private def setSelected(item: RowItem, isSelected: Boolean) {
    item match {
      case Left(cliqueName) if isSelected    => selectedCliques += cliqueName
      case Left(cliqueName) if !isSelected   => selectedCliques -= cliqueName
      case Right((userID, _)) if isSelected  => selectedUsers += userID
      case Right((userID, _)) if !isSelected => selectedUsers -= userID
    }
  }

  def onItemClick(position: Int, rowView: View) {
    val item = getItem(position)
    setSelected(item, !getIsSelected(item))
    rowView.getTag.asInstanceOf[ViewTag].update(item, getIsSelected(item))
  }
}

