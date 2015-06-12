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
import org.bone.soplurk.constant.AlertType

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

class FriendPendingFragment extends Fragment {

  private implicit def activity = getActivity.asInstanceOf[FragmentActivity]

  private def listViewHolder = Option(getView).map(_.findView(TR.userListListView))
  private def loadingIndicatorHolder = Option(getView).map(_.findView(TR.userListLoadingIndicator))
  private def errorNoticeHolder = Option(getView).map(_.findView(TR.userListErrorNotice))
  private def emptyNoticeHolder = Option(getView).map(_.findView(TR.userListEmptyNotice))
  private def retryButtonHolder = Option(getView).map(_.findView(TR.moduleErrorNoticeRetryButton))

  private def plurkAPI = PlurkAPIHelper.getPlurkAPI(activity)

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
    val future = Future { plurkAPI.Alerts.getActive.get.toVector.filter(_.alertType == AlertType.FriendshipPending) }
    future.onSuccessInUI { alertList =>

      val adapter = new AlertListAdapter(activity, alertList)


      listViewHolder.foreach { listView =>
        listView.setAdapter(adapter)
        emptyNoticeHolder.foreach(view => listView.setEmptyView(view))
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
          override def onItemClick(parent: AdapterView[_], view: View, position: Int, id: Long) {
            val alert = adapter.getItem(position).asInstanceOf[Alert]
            UserTimelineActivity.startActivity(activity, alert.user)
          }
        })

      }
      loadingIndicatorHolder.foreach(_.setVisibility(View.GONE))
    }

    future.onFailureInUI { case e: Exception =>
      showErrorNotice(activity.getString(R.string.fragmentFriendPendingFetchFailure))
    }

  }

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, 
                            savedInstanceState: Bundle): View = {
    val view = inflater.inflate(R.layout.fragment_user_list, container, false)
    updateList()
    view
  }

}

