package idv.brianhsu.maidroid.plurk.fragment

import idv.brianhsu.maidroid.plurk._
import idv.brianhsu.maidroid.plurk.activity._
import idv.brianhsu.maidroid.plurk.TypedResource._
import idv.brianhsu.maidroid.plurk.adapter._
import idv.brianhsu.maidroid.plurk.dialog._
import idv.brianhsu.maidroid.plurk.util._
import idv.brianhsu.maidroid.plurk.view._
import idv.brianhsu.maidroid.ui.util.AsyncUI._
import idv.brianhsu.maidroid.ui.util.CallbackConversions._

import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.widget.ArrayAdapter
import android.content.DialogInterface
import android.os.Bundle
import android.net.Uri
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.View
import android.view.Menu
import android.view.MenuItem
import android.view.MenuInflater
import android.widget.AdapterView
import android.widget.Toast
import android.webkit.WebViewClient
import android.webkit.WebView
import android.support.v4.app.FragmentActivity
import android.support.v7.widget.SearchView
import android.support.v4.view.MenuItemCompat

import org.bone.soplurk.api._
import org.bone.soplurk.api.PlurkAPI._
import org.bone.soplurk.model._

import scala.concurrent._
import scala.util.Try

class BlockListFragment extends Fragment {

  private implicit def activity = getActivity.asInstanceOf[FragmentActivity]

  private def listViewHolder = Option(getView).map(_.findView(TR.userListListView))
  private def loadingIndicatorHolder = Option(getView).map(_.findView(TR.userListLoadingIndicator))
  private def errorNoticeHolder = Option(getView).map(_.findView(TR.userListErrorNotice))
  private def emptyNoticeHolder = Option(getView).map(_.findView(TR.userListEmptyNotice))
  private def retryButtonHolder = Option(getView).map(_.findView(TR.moduleErrorNoticeRetryButton))

  private def plurkAPI = PlurkAPIHelper.getPlurkAPI(activity)
  private var blockList: Option[Vector[User]] = None
  private lazy val searchView = new SearchView(activity)

  private def getBlockList: Vector[User] = {

    blockList match {
      case Some(list) => list
      case None =>
        var batch = plurkAPI.Blocks.get().get._2
        var allBlock: Vector[User] = batch.toVector
    
        while (batch != Nil) {
          batch = plurkAPI.Blocks.get(offset = allBlock.size).get._2
          allBlock = allBlock ++ batch.toVector
        }
        val distinctUser = allBlock.distinct
        blockList = Some(distinctUser)
        distinctUser
    }
  }

  private def showErrorNotice(message: String) {
    loadingIndicatorHolder.foreach(_.hide())
    errorNoticeHolder.foreach(_.setVisibility(View.VISIBLE))
    errorNoticeHolder.foreach { errorNotice =>
      errorNotice.setMessageWithRetry(message) { retryButton =>
        retryButton.setEnabled(false)
        errorNoticeHolder.foreach(_.setVisibility(View.GONE))
        loadingIndicatorHolder.foreach(_.show())
        updateList()
      }
    }
  }

  private def removeBlock(adapter: UserListAdapter, user: User) {
    val dialogBuilder = new AlertDialog.Builder(activity)
    val displayName = (
      user.displayName.filterNot(_.trim.isEmpty) orElse 
      Option(user.fullName).filterNot(_.trim.isEmpty) orElse
      Option(user.nickname).filterNot(_.trim.isEmpty)
    ).getOrElse(user.id)

    val confirmDialog = 
        dialogBuilder.setTitle(R.string.fragmentBlockListUnblockTitle)
                     .setMessage(activity.getString(R.string.fragmentBlockListUnblockMessage).format(displayName))
                     .setPositiveButton(R.string.ok, null)
                     .setNegativeButton(R.string.cancel, null)
                     .create()

    confirmDialog.setOnShowListener(new DialogInterface.OnShowListener() {
      override def onShow(dialog: DialogInterface) {
        val okButton = confirmDialog.getButton(DialogInterface.BUTTON_POSITIVE)
        okButton.setOnClickListener { view: View =>
          val progressDialog = ProgressDialog.show(
            activity, 
            activity.getString(R.string.pleaseWait), 
            activity.getString(R.string.fragmentBlockListUnblocking), 
            true, false
          )
          val future = Future { plurkAPI.Blocks.unblock(user.id) }

          future.onSuccessInUI { status => 
            adapter.removeUser(user.id) 
            progressDialog.dismiss()
            confirmDialog.dismiss()
          }

          future.onFailureInUI { case e: Exception => 
            Toast.makeText(activity, R.string.fragmentBlockListUnblockFailed, Toast.LENGTH_LONG).show() 
            progressDialog.dismiss()
            confirmDialog.dismiss()
          }
        }
      }
    })

    confirmDialog.show()
  }

  def updateList() {
    val future = Future { getBlockList }
    future.onSuccessInUI { allFollowings =>
      val adapter = new UserListAdapter(activity, allFollowings)

      listViewHolder.foreach { listView =>
        listView.setAdapter(adapter)
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
          override def onQueryTextChange(newText: String) = {
            adapter.getFilter.filter(newText)
            false
          }
          override def onQueryTextSubmit(text: String) = {
            adapter.getFilter.filter(text)
            true
          }
        })

        emptyNoticeHolder.foreach(view => listView.setEmptyView(view))
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
          override def onItemClick(parent: AdapterView[_], view: View, position: Int, id: Long) {
            val user = adapter.getItem(position).asInstanceOf[User]
            UserTimelineActivity.startActivity(activity, user)
          }
        })

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

          override def onItemLongClick(parent: AdapterView[_], view: View, position: Int, id: Long): Boolean = {
            val dialog = new AlertDialog.Builder(activity)
            val itemList = Array(
              activity.getString(R.string.fragmentBlockListViewTimeline), 
              activity.getString(R.string.fragmentBlockListUnblock)
            )
            val itemAdapter = new ArrayAdapter(activity, android.R.layout.select_dialog_item, itemList)
            val onClickListener = new DialogInterface.OnClickListener {
              override def onClick(dialog: DialogInterface, which: Int) {
                val user = adapter.getItem(position)
                which match {
                  case 0 => UserTimelineActivity.startActivity(activity, user)
                  case 1 => removeBlock(adapter, user)
                }
              }
            }
            dialog.setTitle(R.string.fragmentBlockListAction)
                  .setAdapter(itemAdapter, onClickListener)
                  .show()
            true
          }
        })

      }
      loadingIndicatorHolder.foreach(_.setVisibility(View.GONE))
    }

    future.onFailureInUI { case e: Exception =>
      showErrorNotice(activity.getString(R.string.fragmentBlockFetchFailure))
    }

  }

  override def onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
    inflater.inflate(R.menu.fragment_user_list, menu)
    val searchItem = menu.findItem(R.id.userListSearch)
    if (searchView.getParent != null) {
      searchView.getParent.asInstanceOf[ViewGroup].removeView(searchView)
    }
    MenuItemCompat.setActionView(searchItem, searchView)
    searchView.setIconified(true)
    super.onCreateOptionsMenu(menu, inflater)
  }

  override def onOptionsItemSelected(item: MenuItem) = item.getItemId match {
    case _ => super.onOptionsItemSelected(item)
  }

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, 
                            savedInstanceState: Bundle): View = {
    val view = inflater.inflate(R.layout.fragment_user_list, container, false)
    updateList()
    setHasOptionsMenu(true)
    view
  }

}

