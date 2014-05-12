package idv.brianhsu.maidroid.plurk.view

import idv.brianhsu.maidroid.plurk._
import idv.brianhsu.maidroid.plurk.TypedResource._
import idv.brianhsu.maidroid.plurk.fragment.EmoticonFragment

import org.bone.soplurk.model.Icon

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.widget.AdapterView
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.LinearLayout
import android.widget.GridView
import android.graphics.drawable.Drawable

class IconGrid(activity: Activity, icons: Vector[Icon]) extends LinearLayout(activity) {
  private lazy val inflater = LayoutInflater.from(activity)
  private lazy val gridView = this.findView(TR.viewIconGridView)
  private val adapter = new IconAdapter

  class IconAdapter extends BaseAdapter {

    private var columnCounts: Option[Int] = None

    def getCount = icons.size
    def getItem(position: Int) = icons(position)
    def getItemId(position: Int) = position
    def getView(position: Int, convertView: View, parent: ViewGroup): View = {

      val iconView = convertView match {
        case oldView: IconView => oldView
        case _ => new IconView(activity)
      }
      iconView.update(icons(position))
      iconView
    }

  }

  inflater.inflate(R.layout.view_icon_grid, this, true)
  gridView.setAdapter(adapter)

  def setOnIconClickListener(callback: (Icon, Option[Drawable]) => Any) {
    gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      override def onItemClick(parent: AdapterView[_], view: View, position: Int, id: Long) {
        val drawable = view.asInstanceOf[IconView].getClonedDrawable(activity)
        callback(adapter.getItem(position), drawable)
      }
    })
  }
}

