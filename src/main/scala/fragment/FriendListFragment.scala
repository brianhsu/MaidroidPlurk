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
import android.widget.AdapterView
import android.widget.Toast
import android.webkit.WebViewClient
import android.webkit.WebView
import android.support.v4.app.FragmentActivity

import org.bone.soplurk.api._
import org.bone.soplurk.api.PlurkAPI._
import org.bone.soplurk.model._

import scala.concurrent._
import scala.util.Try

class FriendListFragment extends Fragment {

  private implicit def activity = getActivity.asInstanceOf[FragmentActivity]

  private def listViewHolder = Option(getView).map(_.findView(TR.userListListView))
  private def loadingIndicatorHolder = Option(getView).map(_.findView(TR.userListLoadingIndicator))
  private def errorNoticeHolder = Option(getView).map(_.findView(TR.userListErrorNotice))
  private def emptyNoticeHolder = Option(getView).map(_.findView(TR.userListEmptyNotice))
  private def retryButtonHolder = Option(getView).map(_.findView(TR.moduleErrorNoticeRetryButton))
  private def searchViewHolder = Option(getView).map(_.findView(TR.userListSearchView))

  private def plurkAPI = PlurkAPIHelper.getPlurkAPI(activity)
  private val userID = PlurkAPIHelper.plurkUserID


  private def getFriendList: Vector[ExtendedUser] = {
    var batch = plurkAPI.FriendsFans.getFriendsByOffset(userID, 100).get
    var allFriends: Vector[ExtendedUser] = batch.toVector
    
    while (batch != Nil) {
      batch = plurkAPI.FriendsFans.getFriendsByOffset(userID, 100, offset = Some(allFriends.size)).get
      allFriends = allFriends ++ batch.toVector
    }

    allFriends.distinct
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

  private def removeFriend(adapter: FriendListAdapter, user: ExtendedUser) {
    val dialogBuilder = new AlertDialog.Builder(activity)
    val displayName = user.basicInfo.displayName.getOrElse(user.basicInfo.nickname)
    val confirmDialog = 
        dialogBuilder.setTitle("確定要刪除好友嗎？")
                     .setMessage(s"確定要把【$displayName】從好友名單中移除嗎？")
                     .setPositiveButton(R.string.ok, null)
                     .setNegativeButton(R.string.cancel, null)
                     .create()

    confirmDialog.setOnShowListener(new DialogInterface.OnShowListener() {
      override def onShow(dialog: DialogInterface) {
        val okButton = confirmDialog.getButton(DialogInterface.BUTTON_POSITIVE)
        okButton.setOnClickListener { view: View =>
          val progressDialog = ProgressDialog.show(activity, "請稍候", "刪除好友中，請稍候……", true, false)
          val future = Future { plurkAPI.FriendsFans.removeAsFriend(user.basicInfo.id) }

          future.onSuccessInUI { status => 
            adapter.removeUser(user.basicInfo.id) 
            progressDialog.dismiss()
            confirmDialog.dismiss()
          }

          future.onFailureInUI { case e: Exception => 
            Toast.makeText(activity, "刪除失敗，請檢查網路狀態後重試一次。", Toast.LENGTH_LONG).show() 
            progressDialog.dismiss()
            confirmDialog.dismiss()
          }
        }
      }
    })

    confirmDialog.show()
  }

  def updateList() {
    val future = Future { getFriendList }
    future.onSuccessInUI { allFriends =>
      val adapter = new FriendListAdapter(activity, allFriends)
      listViewHolder.foreach { listView =>
        listView.setAdapter(adapter)
        emptyNoticeHolder.foreach(view => listView.setEmptyView(view))
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
          override def onItemClick(parent: AdapterView[_], view: View, position: Int, id: Long) {
            val user = adapter.getItem(position).asInstanceOf[ExtendedUser]
            UserTimelineActivity.startActivity(activity, user.basicInfo)
          }
        })

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

          override def onItemLongClick(parent: AdapterView[_], view: View, position: Int, id: Long): Boolean = {
            val dialog = new AlertDialog.Builder(activity)
            val itemList = Array("關看河道", "刪除好友")
            val itemAdapter = new ArrayAdapter(activity, android.R.layout.select_dialog_item, itemList)
            val onClickListener = new DialogInterface.OnClickListener {
              override def onClick(dialog: DialogInterface, which: Int) {
                val user = adapter.getItem(position)
                which match {
                  case 0 => UserTimelineActivity.startActivity(activity, user.basicInfo)
                  case 1 => removeFriend(adapter, user)
                }
              }
            }
            dialog.setTitle("請選擇")
                  .setAdapter(itemAdapter, onClickListener)
                  .show()
            true
          }
        })

      }
      loadingIndicatorHolder.foreach(_.setVisibility(View.GONE))
      searchViewHolder.foreach { searchView =>
        searchView.setIconifiedByDefault(false)
      }
    }

    future.onFailureInUI { case e: Exception =>
      showErrorNotice("無法取得好友列表")
    }

  }

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, 
                            savedInstanceState: Bundle): View = {
    val view = inflater.inflate(R.layout.fragment_user_list, container, false)
    updateList()
    view
  }

}

