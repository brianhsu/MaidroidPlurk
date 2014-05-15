package idv.brianhsu.maidroid.plurk.fragment

import idv.brianhsu.maidroid.plurk._

import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.app.ProgressDialog
import android.app.Dialog

class ProgressDialogFragment(title: String, message: String, 
                             maxValueHolder: Option[Int] = None,
                             isCancelable: Boolean = false) extends DialogFragment {


  override def onCreateDialog(savedInstanceState: Bundle) = {
    val dialog = new ProgressDialog(getActivity)
    dialog.setTitle(title)
    maxValueHolder match { 
      case None => 
        dialog.setMessage(message)
      case Some(maxValue) =>
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
        dialog.setMax(maxValue)
        dialog.setProgress(0)
    }
    dialog
  }

  def setProgress(progress: Int) {
    getDialog.asInstanceOf[ProgressDialog].setProgress(progress)
  }

  setCancelable(isCancelable)

}