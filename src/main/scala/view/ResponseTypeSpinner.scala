package idv.brianhsu.maidroid.plurk.view

import idv.brianhsu.maidroid.plurk.adapter._

import org.bone.soplurk.constant.Qualifier
import org.bone.soplurk.constant.CommentSetting

import android.widget.Spinner

import android.content.Context
import android.util.AttributeSet

import android.widget.BaseAdapter
import android.widget.TextView
import android.view.View
import android.view.ViewGroup
import android.util.TypedValue


class ResponseTypeAdapter(context: Context) extends BaseAdapter {
  def getCount = 3
  def getItemId(position: Int) = position
  def getItem(position: Int) = position match {
    case 0 => None
    case 1 => Some(CommentSetting.OnlyFriends)
    case 2 => Some(CommentSetting.Disabled)
  }

  def getView(position: Int, convertView: View, viewGroup: ViewGroup): View = {
    val view = convertView match {
      case oldView: TextView => oldView
      case _ => new TextView(context)
    }

    view.setPadding(10, 10, 10, 10)
    view.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16)
    position match {
      case 0 => view.setText("開放回應")
      case 1 => view.setText("只有好友可以回應")
      case 2 => view.setText("關閉回應")
    }
    view
  }
}

class ResponseTypeSpinner(context: Context, attrs: AttributeSet) extends 
      Spinner(context, attrs){
  this.setAdapter(new ResponseTypeAdapter(context))
}
