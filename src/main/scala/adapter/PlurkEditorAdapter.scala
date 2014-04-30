package idv.brianhsu.maidroid.plurk.adapter

import idv.brianhsu.maidroid.plurk.fragment._

import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.view.ViewGroup

class PlurkEditorAdapter(fragmentManager: FragmentManager) extends FragmentPagerAdapter(fragmentManager) {

  private var editors: Map[Int, PlurkEditor] = Map.empty

  def getEditor(position: Int) = editors.get(position)

  override def getItem(position: Int) = {
    val newFragment = position match {
      case 0 => new PostPublicFragment
      case 1 => new PostPrivateFragment
      case 2 => new PostBackstabFragment
    }
    editors += (position -> newFragment)
    newFragment
  }

  override def getCount = 3
  override def getPageTitle(position: Int) = {
    
    (position % 3) match {
      case 0 => "一般"
      case 1 => "私噗"
      case 2 => "背刺"
    }
  }

  override def destroyItem(container: ViewGroup, position: Int, obj: Object) {
    super.destroyItem(container, position, obj)
    editors -= position
  }
}


