package idv.brianhsu.maidroid.plurk.dialog

import idv.brianhsu.maidroid.plurk._
import idv.brianhsu.maidroid.plurk.util._

import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.app.ProgressDialog
import android.app.Dialog

object ProgressDialogFragment {
  def createDialog(title: String, message: String, 
                   maxValueHolder: Option[Int] = None, 
                   isCancelable: Boolean = false) = {

    val args = new Bundle
    args.putString("title", title)
    args.putString("message", message)

    maxValueHolder.foreach { maxValue => args.putInt("maxValue", maxValue) }
    args.putBoolean("isCancelable", isCancelable)
    val fragment = new ProgressDialogFragment
    fragment.setArguments(args)
    fragment
  }
}

class ProgressDialogFragment extends DialogFragment {


  override def onCreateDialog(savedInstanceState: Bundle) = {
    val dialog = new ProgressDialog(getActivity)
    val title = getArguments.getString("title")
    val message = getArguments.getString("message")
    val isCancelable = getArguments.getBoolean("isCancelable")
    val maxValueHolder = Option(getArguments.getInt("maxValue", Int.MinValue)).filterNot(_ == Int.MinValue)
    dialog.setTitle(title)
    dialog.setCancelable(isCancelable)
    maxValueHolder match { 
      case None => 
        dialog.setMessage(message)
      case Some(maxValue) =>
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
        dialog.setProgressNumberFormat("%1d / %2d")
        dialog.setMax(maxValue)
        dialog.setProgress(0)
    }
    dialog
  }

  def setProgress(progress: Int) {
    getDialog.asInstanceOf[ProgressDialog].setProgress(progress)
  }

  def setTitle(title: String) {
    val dialog = getDialog.asInstanceOf[ProgressDialog]
    dialog.setTitle(title)
  }

  def setTitle(title: String, progress: Int, max: Int) {
    val dialog = getDialog.asInstanceOf[ProgressDialog]
    dialog.setTitle(title)
    dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
    dialog.setMax(max)
    dialog.setProgress(progress)
  }

}
