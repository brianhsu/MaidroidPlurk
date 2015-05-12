package idv.brianhsu.maidroid.plurk.adapter

import idv.brianhsu.maidroid.plurk._

import android.content.Context
import android.view.View
import android.view.ViewGroup

import android.support.v4.view.PagerAdapter

class AboutPageAdapter(context: Context, views: Vector[View])  extends PagerAdapter {

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
    case 0 => context.getString(R.string.adapterAboutPagerVersion)
    case 1 => context.getString(R.string.adapterAboutPagerDonation)
    case 2 => context.getString(R.string.adapterAboutPagerAppLicense)
    case 3 => context.getString(R.string.adapterAboutPagerLibraryLicense)
    case 4 => context.getString(R.string.adapterAboutPagerIconLicenes)
  }
}

