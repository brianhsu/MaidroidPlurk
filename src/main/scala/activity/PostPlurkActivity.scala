package idv.brianhsu.maidroid.plurk.activity

import idv.brianhsu.maidroid.plurk._

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.support.v7.app.ActionBarActivity
import android.support.v4.app.Fragment

import android.view.LayoutInflater
import android.view.ViewGroup
import android.support.v7.app.ActionBar.TabListener
import android.support.v7.app.ActionBar.Tab
import android.support.v4.app.FragmentTransaction

class PostPublicFragment extends Fragment {

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, 
                            savedInstanceState: Bundle): View = {
    inflater.inflate(R.layout.fragment_post_public, container, false)
  }
}

class PostPrivateFragment extends Fragment {

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, 
                            savedInstanceState: Bundle): View = {
    inflater.inflate(R.layout.fragment_post_private, container, false)
  }
}

class PostBackstabFragment extends Fragment {

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, 
                            savedInstanceState: Bundle): View = {
    inflater.inflate(R.layout.fragment_post_backstab, container, false)
  }
}

import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.app.FragmentManager
import android.app.ActionBar
import android.support.v4.view.ViewPager.OnPageChangeListener

class PostPlurkAdapter(fragmentManager: FragmentManager) extends FragmentPagerAdapter(fragmentManager) {
  override def getItem(position: Int) = position match {
    case 0 => new PostPublicFragment
    case 1 => new PostPrivateFragment
    case 2 => new PostBackstabFragment
  }

  override def getCount = 3
  override def getPageTitle(position: Int) = {
    
    (position % 3) match {
      case 0 => "一般"
      case 1 => "私噗"
      case 2 => "背刺"
    }
  }
}

class PostPlurkActivity extends ActionBarActivity 
                        with TabListener with OnPageChangeListener
                        with TypedViewHolder 
{

  private lazy val viewPager = findView(TR.activityPostPlurkViewPager)

  override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_post_plurk)
    val actionbar = getSupportActionBar

    actionbar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS)
    actionbar.addTab(actionbar.newTab().setText("一般").setTabListener(this))
    actionbar.addTab(actionbar.newTab().setText("私噗").setTabListener(this))
    actionbar.addTab(actionbar.newTab().setText("背刺").setTabListener(this))
 
    val adapter = new PostPlurkAdapter(getSupportFragmentManager)
    viewPager.setAdapter(adapter)
    viewPager.setOnPageChangeListener(this)
  }

  override def onTabReselected(tab: Tab, ft: FragmentTransaction) {  }
  override def onTabUnselected(tab: Tab, ft: FragmentTransaction) {}
  override def onPageScrollStateChanged(state: Int) {}
  override def onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

  override def onTabSelected(tab: Tab, ft: FragmentTransaction) {
    viewPager.setCurrentItem(tab.getPosition)
  }

  override def onPageSelected(position: Int) {
    getSupportActionBar().setSelectedNavigationItem(position)
  }

}
