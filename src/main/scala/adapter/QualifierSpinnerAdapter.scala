package idv.brianhsu.maidroid.plurk.adapter

import idv.brianhsu.maidroid.plurk.util.QualifierDisplay

import android.widget.Spinner
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import org.bone.soplurk.constant.Qualifier
import android.util.TypedValue

class QualifierSpinnerAdapter(context: Context) extends BaseAdapter {

  def getCount = 20
  def getItemId(position: Int) = position
  def getItem(position: Int) = position match {
    case 0  => Qualifier.::
    case 1  => Qualifier.Loves
    case 2  => Qualifier.Likes
    case 3  => Qualifier.Shares
    case 4  => Qualifier.Gives
    case 5  => Qualifier.Hates
    case 6  => Qualifier.Wants
    case 7  => Qualifier.Wishes
    case 8  => Qualifier.Needs
    case 9  => Qualifier.Will
    case 10 => Qualifier.Hopes
    case 11 => Qualifier.Asks
    case 12 => Qualifier.Has
    case 13 => Qualifier.Was
    case 14 => Qualifier.Wonders
    case 15 => Qualifier.Feels
    case 16 => Qualifier.Thinks
    case 17 => Qualifier.Says
    case 18 => Qualifier.Is
    case 19 => Qualifier.Whispers
  }

  def getView(position: Int, convertView: View, parent: ViewGroup): View = {
    val label = convertView match {
      case oldView: TextView => oldView
      case _ => new TextView(context)
    }

    val qualifier = getItem(position).asInstanceOf[Qualifier]
    val (qualifierBackground, qualifierText) = QualifierDisplay(qualifier, context) getOrElse (0x000000, ":")
    label.setText(qualifierText)
    label.setPadding(10, 10, 10, 10)
    label.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16)
    label.setBackgroundColor(qualifierBackground)
    label
  }
}

