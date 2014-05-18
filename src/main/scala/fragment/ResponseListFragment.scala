package idv.brianhsu.maidroid.plurk.fragment

import idv.brianhsu.maidroid.plurk._
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
import android.webkit.WebViewClient
import android.webkit.WebView

import android.support.v4.app.FragmentActivity

import org.bone.soplurk.api._
import org.bone.soplurk.api.PlurkAPI._
import org.bone.soplurk.model._

import scala.concurrent._
import scala.util.Try

object ResponseListFragment {

  trait Listener {
    def onGetResponseSuccess(responses: PlurkResponses): Unit
    def onGetResponseFailure(e: Exception): Unit
  }
}

class ResponseListFragment extends Fragment {

  private implicit def activity = getActivity.asInstanceOf[FragmentActivity with ConfirmDialog.Listener with ResponseListFragment.Listener with PlurkView.Listener]

  private def plurkAPI = PlurkAPIHelper.getPlurkAPI(activity)

  private lazy val adapter = new ResponseAdapter(activity, plurk, owner)

  private def listHolder = Option(getView).map(_.findView(TR.fragmentResponseList))
  private def emptyViewHolder = Option(getView).map(_.findView(TR.fragmentResponseEmptyNotice))
  private def errorNoticeHolder = Option(getView).map(_.findView(TR.fragmentResponseErrorNotice))

  var plurk: Plurk = _
  var owner: User = _

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, 
                            savedInstanceState: Bundle): View = {
    inflater.inflate(R.layout.fragment_response, container, false)
  }

  override def onStart() {
    super.onStart()
    setupResponseListView()
    loadResponses()
  }

  private def setupResponseListView() {
    listHolder.foreach(_.setAdapter(adapter))

    for {
      list <- listHolder
      emptyView <- emptyViewHolder
    } {
      listHolder.foreach(_.setEmptyView(emptyView))
    }
  }

  def loadResponses() {

    val responses = future { plurkAPI.Responses.get(plurk.plurkID).get }

    responses.onSuccessInUI { response =>
      adapter.clearErrorCallback()
      adapter.update(response.responses, response.friends)
      PlurkView.updatePlurkCommentInfo(plurk.plurkID, response.responses.size, true)
      activity.onGetResponseSuccess(response)
    }

    responses.onFailureInUI { case e: Exception =>
      activity.onGetResponseFailure(e)
      val message = getString(R.string.cannotGetResponse)
      adapter.setupErrorCallback(message, () => { 
        adapter.clearErrorCallback()
        loadResponses()
      })
    }

  }

  def deleteResponse(responseID: Long) {
    adapter.deleteResponse(responseID)
  }
}

