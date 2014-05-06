package idv.brianhsu.maidroid.plurk.adapter

import idv.brianhsu.maidroid.plurk.view.IconGrid
import idv.brianhsu.maidroid.plurk.util.EmoticonTabs

import android.app.Activity
import android.view.ViewGroup
import android.view.View
import android.support.v4.view.PagerAdapter

class IconPagerAdapter(activity: Activity, tabs: EmoticonTabs)  extends PagerAdapter {

  val tabsGrid = Vector(
    tabs.customPage, tabs.basicPage, 
    tabs.morePage, tabs.hiddenPage
  ).map(new IconGrid(activity, _))

  override def getCount = 4
  override def instantiateItem(container: ViewGroup, position: Int): Object = {
    val iconGrid = tabsGrid(position)
    iconGrid.setTag(position)
    container.addView(iconGrid)
    position.toString
  }
  override def destroyItem(container: ViewGroup, position: Int, obj: Object) {
    container.removeView(tabsGrid(position))
  }
  override def isViewFromObject(view: View, obj: Object) = {
    view.getTag.toString == obj.toString
  }
 
  override def getPageTitle(position: Int) = position match {
    case 0 => "自訂表情"
    case 1 => "常用表情"
    case 2 => "更多表情"
    case 3 => "隱藏表情"
  }
}

