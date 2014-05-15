package idv.brianhsu.maidroid.plurk.fragment

import idv.brianhsu.maidroid.plurk._
import idv.brianhsu.maidroid.plurk.TypedResource._
import idv.brianhsu.maidroid.plurk.adapter._
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

import org.bone.soplurk.api._
import org.bone.soplurk.api.PlurkAPI._
import org.bone.soplurk.model._

import scala.concurrent._
import scala.util.Try

object ResponseListFragment {

  trait Listener {
    def onGetResponseSuccess(responses: PlurkResponses): Unit
    def onGetResponseFailure(e: Exception): Unit
    def onDeleteResponse(): Unit
    def onDeleteResponseSuccess(): Unit
    def onDeleteResponseFailure(e: Exception): Unit
  }
}

class ResponseListFragment extends Fragment {

  private implicit def activity = getActivity

  private def plurkAPI = PlurkAPIHelper.getPlurkAPI(activity)

  private var callbackHolder: Option[ResponseListFragment.Listener] = None
  private lazy val adapter = new ResponseAdapter(activity, plurk, owner)

  private def listHolder = Option(getView).map(_.findView(TR.fragmentResponseList))
  private def emptyViewHolder = Option(getView).map(_.findView(TR.fragmentResponseEmptyNotice))

  var plurk: Plurk = _
  var owner: User = _

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, 
                            savedInstanceState: Bundle): View = {
    inflater.inflate(R.layout.fragment_response, container, false)
  }

  override def onAttach(activity: Activity) {
    super.onAttach(activity)
    callbackHolder = for {
      activity <- Option(activity)
      callback <- Try(activity.asInstanceOf[ResponseListFragment.Listener]).toOption
    } yield callback
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
      adapter.update(response.responses, response.friends)
      PlurkView.updatePlurkCommentInfo(plurk.plurkID, response.responses.size, true)
      callbackHolder.foreach(_.onGetResponseSuccess(response))
    }

    responses.onFailureInUI { case e: Exception =>
      callbackHolder.foreach(_.onGetResponseFailure(e))
    }

  }
}

