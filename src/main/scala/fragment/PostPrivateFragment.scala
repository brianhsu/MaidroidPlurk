package idv.brianhsu.maidroid.plurk.fragment

import idv.brianhsu.maidroid.plurk._
import idv.brianhsu.maidroid.plurk.util._
import idv.brianhsu.maidroid.plurk.TypedResource._
import idv.brianhsu.maidroid.ui.util.CallbackConversions._
import idv.brianhsu.maidroid.ui.util.AsyncUI._

import org.bone.soplurk.api._

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams._
import android.view.View
import android.widget.PopupWindow
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout

object SelectLimitedToUI
{
  private val SelectedCliquesBundle = "idv.brianhsu.maidroid.plurk.SELECTED_CLIQUES" 
  private val SelectedUsersNameBundle = "idv.brianhsu.maidroid.plurk.SELECTED_USERS_NAME" 
  private val SelectedUsersIDBundle = "idv.brianhsu.maidroid.plurk.SELECTED_USERS_ID" 

}

trait SelectLimitedToUI {

  this: Fragment =>

  import SelectLimitedToUI._

  protected def inflater: LayoutInflater
  protected def plurkAPI: PlurkAPI

  protected var selectedUsers: Set[(Long, String)] = Set.empty
  protected var selectedCliques: Set[String] = Set.empty

  protected def addLimitedToHolder: Option[ImageButton]
  protected def limitedToList: Option[LinearLayout]

  protected def createButton(title: String) = {
    val button = inflater.inflate(R.layout.view_people_button, null).asInstanceOf[Button]
    button.setText(title)
    button
  }

  protected def updateLimitedToList() {
    if (selectedCliques.isEmpty && selectedUsers.isEmpty) {
      updateLimitedToListSelectedAll()
    } else {
      updateLimitedToListSelectedSome()
    }
  }

  protected def updateLimitedToListSelectedAll() {

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

  protected def updateLimitedToListSelectedSome() {
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

  protected def showSelectPeopleDialog() {
    val dialogFragment = new AddLimitedToDialog(selectedCliques, selectedUsers.map(_._1))
    dialogFragment.show(getActivity.getSupportFragmentManager(), "selectedPeople")
  }

  protected def saveLimitedToUIState(outState: Bundle) {
    val sortedUsers = selectedUsers.toVector
    outState.putLongArray(SelectedUsersIDBundle, sortedUsers.map(_._1).toArray)
    outState.putStringArray(SelectedUsersNameBundle, sortedUsers.map(_._2).toArray)
    outState.putStringArray(SelectedCliquesBundle, selectedCliques.toArray)
  }

  protected def restoreLimitedToUIState(state: Bundle) {
    val savedCliques = Option(state.getStringArray(SelectedCliquesBundle)).map(_.toSet)
    this.selectedCliques = savedCliques getOrElse Set.empty[String]

    for {
      savedUsersID <- Option(state.getLongArray(SelectedUsersIDBundle))
      savedUsersName <- Option(state.getStringArray(SelectedUsersNameBundle))
      savedUsers = (savedUsersID zip savedUsersName).toSet
    } {
      this.selectedUsers = savedUsers
    }
  }

  protected def getSelectedLimitedTo: Set[Long] = {
    val userInCliques = for {
      cliqueName <- selectedCliques
      users <- plurkAPI.Cliques.getClique(cliqueName).get
    } yield users.id

    selectedUsers.map(_._1) ++ userInCliques
  }

}

class PostPrivateFragment extends Fragment with PlurkEditor with SelectLimitedToUI
{
  protected def plurkAPI = PlurkAPIHelper.getPlurkAPI(getActivity)
  protected def contentEditor = Option(getView).map(_.findView(TR.fragmentPostPrivateContent))
  protected def qualifierSpinner = Option(getView).map(_.findView(TR.fragmentPostPrivateQualifier))
  protected def responseTypeSpinner = Option(getView).map(_.findView(TR.fragmentPostPrivateResponseTypeSpinner))

  protected def addLimitedToHolder = Option(getView).map(_.findView(TR.fragmentPostPrivateAddLimitedTo))
  protected def limitedToList = Option(getView).map(_.findView(TR.fragmentPostPrivateLimitedToList))
  protected lazy val inflater = LayoutInflater.from(getActivity)


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

    if (savedInstanceState != null) {
      restoreLimitedToUIState(savedInstanceState)
    }

    updateLimitedToList()
  }

  override def onSaveInstanceState(outState: Bundle) {
    saveLimitedToUIState(outState)
  }

  override protected def limitedTo: List[Long] = {
    val selectedUsers = getSelectedLimitedTo
    selectedUsers.isEmpty match {
      case true  => List(0)
      case false => selectedUsers.toList
    }
  }

}

