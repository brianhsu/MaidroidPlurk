package idv.brianhsu.maidroid.plurk.fragment

import idv.brianhsu.maidroid.plurk._
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

class PostBackstabFragment extends Fragment with PlurkEditor with SelectLimitedToUI
{
  import PostBackstabFragment._

  protected def plurkAPI = PlurkAPIHelper.getPlurkAPI(getActivity)
  protected def contentEditor = Option(getView).map(_.findView(TR.fragmentPostBackstabContent))
  protected def qualifierSpinner = Option(getView).map(_.findView(TR.fragmentPostBackstabQualifier))
  protected def responseTypeSpinner = Option(getView).map(_.findView(TR.fragmentPostBackstabResponseTypeSpinner))

  private def addBlockHolder = Option(getView).map(_.findView(TR.fragmentPostBackstabAddBlock))

  protected def addLimitedToHolder = Option(getView).map(_.findView(TR.fragmentPostBackstabAddLimitedTo))
  protected def limitedToList = Option(getView).map(_.findView(TR.fragmentPostBlackstabLimitedToList))
  protected lazy val inflater = LayoutInflater.from(getActivity)

  override def setSelected(cliques: Set[String], users: Set[(Long, String)]) {
    this.selectedCliques = cliques
    this.selectedUsers = users
    updateLimitedToList()
  }

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, 
                            savedInstanceState: Bundle): View = {
    inflater.inflate(R.layout.fragment_post_backstab, container, false)
  }

  override def onViewCreated(view: View, savedInstanceState: Bundle) {
    for {
      addLimitedToButton <- addLimitedToHolder
      addBlockButton <- addBlockHolder
    } {
      addLimitedToButton.setOnClickListener { view: View => showSelectPeopleDialog() }
    }

    if (savedInstanceState != null) {
      restoreLimitedToUIState(savedInstanceState)
    }

    updateLimitedToList()
  }

  override def onSaveInstanceState(outState: Bundle) {
    saveLimitedToUIState(outState)
  }


}

