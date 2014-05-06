package idv.brianhsu.maidroid.plurk.view

import idv.brianhsu.maidroid.plurk._
import idv.brianhsu.maidroid.plurk.TypedResource._

import org.bone.soplurk.model.Icon

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.LinearLayout
import android.widget.GridView

class IconGrid(activity: Activity, icons: Vector[Icon]) extends LinearLayout(activity) {
  private lazy val inflater = LayoutInflater.from(activity)
  private lazy val gridView = this.findView(TR.viewIconGridView)
  private val adapter = new IconAdapter

  class IconAdapter extends BaseAdapter {

    private var columnCounts: Option[Int] = None

    def getCount = icons.size
    def getItem(position: Int) = icons(position.max(getCount-1))
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
}

