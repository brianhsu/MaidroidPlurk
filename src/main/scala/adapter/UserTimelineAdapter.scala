package idv.brianhsu.maidroid.plurk.adapter

import idv.brianhsu.maidroid.plurk.fragment._
import android.content.Context
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import idv.brianhsu.maidroid.plurk._


class UserTimelineAdapter(context: Context, fm: FragmentManager, userID: Long) extends FragmentPagerAdapter(fm) {
  override def getCount = 2
  override def getItem(position: Int) = position match {
    case 0 => UserTimelineFragment.newInstance(userID)
    case 1 => UserProfileFragment.newInstance(userID)
  }
  override def getPageTitle(position: Int) = position match {
    case 0 => context.getString(R.string.adapterUserTimelineTimeline)
    case 1 => context.getString(R.string.adapterUserTimelineProfile)
  }
}


