package idv.brianhsu.maidroid.plurk.adapter

import idv.brianhsu.maidroid.plurk.fragment._
import idv.brianhsu.maidroid.plurk.util._

import android.content.Context
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import idv.brianhsu.maidroid.plurk._


class CurrentUserAdapter(context: Context, fm: FragmentManager) extends FragmentPagerAdapter(fm) {

  lazy val currentUserProfileFragment = CurrentUserProfileFragment.newInstance
  lazy val cliqueListFragment = new CliqueListFragment
  lazy val friendListFragment = new FriendListFragment
  lazy val fanListFragment = new FanListFragment
  lazy val followingListFragment = new FollowingListFragment
  lazy val blockListFragment = new BlockListFragment

  override def getCount = 6
  override def getItem(position: Int) = position match {
    case 0 => currentUserProfileFragment
    case 1 => cliqueListFragment
    case 2 => friendListFragment
    case 3 => fanListFragment
    case 4 => followingListFragment
    case 5 => blockListFragment
  }
  override def getPageTitle(position: Int) = position match {
    case 0 => context.getString(R.string.titleMyProfile)
    case 1 => context.getString(R.string.fragmentCliqueListTitle)
    case 2 => context.getString(R.string.fragmentFriendListTitle)
    case 3 => context.getString(R.string.fragmentFanListTitle)
    case 4 => context.getString(R.string.fragmentFollowingListTitle)
    case 5 => context.getString(R.string.fragmentBlockListTitle)
  }
}


