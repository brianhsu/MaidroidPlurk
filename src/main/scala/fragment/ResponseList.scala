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

object ResponseList {

  trait Listener {
    def onGetResponseSuccess(responses: PlurkResponses): Unit
    def onGetResponseFailure(e: Exception): Unit
  }
}

class ResponseList(plurk: Plurk, owner: User) extends Fragment with PlurkAdapter.Listener {

  private implicit def activity = getActivity

  private def plurkAPI = PlurkAPIHelper.getPlurkAPI

  private lazy val activityCallback = getActivity.asInstanceOf[ResponseList.Listener]
  private lazy val adapter = new ResponseAdapter(activity, plurk, owner)

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, 
                            savedInstanceState: Bundle): View = {
    val view = inflater.inflate(R.layout.fragment_response, container, false)
    setupResponseListView(view)
    view
  }

  override def onViewCreated(view: View, savedInstanceState: Bundle) {
    loadResponses()
  }

  private def setupResponseListView(containerView: View) {
    val list = containerView.findView(TR.fragmentResponseList)
    val emptyView = containerView.findView(TR.fragmentResponseEmptyNotice)
    list.setEmptyView(emptyView)
    list.setAdapter(adapter)
  }

  def loadResponses() {

    val responses = future { plurkAPI.Responses.get(plurk.plurkID).get }

    responses.onSuccessInUI { response =>
      adapter.update(response.responses, response.friends)
      PlurkView.updatePlurkCommentInfo(plurk.plurkID, response.responses.size, true)
      activityCallback.onGetResponseSuccess(response)
    }

    responses.onFailureInUI { case e: Exception =>
      activityCallback.onGetResponseFailure(e)
    }

  }
}

