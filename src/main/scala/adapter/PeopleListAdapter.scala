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

import android.widget.Filterable
import android.widget.Filter

class PeopleListAdapter(context: Context, 
                        cliques: Vector[String], 
                        usersCompletion: Vector[(Long, String)]) extends BaseAdapter 
                                                                 with Filterable
{

  sealed abstract class RowItem(title: String)

  case class Clique(name: String) extends RowItem(name)
  case class Friend(userID: Long, name: String) extends RowItem(name)

  case class ViewTag(checkbox: CheckBox, title: TextView) {
    def update(item: RowItem, isSelected: Boolean) {
      item match {
        case Clique(cliqueTitle) => 
          val prefix = context.getString(R.string.adapterPeopleListCliquePrefix)
          title.setText(s"${prefix} ${cliqueTitle}")
        case Friend(userID, name) => 
          title.setText(s"${name}")
      }

      checkbox.setChecked(isSelected)
    }
  }

  private lazy val inflater = LayoutInflater.from(context)

  private var selectedCliques: Set[String] = Set.empty
  private var selectedUsers: Set[Long] = Set.empty
  private var listedCliques: Vector[String] = cliques
  private var listedUsers: Vector[(Long, String)] = usersCompletion

  private def getIsSelected(item: RowItem): Boolean = item match {
    case Clique(cliqueName) => selectedCliques.contains(cliqueName)
    case Friend(userID, title) => selectedUsers.contains(userID)
  }

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

  override def getFilter = filter

  private lazy val filter = new Filter() {
    import Filter.FilterResults

    private def getDefaultResults = {
      val results = new FilterResults
      results.count = cliques.size + usersCompletion.size
      results.values = (cliques, usersCompletion)
      results
    }

    private def getSearchResults(constraint: String) = {
      val results = new FilterResults
      val filteredCliques = cliques.filter(_.toLowerCase contains constraint.trim.toLowerCase)
      val filteredUsers = usersCompletion.filter(_._2.toLowerCase contains constraint.trim.toLowerCase)
      results.count = filteredCliques.size + filteredUsers.size
      results.values = (filteredCliques, filteredUsers)
      results
    }

    override def performFiltering(constraint: CharSequence): FilterResults = {

      val isEmptySearchBar = (constraint == null || constraint.toString.trim.isEmpty)
      isEmptySearchBar match {
        case true  => getDefaultResults
        case false => getSearchResults(constraint.toString)
      }
    }

    override def publishResults(constraint: CharSequence, results: FilterResults) {

      val (filteredCliques, filteredUsers) = 
        results.values.asInstanceOf[(Vector[String], Vector[(Long, String)])]

      if (filteredCliques != listedCliques || filteredUsers != listedUsers ) {
        listedCliques = filteredCliques
        listedUsers = filteredUsers
        notifyDataSetChanged()
      }
    }

  }

  override def getCount = listedCliques.size + listedUsers.size
  override def getItem(position: Int): RowItem = {

    (position < listedCliques.size) match {
      case true  => Clique(listedCliques(position))
      case false => 
        val user = listedUsers(position - listedCliques.size)
        Friend(user._1, user._2)
    }
  }

  override def getItemId(position: Int) = getItem(position).hashCode

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

