package idv.brianhsu.maidroid.plurk.fragment

import idv.brianhsu.maidroid.plurk._
import idv.brianhsu.maidroid.plurk.activity.PostPlurkActivity
import idv.brianhsu.maidroid.plurk.dialog._
import idv.brianhsu.maidroid.plurk.util._
import idv.brianhsu.maidroid.plurk.TypedResource._

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.View

class PostPrivateFragment extends Fragment with PlurkEditor
{
  protected lazy val inflater = LayoutInflater.from(getActivity)

  protected def plurkAPI = PlurkAPIHelper.getPlurkAPI(getActivity)
  protected def contentEditorHolder = Option(getView).map(_.findView(TR.fragmentPostPrivateContent))
  protected def qualifierSpinnerHolder = Option(getView).map(_.findView(TR.fragmentPostPrivateQualifier))
  protected def responseTypeSpinnerHolder = Option(getView).map(_.findView(TR.fragmentPostPrivateResponseTypeSpinner))
  protected def charCounterHolder = Option(getView).map(_.findView(TR.fragmentPostPrivateCharCounter))

  protected def limitedButtonHolder = Option(getView).map(_.findView(TR.fragmentPostPrivateLimitedButton))
  protected def limitedListHolder = Option(getView).map(_.findView(TR.fragmentPostPrivateLimitedList))

  private lazy val selectLimitedUI = {
    new SelectUserUI(
      fragment = this, plurkAPI, 
      buttonResID = R.layout.view_people_button, 
      limitedButtonHolder, limitedListHolder
    ) {
      protected def createDialog(selectedCliques: Set[String], selectedUsers: Set[Long]) = {
        new SelectLimitedToDialog(selectedCliques, selectedUsers)
      }
    }
  }

  override def setSelected(cliques: Set[String], users: Set[(Long, String)]) {
    selectLimitedUI.selectedCliques = cliques
    selectLimitedUI.selectedUsers = users
    selectLimitedUI.updateUI()
  }

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, 
                            savedInstanceState: Bundle): View = {
    inflater.inflate(R.layout.fragment_post_private, container, false)
  }

  override def onViewCreated(view: View, savedInstanceState: Bundle) {

    setupCharCounter()

    if (savedInstanceState != null) {
      selectLimitedUI.restoreUIState(savedInstanceState)
    }

    val extraDataHolder = Option(getActivity.getIntent.getExtras)

    extraDataHolder.foreach { extraData =>
      val userID = extraData.getLong(PostPlurkActivity.PrivatePlurkUserID, -1)
      val userFullName = extraData.getString(PostPlurkActivity.PrivatePlurkFullName)
      val userDisplayName = extraData.getString(PostPlurkActivity.PrivatePlurkDisplayName)
      val selectedPeople = Set((userID, s"$userFullName ($userDisplayName)"))
      setSelected(Set.empty, selectedPeople)
    }

    selectLimitedUI.updateUI()
  }

  override def onSaveInstanceState(outState: Bundle) {
    selectLimitedUI.saveUIState(outState)
  }

  override protected def limitedTo: List[Long] = {
    val selectedUsers = selectLimitedUI.getSelectedUsers
    selectedUsers.isEmpty match {
      case true  => List(0)
      case false => selectedUsers.toList
    }
  }

}

