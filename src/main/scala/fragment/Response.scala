package idv.brianhsu.maidroid.plurk.fragment

import idv.brianhsu.maidroid.plurk._
import idv.brianhsu.maidroid.plurk.util._
import idv.brianhsu.maidroid.plurk.TypedResource._
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
import scala.concurrent._

object Response {
  trait Listener {
  }
}

class Response extends Fragment {

  private implicit def activity = getActivity

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, 
                            savedInstanceState: Bundle): View = {
    inflater.inflate(R.layout.fragment_response, container, false)
  }
}

