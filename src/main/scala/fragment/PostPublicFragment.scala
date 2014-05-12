package idv.brianhsu.maidroid.plurk.fragment

import idv.brianhsu.maidroid.plurk._
import idv.brianhsu.maidroid.plurk.util._
import idv.brianhsu.maidroid.plurk.adapter._
import idv.brianhsu.maidroid.plurk.util.PlurkAPIHelper
import idv.brianhsu.maidroid.plurk.TypedResource._

import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.View
import android.os.Bundle
import android.widget.ArrayAdapter

import android.support.v4.app.Fragment
import android.content.Intent
import android.net.Uri
import scala.collection.JavaConversions._

object PostPublicFragment {
  trait Listener {
    def onActionSendImage(uri: Uri)
    def onActionSendMultipleImage(uriList: List[Uri])
  }
}

class PostPublicFragment extends Fragment with PlurkEditor {

  protected def plurkAPI = PlurkAPIHelper.getPlurkAPI(getActivity)
  protected def contentEditor = Option(getView).map(_.findView(TR.fragmentPostPublicContent))
  protected def qualifierSpinner = Option(getView).map(_.findView(TR.fragmentPostPublicQualifier))
  protected def responseTypeSpinner = Option(getView).map(_.findView(TR.fragmentPostPublicResponseTypeSpinner))

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, 
                            savedInstanceState: Bundle): View = {
    val view = inflater.inflate(R.layout.fragment_post_public, container, false)
    view
  }

  override def onViewStateRestored(savedInstanceState: Bundle) {

    val isEditorEmpty = contentEditor.map(_.getText.toString.trim.isEmpty).getOrElse(false)

    if (savedInstanceState == null && isEditorEmpty) {
      handleActionSend()
    }

    super.onViewStateRestored(savedInstanceState)
  }

  private def handleActionSend() {
    val intent = getActivity.getIntent
    val action = intent.getAction
    val mimeType = Option(intent.getType)

    if (action == Intent.ACTION_SEND && mimeType.isDefined) {
      mimeType match {
        case Some("text/plain") => processText(intent)
        case Some(mType) if mType.startsWith("image/") => processImage(intent)
        case _ =>
      }
    } else if (action == Intent.ACTION_SEND_MULTIPLE && mimeType.isDefined) {
      mimeType match {
        case Some(mType) if mType.startsWith("image/") => processMultipleImage(intent)
        case _ =>
      }
    }

  }

  private def processText(intent: Intent) {
    for {
      content <- Option(intent.getStringExtra(Intent.EXTRA_TEXT))
      editor <- contentEditor
    } {
      editor.setText(content)
    }
  }

  private def processImage(intent: Intent) {
    val uri = intent.getParcelableExtra(Intent.EXTRA_STREAM).asInstanceOf[Uri]
    getActivity.asInstanceOf[PostPublicFragment.Listener].onActionSendImage(uri)
  }

  private def processMultipleImage(intent: Intent) {
    val uriList = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM).toList
    getActivity.asInstanceOf[PostPublicFragment.Listener].onActionSendMultipleImage(uriList)
  }

}

