package idv.brianhsu.maidroid.plurk.fragment

import idv.brianhsu.maidroid.plurk._
import idv.brianhsu.maidroid.plurk.util._
import idv.brianhsu.maidroid.plurk.TypedResource._
import idv.brianhsu.maidroid.ui.util.CallbackConversions._

import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.View
import android.os.Bundle

import android.support.v4.app.Fragment
import android.widget.PopupWindow

class PostPrivateFragment extends Fragment with PlurkEditor {

  private var selectedUsers: Set[(Long, String)] = Set.empty
  private var selectedCliques: Set[String] = Set.empty

  protected def plurkAPI = PlurkAPIHelper.getPlurkAPI(getActivity)
  protected def contentEditor = Option(getView).map(_.findView(TR.fragmentPostPrivateContent))
  protected def qualifierSpinner = Option(getView).map(_.findView(TR.fragmentPostPrivateQualifier))
  protected def responseTypeSpinner = Option(getView).map(_.findView(TR.fragmentPostPrivateResponseTypeSpinner))
  protected def addLimitedToHolder = Option(getView).map(_.findView(TR.fragmentPostPrivateAddLimitedTo))

  override def setSelectedCliques(cliques: Set[String]) {
    DebugLog("====> setSelectedCliques:" + cliques)
    this.selectedCliques = cliques
  }

  override def setSelectedUsers(users: Set[(Long, String)]) {
    DebugLog("====> setSelectedUsers:" + users)
    this.selectedUsers = users
  }

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, 
                            savedInstanceState: Bundle): View = {
    inflater.inflate(R.layout.fragment_post_private, container, false)
  }

  override def onViewCreated(view: View, savedInstanceState: Bundle) {
    addLimitedToHolder.foreach(_.setOnClickListener({ view: View =>
      val dialogFragment = new AddLimitedToDialogFragment(
        selectedCliques, 
        selectedUsers.map(_._1)
      )
      dialogFragment.show(getActivity.getSupportFragmentManager(), "tag")
    }))
  }

}

