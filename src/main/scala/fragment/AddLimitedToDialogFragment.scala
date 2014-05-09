package idv.brianhsu.maidroid.plurk.fragment

import idv.brianhsu.maidroid.plurk.adapter._

object AddLimitedToDialogFragment {
  trait Listener {
    def onDialogConfirmed(selectedCliques: Set[String], selectedUsers: Set[(Long, String)])
  }
}

class AddLimitedToDialogFragment(
  defaultSelectedCliques: Set[String], 
  defaultSelectedUsers: Set[Long]) extends SelectPeopleDialogFragment("誰可以看到這則發文？", 
                                                                      defaultSelectedCliques, 
                                                                      defaultSelectedUsers) {

  def this() = this(Set.empty, Set.empty)

  override def onDialogConfirmed(adapter: PeopleListAdapter) {
    val activityCallback = getActivity.asInstanceOf[AddLimitedToDialogFragment.Listener]
    activityCallback.onDialogConfirmed(adapter.getSelectedCliques, adapter.getSelectedUsersWithTitle)
  }
}
