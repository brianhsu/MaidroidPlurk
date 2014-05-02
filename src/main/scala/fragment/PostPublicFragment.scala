package idv.brianhsu.maidroid.plurk.fragment

import idv.brianhsu.maidroid.plurk._
import idv.brianhsu.maidroid.plurk.adapter._
import idv.brianhsu.maidroid.plurk.TypedResource._

import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.View
import android.os.Bundle
import android.widget.ArrayAdapter

import android.support.v4.app.Fragment

class PostPublicFragment extends Fragment with PlurkEditor {

  protected def contentEditor = Option(getView).map(_.findView(TR.fragmentPostPublicContent))

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, 
                            savedInstanceState: Bundle): View = {
    val view = inflater.inflate(R.layout.fragment_post_public, container, false)
    val respondAdapter = new ArrayAdapter(getActivity, android.R.layout.simple_spinner_item, Array("開放回應", "只有朋友可以回應", "關閉回應功能"))

    view
  }
}

