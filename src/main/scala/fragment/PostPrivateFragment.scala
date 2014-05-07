package idv.brianhsu.maidroid.plurk.fragment

import idv.brianhsu.maidroid.plurk._
import idv.brianhsu.maidroid.plurk.util.PlurkAPIHelper
import idv.brianhsu.maidroid.plurk.TypedResource._
import idv.brianhsu.maidroid.ui.util.CallbackConversions._

import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.View
import android.os.Bundle

import android.support.v4.app.Fragment
import android.widget.PopupWindow

class PostPrivateFragment extends Fragment with PlurkEditor {

  protected def plurkAPI = PlurkAPIHelper.getPlurkAPI(getActivity)
  protected def contentEditor = Option(getView).map(_.findView(TR.fragmentPostPrivateContent))
  protected def qualifierSpinner = Option(getView).map(_.findView(TR.fragmentPostPrivateQualifier))
  protected def responseTypeSpinner = Option(getView).map(_.findView(TR.fragmentPostPrivateResponseTypeSpinner))
  protected def addLimitedToHolder = Option(getView).map(_.findView(TR.fragmentPostPrivateAddLimitedTo))

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, 
                            savedInstanceState: Bundle): View = {
    inflater.inflate(R.layout.fragment_post_private, container, false)
  }

  override def onViewCreated(view: View, savedInstanceState: Bundle) {
    addLimitedToHolder.foreach(_.setOnClickListener({ view: View =>
      new ProgressDialogFragment("AAA", "QQQ").show(getActivity.getSupportFragmentManager(), "tag")
    }))
  }

}

