package idv.brianhsu.maidroid.plurk.adapter

import idv.brianhsu.maidroid.plurk.fragment._
import idv.brianhsu.maidroid.plurk.util._

import android.content.Context
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import idv.brianhsu.maidroid.plurk._


class CurrentUserAdapter(context: Context, fm: FragmentManager) extends FragmentPagerAdapter(fm) {
  override def getCount = 3
  override def getItem(position: Int) = position match {
    case 0 => 
      println("=====> create current userpfoile")
      CurrentUserProfileFragment.newInstance()
    case 1 => 
      println("=====> create friend list")
      new FriendListFragment
    case 2 => 
      println("=====> create fan list")
      new FanListFragment
  }
  override def getPageTitle(position: Int) = position match {
    case 0 => context.getString(R.string.titleMyProfile)
    case 1 => context.getString(R.string.fragmentFriendListTitle)
    case 2 => context.getString(R.string.fragmentFanListTitle)
  }
}


