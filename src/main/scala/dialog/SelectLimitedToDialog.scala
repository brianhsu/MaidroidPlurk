package idv.brianhsu.maidroid.plurk.fragment

import idv.brianhsu.maidroid.plurk.adapter._

object SelectLimitedToDialog {
  trait Listener {
    def onPeopleSelected(selectedCliques: Set[String], selectedUsers: Set[(Long, String)])
  }
}

class SelectLimitedToDialog(
  defaultSelectedCliques: Set[String], 
  defaultSelectedUsers: Set[Long]) extends SelectPeopleDialog("誰可以看到這則發文？", 
                                                              defaultSelectedCliques, 
                                                              defaultSelectedUsers) {

  def this() = this(Set.empty, Set.empty)

  override def onDialogConfirmed(adapter: PeopleListAdapter) {
    val activityCallback = getActivity.asInstanceOf[SelectLimitedToDialog.Listener]
    activityCallback.onPeopleSelected(
      adapter.getSelectedCliques, 
      adapter.getSelectedUsersWithTitle
    )
  }
}
