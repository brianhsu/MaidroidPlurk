package idv.brianhsu.maidroid.plurk.fragment

import idv.brianhsu.maidroid.plurk._
import idv.brianhsu.maidroid.plurk.activity._
import idv.brianhsu.maidroid.plurk.TypedResource._
import idv.brianhsu.maidroid.plurk.adapter._
import idv.brianhsu.maidroid.plurk.dialog._
import idv.brianhsu.maidroid.plurk.util._
import idv.brianhsu.maidroid.plurk.view._
import idv.brianhsu.maidroid.ui.util.AsyncUI._

import android.app.Activity
import android.os.Bundle
import android.net.Uri
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.View
import android.widget.AdapterView
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

