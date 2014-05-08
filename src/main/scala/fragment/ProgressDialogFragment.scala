package idv.brianhsu.maidroid.plurk.fragment

import idv.brianhsu.maidroid.plurk._

import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.app.ProgressDialog
import android.app.Dialog

class ProgressDialogFragment(title: String, message: String, 
                             isCancelable: Boolean = false) extends DialogFragment {

  override def onCreateDialog(savedInstanceState: Bundle) = {
    val dialog = new ProgressDialog(getActivity)
    dialog.setMessage(message)
    dialog.setTitle(title)
    dialog
  }

  setCancelable(isCancelable)

}
