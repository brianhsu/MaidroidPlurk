package idv.brianhsu.maidroid.plurk.adapter

import idv.brianhsu.maidroid.plurk.fragment._
import idv.brianhsu.maidroid.plurk.util._

import android.content.Context
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import idv.brianhsu.maidroid.plurk._


class AlertPagerAdapter(context: Context, fm: FragmentManager) extends FragmentPagerAdapter(fm) {
  override def getCount = 2
  override def getItem(position: Int) = position match {
    case 0 => new FriendRequestFragment
    case 1 => new AlertHistoryFragment
  }
  override def getPageTitle(position: Int) = position match {
    case 0 => context.getString(R.string.fragmentFriendRequestListTitle)
    case 1 => context.getString(R.string.fragmentAlertHistoryTitle)
  }
}


