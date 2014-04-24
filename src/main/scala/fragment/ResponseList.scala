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
import org.bone.soplurk.model._

import scala.concurrent._

object ResponseList {

  trait Listener {
  }
}

class ResponseList(plurk: Plurk, owner: User) extends Fragment with PlurkAdapter.Listener {

  private implicit def activity = getActivity

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, 
                            savedInstanceState: Bundle): View = {
    val view = inflater.inflate(R.layout.fragment_response, container, false)
    val originPlurkList = view.findView(TR.fragmentResponseOriginPlurk)
    val adapter = new PlurkAdapter(activity, true)
    originPlurkList.setAdapter(adapter)
    adapter.addOnlyOnePlurk(owner, plurk)
    view
  }
}

