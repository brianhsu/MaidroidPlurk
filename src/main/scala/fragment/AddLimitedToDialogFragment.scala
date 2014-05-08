package idv.brianhsu.maidroid.plurk.fragment

import idv.brianhsu.maidroid.plurk._
import idv.brianhsu.maidroid.plurk.TypedResource._
import idv.brianhsu.maidroid.ui.util.AsyncUI._
import idv.brianhsu.maidroid.plurk.util._
import idv.brianhsu.maidroid.plurk.adapter._

import org.bone.soplurk.model.Completion

import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.app.ProgressDialog
import android.app.Dialog
import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.content.DialogInterface

import scala.concurrent._

object AddLimitedToDialogFragment {
  val SelectedCliquesBundle = "idv.brianhsu.maidroid.plurk.SelectedCliquesBundle"
  val SelectedUsersBundle = "idv.brianhsu.maidroid.plurk.SelectedUsersBundle"

  trait Listener {
    def onDialogConfirmed(selectedCliques: Set[String], selectedUsers: Set[(Long, String)])
  }
}

class AddLimitedToDialogFragment(defaultSelectedCliques: Set[String], 
                                 defaultSelectedUsers: Set[Long]) extends DialogFragment {

  import AddLimitedToDialogFragment._

  private lazy val plurkAPI = PlurkAPIHelper.getPlurkAPI(getActivity)
  private implicit def activity = getActivity

  def this() = this(Set.empty, Set.empty)

  private lazy val adapterFuture = future {

    val cliques = plurkAPI.Cliques.getCliques.get
    val completion = plurkAPI.FriendsFans.getCompletion.get
    val friends = completion.map { case(userID, completion) =>
      val displayName = completion.displayName getOrElse completion.nickname
      (userID, s"${completion.fullName} (${displayName})")
    }.toVector

    new PeopleListAdapter(activity, cliques.toVector, friends)
  }

  private def getOnCancelListener = new DialogInterface.OnClickListener() {
    override def onClick(dialog: DialogInterface, which: Int) {
      dialog.dismiss()
    }
  }

  private def getOnOKListener = new DialogInterface.OnClickListener() {
    override def onClick(dialog: DialogInterface, which: Int) {
      adapterFuture.foreach { adapter =>
        val activityCallback = activity.asInstanceOf[AddLimitedToDialogFragment.Listener]
        activityCallback.onDialogConfirmed(
          adapter.getSelectedCliques, 
          adapter.getSelectedUsersWithTitle
        )
      }
      dialog.dismiss()
    }
  }

  override def onSaveInstanceState(outState: Bundle) {
    for {
      adapter <- adapterFuture
      selectedCliques = adapter.getSelectedCliques.toArray
      selectedUsers = adapter.getSelectedUsers.toArray
    } { 
      outState.putStringArray(SelectedCliquesBundle, selectedCliques)
      outState.putLongArray(SelectedUsersBundle, selectedUsers)
    }
    super.onSaveInstanceState(outState)
  }

  override def onCreateDialog(savedInstanceState: Bundle) = {
    val inflater = LayoutInflater.from(getActivity)
    val view = inflater.inflate(R.layout.dialog_select_people, null)
    val searchView = view.findView(TR.dialogSelectPeopleSearchView)
    val listView = view.findView(TR.dialogSelectPeopleListView)
    val content = view.findView(TR.dialogSelectPeopleContent)
    val loadingIndicator = view.findView(TR.moduleLoadingIndicator)

    val savedSelectedRow = for {
      bundle <- Option(savedInstanceState)
      selectedCliquesBundle <- Option(bundle.getStringArray(SelectedCliquesBundle))
      selectedUsersBundle <- Option(bundle.getLongArray(SelectedUsersBundle))
    } yield {
      (selectedCliquesBundle, selectedUsersBundle)
    }

    searchView.setIconifiedByDefault(false)

    val dialog = new AlertDialog.Builder(getActivity).
        setTitle("選取").
        setView(view).
        setNegativeButton("取消", getOnCancelListener).
        setPositiveButton("確定", getOnOKListener).
        create()

    adapterFuture.onSuccessInUI { adapter =>

      savedSelectedRow match {
        case None =>
          adapter.setSelectedCliques(defaultSelectedCliques)
          adapter.setSelectedUsers(defaultSelectedUsers)
        case Some((selectedCliques, selectedUsers)) =>
          adapter.setSelectedCliques(selectedCliques.toSet)
          adapter.setSelectedUsers(selectedUsers.toSet)
      }

      listView.setAdapter(adapter)
      listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
        override def onItemClick(parent: AdapterView[_], view: View, position: Int, id: Long) {
          adapter.onItemClick(position, view)
        }
      })
      loadingIndicator.setVisibility(View.GONE)
      content.setVisibility(View.VISIBLE)
    }

    adapterFuture.onFailureInUI { case e: Exception =>
      loadingIndicator.setVisibility(View.GONE)
      content.setVisibility(View.VISIBLE)
    }

    dialog
  }

}

