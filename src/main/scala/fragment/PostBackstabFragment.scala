package idv.brianhsu.maidroid.plurk.fragment

import idv.brianhsu.maidroid.plurk._
import idv.brianhsu.maidroid.plurk.dialog._
import idv.brianhsu.maidroid.plurk.util._
import idv.brianhsu.maidroid.plurk.TypedResource._
import idv.brianhsu.maidroid.ui.util.CallbackConversions._
import idv.brianhsu.maidroid.ui.util.AsyncUI._

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams._
import android.view.View
import android.widget.PopupWindow
import android.widget.Button
import android.widget.LinearLayout

object PostBackstabFragment 
{
  private val SelectedCliquesBundle = "idv.brianhsu.maidroid.plurk.SELECTED_CLIQUES" 
  private val SelectedUsersNameBundle = "idv.brianhsu.maidroid.plurk.SELECTED_USERS_NAME" 
  private val SelectedUsersIDBundle = "idv.brianhsu.maidroid.plurk.SELECTED_USERS_ID" 

}

class PostBackstabFragment extends Fragment with PlurkEditor
{
  import PostBackstabFragment._

  protected lazy val inflater = LayoutInflater.from(getActivity)

  protected def plurkAPI = PlurkAPIHelper.getPlurkAPI(getActivity)
  protected def contentEditorHolder = Option(getView).map(_.findView(TR.fragmentPostBackstabContent))
  protected def qualifierSpinnerHolder = Option(getView).map(_.findView(TR.fragmentPostBackstabQualifier))
  protected def responseTypeSpinnerHolder = Option(getView).map(_.findView(TR.fragmentPostBackstabResponseTypeSpinner))
  protected def charCounterHolder = Option(getView).map(_.findView(TR.fragmentPostBackstabCharCounter))

  protected def limitedButtonHolder = Option(getView).map(_.findView(TR.fragmentPostBackstabLimitedButton))
  protected def limitedListHolder = Option(getView).map(_.findView(TR.fragmentPostBlackstabLimitedList))
  protected def blockButtonHolder = Option(getView).map(_.findView(TR.fragmentPostBackstabBlockButton))
  protected def blockListHolder = Option(getView).map(_.findView(TR.fragmentPostBackstabBlockList))

  private lazy val selectLimitedUI = {
    new SelectUserUI(
      fragment = this, plurkAPI, 
      buttonResID = R.layout.view_people_button,
      limitedButtonHolder, limitedListHolder) 
    {
      protected def createDialog(selectedCliques: Set[String], selectedUsers: Set[Long]) = {
        new SelectLimitedToDialog(selectedCliques, selectedUsers)
      }
    }
  }

  private lazy val selectBlockUI = {
    new SelectUserUI(
       fragment = this, plurkAPI, 
       buttonResID = R.layout.view_block_button,
       blockButtonHolder, 
       blockListHolder
    ) {
      protected def createDialog(selectedCliques: Set[String], selectedUsers: Set[Long]) = {
        new SelectBlockPeopleDialog(selectedCliques, selectedUsers)
      }

      override protected def updateUISelectedNone() {
        getActivity.runOnUIThread { 
          blockListHolder.foreach { viewGroup => viewGroup.removeAllViews() }
        }
      }
    }
  }

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, 
                            savedInstanceState: Bundle): View = {
    inflater.inflate(R.layout.fragment_post_backstab, container, false)
  }

  override def setSelected(cliques: Set[String], users: Set[(Long, String)]) {
    selectLimitedUI.selectedCliques = cliques
    selectLimitedUI.selectedUsers = users
    selectLimitedUI.updateUI()
  }

  override def setBlocked(cliques: Set[String], users: Set[(Long, String)]) {
    selectBlockUI.selectedCliques = cliques
    selectBlockUI.selectedUsers = users
    selectBlockUI.updateUI()
  }

  override def onViewCreated(view: View, savedInstanceState: Bundle) {
    setupCharCounter()

    if (savedInstanceState != null) {
      selectLimitedUI.restoreUIState(savedInstanceState)
      selectBlockUI.restoreUIState(savedInstanceState)
    }

    selectLimitedUI.updateUI()
    selectBlockUI.updateUI()
  }

  override def onSaveInstanceState(outState: Bundle) {
    selectLimitedUI.saveUIState(outState)
    selectBlockUI.saveUIState(outState)
  }

  override def limitedTo: List[Long] = {

    val blockUsers = selectBlockUI.getSelectedUsers
    val limitedToUser = selectLimitedUI.getSelectedUsers match {
      case users if users.isEmpty => plurkAPI.FriendsFans.getCompletion.get.keys.toSet
      case users => users
    }

    (limitedToUser -- blockUsers).toList
  }

}

