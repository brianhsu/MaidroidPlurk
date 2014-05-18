package idv.brianhsu.maidroid.plurk.adapter

import idv.brianhsu.maidroid.plurk.R
import idv.brianhsu.maidroid.plurk.util.QualifierDisplay

import android.widget.Spinner
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import org.bone.soplurk.constant.Filter
import android.util.TypedValue

class FilterSpinnerAdapter(context: Context) extends BaseAdapter {

  def getCount = 5
  def getItemId(position: Int) = position
  def getItem(position: Int) = position match {
    case 0 => None
    case 1 => Some(Filter.OnlyUser)
    case 2 => Some(Filter.OnlyPrivate)
    case 3 => Some(Filter.OnlyResponded)
    case 4 => Some(Filter.OnlyFavorite)
  }

  override def getDropDownView(position: Int, convertView: View, parent:ViewGroup) = {
    val label = getView(position, convertView, parent)
    label.asInstanceOf[TextView].setPadding(15, 20, 0, 20)
    label
  }

  def getView(position: Int, convertView: View, parent: ViewGroup): View = {
    val label = convertView match {
      case oldView: TextView => oldView
      case _ => new TextView(context)
    }

    val title = position match {
      case 0 => R.string.filterAll
      case 1 => R.string.filterOnlyUser
      case 2 => R.string.filterOnlyPrivate
      case 3 => R.string.filterOnlyResponded
      case 4 => R.string.filterOnlyFavorite
    }

    label.setText(title)
    label.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18)
    label
  }
}

