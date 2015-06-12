package idv.brianhsu.maidroid.plurk.fragment

import idv.brianhsu.maidroid.plurk._
import idv.brianhsu.maidroid.plurk.activity._
import idv.brianhsu.maidroid.plurk.dialog._
import idv.brianhsu.maidroid.plurk.util._
import idv.brianhsu.maidroid.plurk.adapter._
import idv.brianhsu.maidroid.plurk.util.PlurkAPIHelper
import idv.brianhsu.maidroid.plurk.TypedResource._

import android.app.Activity
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

  private implicit def activity = getActivity.asInstanceOf[PostPlurkActivity with PostPublicFragment.Listener]

  protected def plurkAPI = PlurkAPIHelper.getPlurkAPI(activity)
  protected def contentEditorHolder = Option(getView).map(_.findView(TR.fragmentPostPublicContent))
  protected def qualifierSpinnerHolder = Option(getView).map(_.findView(TR.fragmentPostPublicQualifier))
  protected def responseTypeSpinnerHolder = Option(getView).map(_.findView(TR.fragmentPostPublicResponseTypeSpinner))
  protected def charCounterHolder = Option(getView).map(_.findView(TR.fragmentPostPublicCharCounter))
  override protected def shareSettingPostfix = activity.shareSettingPostfix

  override def maxTextLength = 210 - shareSettingPostfix.length

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, 
                            savedInstanceState: Bundle): View = {
    val view = inflater.inflate(R.layout.fragment_post_public, container, false)
    view
  }

  override def onViewCreated(view: View, savedInstanceState: Bundle) {
    setupCharCounter()
  }

  override def onViewStateRestored(savedInstanceState: Bundle) {

    val isEditorEmpty = contentEditorHolder.map(_.getText.toString.trim.isEmpty).getOrElse(false)

    if (savedInstanceState == null && isEditorEmpty) {
      handleActionSend()
    }

    super.onViewStateRestored(savedInstanceState)
  }

  private def handleActionSend() {
    val intent = activity.getIntent
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
      editor <- contentEditorHolder
    } {
      editor.setText(content)
    }
  }

  private def processImage(intent: Intent) {
    val uri = intent.getParcelableExtra(Intent.EXTRA_STREAM).asInstanceOf[Uri]
    activity.onActionSendImage(uri)
  }

  private def processMultipleImage(intent: Intent) {
    val uriList = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM).toList
    activity.onActionSendMultipleImage(uriList)
  }

}

