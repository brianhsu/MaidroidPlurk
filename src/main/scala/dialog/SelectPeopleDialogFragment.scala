package idv.brianhsu.maidroid.plurk.dialog

import idv.brianhsu.maidroid.plurk._
import idv.brianhsu.maidroid.plurk.TypedResource._
import idv.brianhsu.maidroid.ui.util.AsyncUI._
import idv.brianhsu.maidroid.plurk.util._
import idv.brianhsu.maidroid.plurk.adapter._
import org.bone.soplurk.model.Completion

import android.os.Bundle
import android.app.ProgressDialog
import android.app.AlertDialog
import android.content.DialogInterface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Toast

import android.support.v7.widget.SearchView
import android.support.v4.app.DialogFragment

import scala.concurrent._

object SelectPeopleDialog {
  val SelectedCliquesBundle = "idv.brianhsu.maidroid.plurk.SelectedCliquesBundle"
  val SelectedUsersBundle = "idv.brianhsu.maidroid.plurk.SelectedUsersBundle"
}

abstract class SelectPeopleDialog(title: Int,
                                 defaultSelectedCliques: Set[String], 
                                 defaultSelectedUsers: Set[Long]) extends DialogFragment {

  import SelectPeopleDialog._

  private lazy val plurkAPI = PlurkAPIHelper.getPlurkAPI(getActivity)
  private implicit def activity = getActivity
  protected def onDialogConfirmed(adapter: PeopleListAdapter)

  private lazy val adapterFuture = Future {

    val cliques = plurkAPI.Cliques.getCliques.get
    val completion = plurkAPI.FriendsFans.getCompletion.get
    val (currentUser, pageTitle, about) = plurkAPI.Users.currUser.get

    def toRecord(userID: Long, completion: Completion) = {
      val displayName = completion.displayName getOrElse completion.nickname
      (userID, s"${completion.fullName} (${displayName})")
    }

    val friends: Vector[(Long, String)] = completion.map { case(userID, completion) => toRecord(userID, completion) }.toVector
    val myDisplayName = currentUser.basicInfo.displayName getOrElse currentUser.basicInfo.nickname
    val me = (currentUser.basicInfo.id, myDisplayName)
    val allList = (friends :+ me).sortWith(_._2 < _._2)

    new PeopleListAdapter(activity, cliques.toVector, allList)
  }

  private def getOnCancelListener = new DialogInterface.OnClickListener() {
    override def onClick(dialog: DialogInterface, which: Int) {
      dialog.dismiss()
    }
  }

  private def getOnOKListener = new DialogInterface.OnClickListener() {
    override def onClick(dialog: DialogInterface, which: Int) {
      adapterFuture.foreach { adapter => onDialogConfirmed(adapter) }
      dialog.dismiss()
    }
  }

  private def startSearching(text: String) {
    adapterFuture.foreach { adapter =>
      activity.runOnUIThread { adapter.getFilter.filter(text) }
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
    val loadingIndicator = view.findView(TR.dialogSelectPeopleLoadingIndicator)

    val savedSelectedRow = for {
      bundle <- Option(savedInstanceState)
      selectedCliquesBundle <- Option(bundle.getStringArray(SelectedCliquesBundle))
      selectedUsersBundle <- Option(bundle.getLongArray(SelectedUsersBundle))
    } yield {
      (selectedCliquesBundle, selectedUsersBundle)
    }

    searchView.setIconifiedByDefault(false)

    val dialog = new AlertDialog.Builder(getActivity).
        setTitle(R.string.select).
        setView(view).
        setNegativeButton(R.string.cancel, getOnCancelListener).
        setPositiveButton(R.string.ok, getOnOKListener).
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

      listView.setEmptyView(view.findView(TR.dialogSelectPeopleEmptyNotice))
      listView.setAdapter(adapter)
      listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
        override def onItemClick(parent: AdapterView[_], view: View, position: Int, id: Long) {
          adapter.onItemClick(position, view)
        }
      })
      loadingIndicator.hide()
      content.setVisibility(View.VISIBLE)
    }

    adapterFuture.onFailureInUI { case e: Exception =>
      Toast.makeText(
        getActivity, 
        R.string.dialogSelectPeopleFetchCliqueFailure, 
        Toast.LENGTH_LONG
      ).show()
      dialog.dismiss()
    }

    searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
      override def onQueryTextChange(newText: String) = {
        startSearching(newText)
        false
      }
      override def onQueryTextSubmit(text: String) = {
        startSearching(text)
        true
      }
    })

    dialog.setTitle(title)
    dialog
  }
}

