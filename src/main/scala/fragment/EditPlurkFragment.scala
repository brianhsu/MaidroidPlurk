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

class EditPlurkFragment(content: String) extends Fragment with PlurkEditor {

  private implicit def activity = getActivity

  def this() = this("")

  protected def plurkAPI = PlurkAPIHelper.getPlurkAPI(getActivity)
  protected def contentEditorHolder = Option(getView).map(_.findView(TR.fragmentEditPlurkContent))
  protected def qualifierSpinnerHolder = Option(getView).map(_.findView(TR.fragmentEditPlurkQualifier))
  protected def charCounterHolder = Option(getView).map(_.findView(TR.fragmentEditPlurkCharCounter))

  protected def responseTypeSpinnerHolder = None

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, 
                            savedInstanceState: Bundle): View = {
    inflater.inflate(R.layout.fragment_edit_plurk, container, false)
  }

  override def onViewCreated(view: View, savedInstanceState: Bundle) {

    setupCharCounter()

    if (savedInstanceState == null) {
      contentEditorHolder.foreach(_.setText(content))
    }
  }

}

