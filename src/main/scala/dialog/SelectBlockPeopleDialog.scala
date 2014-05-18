package idv.brianhsu.maidroid.plurk.dialog

import idv.brianhsu.maidroid.plurk.adapter._
import idv.brianhsu.maidroid.plurk.R

object SelectBlockPeopleDialog {
  val titleResID = R.string.dialogSelectPeopleBlockTitle
  trait Listener {
    def onBlockSelected(selectedCliques: Set[String], selectedUsers: Set[(Long, String)])
  }
}

class SelectBlockPeopleDialog (defaultSelectedCliques: Set[String], 
                               defaultSelectedUsers: Set[Long]) extends 
      SelectPeopleDialog(SelectBlockPeopleDialog.titleResID, 
                         defaultSelectedCliques, defaultSelectedUsers) {

  def this() = this(Set.empty, Set.empty)

  override def onDialogConfirmed(adapter: PeopleListAdapter) {
    val activityCallback = getActivity.asInstanceOf[SelectBlockPeopleDialog.Listener]
    activityCallback.onBlockSelected(adapter.getSelectedCliques, adapter.getSelectedUsersWithTitle)
  }
}
