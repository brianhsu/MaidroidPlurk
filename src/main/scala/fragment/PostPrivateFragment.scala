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

class PostPrivateFragment extends Fragment with PlurkEditor {

  private var selectedUsers: Set[(Long, String)] = Set.empty
  private var selectedCliques: Set[String] = Set.empty

  protected def plurkAPI = PlurkAPIHelper.getPlurkAPI(getActivity)
  protected def contentEditor = Option(getView).map(_.findView(TR.fragmentPostPrivateContent))
  protected def qualifierSpinner = Option(getView).map(_.findView(TR.fragmentPostPrivateQualifier))
  protected def responseTypeSpinner = Option(getView).map(_.findView(TR.fragmentPostPrivateResponseTypeSpinner))

  private def addLimitedToHolder = Option(getView).map(_.findView(TR.fragmentPostPrivateAddLimitedTo))
  private def limitedToList = Option(getView).map(_.findView(TR.fragmentPostPrivateLimitedToList))
  private lazy val inflater = LayoutInflater.from(getActivity)

  private def createButton(title: String) = {
    val button = inflater.inflate(R.layout.view_people_button, null).asInstanceOf[Button]
    button.setText(title)
    button
  }

  private def updateLimitedToList() {
    if (selectedCliques.isEmpty && selectedUsers.isEmpty) {
      updateLimitedToListSelectedAll()
    } else {
      updateLimitedToListSelectedSome()
    }
  }

  private def updateLimitedToListSelectedAll() {

    getActivity.runOnUIThread { 
      limitedToList.foreach { viewGroup =>
        viewGroup.removeAllViews()
        val button = createButton("[所有好友]")
        val layoutParams = new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
        layoutParams.setMargins(10, 10, 10, 10)
        viewGroup.addView(button, layoutParams)
        button.setOnClickListener { button: View =>
          showSelectPeopleDialog()
        }
      }
    }
 
  }

  private def updateLimitedToListSelectedSome() {
    getActivity.runOnUIThread { 
      limitedToList.foreach { viewGroup =>

        viewGroup.removeAllViews()
        val sortedClique = selectedCliques.filterNot(_ == "[所有好友]").toVector.sortWith(_ < _)
        val sortedUsers = selectedUsers.toVector.sortWith(_._2 < _._2)

        for (clique <- sortedClique) {

          val title = s"[小圈圈] ${clique}"
          val button = createButton(title)
          val layoutParams = new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
          layoutParams.setMargins(10, 10, 10, 10)
          viewGroup.addView(button, layoutParams)
          button.setOnClickListener { button: View =>
            selectedCliques -= clique

            if (selectedCliques.isEmpty && selectedUsers.isEmpty) {
              updateLimitedToListSelectedAll()
            } else {
              viewGroup.removeView(button)
            }

          }
        }

        for (user <- sortedUsers) {
          val button = createButton(user._2)
          val layoutParams = new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
          layoutParams.setMargins(10, 10, 10, 10)
          viewGroup.addView(button, layoutParams)
          button.setOnClickListener { button: View =>
            selectedUsers -= user

            if (selectedCliques.isEmpty && selectedUsers.isEmpty) {
              updateLimitedToListSelectedAll()
            } else {
              viewGroup.removeView(button)
            }

          }
        }
      }
    }
  }

  override def setSelected(cliques: Set[String], users: Set[(Long, String)]) {
    this.selectedCliques = cliques
    this.selectedUsers = users
    updateLimitedToList()
  }

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, 
                            savedInstanceState: Bundle): View = {
    inflater.inflate(R.layout.fragment_post_private, container, false)
  }

  override def onViewCreated(view: View, savedInstanceState: Bundle) {
    addLimitedToHolder.foreach { button =>
      button.setOnClickListener { view: View => showSelectPeopleDialog() }
    }
    updateLimitedToList()
  }

  private def showSelectPeopleDialog() {

    val dialogFragment = new AddLimitedToDialogFragment(
      selectedCliques, 
      selectedUsers.map(_._1)
    )

    dialogFragment.show(getActivity.getSupportFragmentManager(), "selectedPeople")
  }

}

