package idv.brianhsu.maidroid.plurk.adapter

import idv.brianhsu.maidroid.plurk.view.UserListItem
import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Filterable
import android.widget.Filter

import org.bone.soplurk.model._

class UserListAdapter(activity: Activity, originList: Vector[User]) extends BaseAdapter with Filterable {

  private var userList = originList
  private var filteredUserList = originList

  override def getCount = filteredUserList.size
  override def getItem(position: Int) = filteredUserList(position)
  override def getItemId(position: Int) = filteredUserList(position).id
  override def getFilter = filter

  def removeUser(userID: Long) {
    userList = userList.filterNot(_.id == userID)
    filteredUserList = userList
    notifyDataSetChanged()
  }

  def getView(position: Int, convertView: View, parent: ViewGroup): View = {

    val user = getItem(position)
    val itemView = convertView match {
      case view: UserListItem => view
      case _ => new UserListItem(activity)
    }

    itemView.update(user)
    itemView
  }

  val filter = new Filter {
    import Filter.FilterResults

    private def getDefaultResults = {
      val results = new FilterResults
      results.count = userList.size
      results.values = userList
      results
    }

    private def getSearchResults(constraint: String) = {
      val results = new FilterResults
      val filteredUsers = userList.filter { user => 
        user.nickname.toLowerCase.contains(constraint.toLowerCase) ||
        user.fullName.toLowerCase.contains(constraint.toLowerCase) ||
        user.displayName.map(_.toLowerCase contains constraint.toLowerCase).getOrElse(false)
      }
      results.count = filteredUsers.size
      results.values = filteredUsers
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
      val filteredUserList = results.values.asInstanceOf[Vector[User]]
      UserListAdapter.this.filteredUserList = filteredUserList
      notifyDataSetChanged()
    }

  }

}



