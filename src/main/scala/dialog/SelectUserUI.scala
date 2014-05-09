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
import android.view.ViewGroup.LayoutParams._
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout

object SelectUserUI
{
  private val SelectedCliquesBundle = "idv.brianhsu.maidroid.plurk.SELECTED_CLIQUES" 
  private val SelectedUsersNameBundle = "idv.brianhsu.maidroid.plurk.SELECTED_USERS_NAME" 
  private val SelectedUsersIDBundle = "idv.brianhsu.maidroid.plurk.SELECTED_USERS_ID" 

}

abstract class SelectUserUI(fragment: Fragment, 
                            plurkAPI: PlurkAPI, 
                            buttonResID: Int,
                            buttonHolder: Option[ImageButton], 
                            listHolder: Option[LinearLayout]) {

  import SelectUserUI._

  var selectedUsers: Set[(Long, String)] = Set.empty
  var selectedCliques: Set[String] = Set.empty

  private lazy val inflater = LayoutInflater.from(fragment.getActivity)

  protected def createDialog(selectedCliques: Set[String], 
                             selectedUsers: Set[Long]) : SelectPeopleDialog

  protected def createButton(title: String) = {
    val button = inflater.inflate(buttonResID, null).asInstanceOf[Button]
    button.setText(title)
    button
  }

  def updateUI() {
    if (selectedCliques.isEmpty && selectedUsers.isEmpty) {
      updateUISelectedNone()
    } else {
      updateUISelectedSome()
    }
  }

  protected def updateUISelectedNone() {

    fragment.getActivity.runOnUIThread { 
      listHolder.foreach { viewGroup =>
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

  protected def updateUISelectedSome() {
    fragment.getActivity.runOnUIThread { 
      listHolder.foreach { viewGroup =>

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
              updateUISelectedNone()
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
              updateUISelectedNone()
            } else {
              viewGroup.removeView(button)
            }

          }
        }
      }
    }
  }

  def showSelectPeopleDialog() {
    val dialogFragment = createDialog(selectedCliques, selectedUsers.map(_._1))
    dialogFragment.show(
      fragment.getActivity.getSupportFragmentManager(), 
      "selectedPeople"
    )
  }

  def saveUIState(outState: Bundle) {
    val sortedUsers = selectedUsers.toVector
    outState.putLongArray(SelectedUsersIDBundle, sortedUsers.map(_._1).toArray)
    outState.putStringArray(SelectedUsersNameBundle, sortedUsers.map(_._2).toArray)
    outState.putStringArray(SelectedCliquesBundle, selectedCliques.toArray)
  }

  def restoreUIState(state: Bundle) {
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

  def getSelectedUsers: Set[Long] = {
    val userInCliques = for {
      cliqueName <- selectedCliques
      users <- plurkAPI.Cliques.getClique(cliqueName).get
    } yield users.id

    selectedUsers.map(_._1) ++ userInCliques
  }

  buttonHolder.foreach { button =>
    button.setOnClickListener { view: View => showSelectPeopleDialog() }
  }

}
