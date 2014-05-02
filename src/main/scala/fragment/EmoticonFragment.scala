package idv.brianhsu.maidroid.plurk.fragment

import idv.brianhsu.maidroid.plurk._
import idv.brianhsu.maidroid.plurk.util.PlurkAPIHelper
import idv.brianhsu.maidroid.plurk.TypedResource._

import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.View
import android.os.Bundle

import android.support.v4.app.Fragment

class EmoticonFragment extends Fragment {

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, 
                            savedInstanceState: Bundle): View = {
    inflater.inflate(R.layout.fragment_emoticon, container, false)
  }
}

