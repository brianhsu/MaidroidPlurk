package idv.brianhsu.maidroid.plurk.dialog

import idv.brianhsu.maidroid.plurk.R
import idv.brianhsu.maidroid.plurk.adapter._

object SelectLimitedToDialog {
  val titleResID = R.string.dialogSelectPeopleViewableTitle
  trait Listener {
    def onPeopleSelected(selectedCliques: Set[String], selectedUsers: Set[(Long, String)])
  }
}

class SelectLimitedToDialog(defaultSelectedCliques: Set[String], 
                            defaultSelectedUsers: Set[Long]) extends 
        SelectPeopleDialog(SelectLimitedToDialog.titleResID, 
                           defaultSelectedCliques, defaultSelectedUsers) {

  def this() = this(Set.empty, Set.empty)

  override def onDialogConfirmed(adapter: PeopleListAdapter) {
    val activityCallback = getActivity.asInstanceOf[SelectLimitedToDialog.Listener]
    activityCallback.onPeopleSelected(
      adapter.getSelectedCliques, 
      adapter.getSelectedUsersWithTitle
    )
  }
}
