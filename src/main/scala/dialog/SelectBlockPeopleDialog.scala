package idv.brianhsu.maidroid.plurk.dialog

import idv.brianhsu.maidroid.plurk.adapter._

object SelectBlockPeopleDialog {
  trait Listener {
    def onBlockSelected(selectedCliques: Set[String], selectedUsers: Set[(Long, String)])
  }
}

class SelectBlockPeopleDialog (
  defaultSelectedCliques: Set[String], 
  defaultSelectedUsers: Set[Long]) extends SelectPeopleDialog("誰看不到這則發文？", 
                                                              defaultSelectedCliques, 
                                                              defaultSelectedUsers) {

  def this() = this(Set.empty, Set.empty)

  override def onDialogConfirmed(adapter: PeopleListAdapter) {
    val activityCallback = getActivity.asInstanceOf[SelectBlockPeopleDialog.Listener]
    activityCallback.onBlockSelected(adapter.getSelectedCliques, adapter.getSelectedUsersWithTitle)
  }
}
