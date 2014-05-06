package idv.brianhsu.maidroid.plurk.view

import idv.brianhsu.maidroid.plurk.adapter._
import org.bone.soplurk.constant.Qualifier

import android.widget.Spinner
import android.content.Context
import android.util.AttributeSet

class QualifierSpinner(context: Context, attrs: AttributeSet) extends Spinner(context, attrs) {

  this.setAdapter(new QualifierSpinnerAdapter(context))
  this.setSelection(17)

  def getSelectedQualifier = this.getSelectedItem.asInstanceOf[Qualifier]
}
