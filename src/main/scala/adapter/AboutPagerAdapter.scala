package idv.brianhsu.maidroid.plurk.adapter

import android.view.View
import android.view.ViewGroup

import android.support.v4.view.PagerAdapter

class AboutPageAdapter(views: Vector[View])  extends PagerAdapter {

  override def getCount = views.size

  override def instantiateItem(container: ViewGroup, position: Int): Object = {
    container.addView(views(position))
    views(position)
  }

  override def destroyItem(container: ViewGroup, position: Int, obj: Object) {
    container.removeView(views(position))
  }

  override def isViewFromObject(view: View, obj: Object) = {
    view == obj
  }
 
  override def getPageTitle(position: Int) = position match {
    case 0 => "版本"
    case 1 => "自由軟體授權"
    case 2 => "圖示授權"
  }
}

