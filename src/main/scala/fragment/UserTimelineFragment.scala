package idv.brianhsu.maidroid.plurk.fragment

import android.support.v4.app.Fragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

class UserTimelineFragment extends Fragment {
  override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)
  }

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle): View = {
    val t = new TextView(this.getActivity)
    t.setText("Profile")
    t
  }

}

