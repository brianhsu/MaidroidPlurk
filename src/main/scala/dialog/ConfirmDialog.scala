package idv.brianhsu.maidroid.plurk.dialog

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Context
import android.os.Bundle


import android.support.v4.app.DialogFragment

class ConfirmDialog extends DialogFragment {
  override def onCreateDialog(savedInstanceState: Bundle) = {
    val args = this.getArguments
    val title = args.getString("title")
    val message = args.getString("message")
    val okButtonTitle = args.getString("okButtonTitle")
    val cancelButtonTitle = args.getString("cancelButtonTitle")
    val dialogName = Symbol(args.getString("dialogName"))
    val alertDialog = new AlertDialog.Builder(getActivity)

    alertDialog.setTitle(title).
      setMessage(message).
      setCancelable(true).
      setPositiveButton(okButtonTitle, new DialogInterface.OnClickListener() {
        override def onClick(dialog: DialogInterface, which: Int) {
          val callback = getActivity.asInstanceOf[ConfirmDialog.Listener]
          callback.onDialogOKClicked(dialogName, dialog, args)
        }
      }).
      setNegativeButton(cancelButtonTitle, new DialogInterface.OnClickListener() {
        override def onClick(dialog: DialogInterface, which: Int) {
          val callback = getActivity.asInstanceOf[ConfirmDialog.Listener]
          callback.onDialogCancelClicked(dialogName, dialog, args)
        }
      })
    
    alertDialog.create()
  }
}

object ConfirmDialog {

  trait Listener {
    def onDialogOKClicked(dialogName: Symbol, dialog: DialogInterface, data: Bundle)
    def onDialogCancelClicked(dialogName: Symbol, dialog: DialogInterface, data: Bundle) {
      dialog.dismiss()
    }
  }

  def createDialog(activity: ConfirmDialog.Listener,
                   dialogName: Symbol,
                   title: String, message: String, 
                   okButtonTitle: String, 
                   cancelButtonTitle: String,
                   dataHolder: Option[Bundle] = None) = {

    val fragment = new ConfirmDialog
    val args = new Bundle

    args.putString("dialogName", dialogName.name)
    args.putString("title", title)
    args.putString("message", message)
    args.putString("okButtonTitle", okButtonTitle)
    args.putString("cancelButtonTitle", cancelButtonTitle)
    dataHolder.foreach(args.putAll)
    fragment.setArguments(args)
    fragment
  }

}

