package idv.brianhsu.maidroid.plurk.dialog

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Context

object ConfirmDialog {

  def createDialog(context: Context, title: String, message: String,
                   okButtonTitle: String)(onConfirmed: DialogInterface => Any): AlertDialog = {
    createDialog(context, title, message, okButtonTitle, "取消")(onConfirmed)
  }
  def createDialog(context: Context, title: String, message: String, 
                   okButtonTitle: String, 
                   cancelButtonTitle: String)(onConfirmed: DialogInterface => Any): AlertDialog = {

    val alertDialog = new AlertDialog.Builder(context)

    alertDialog.setTitle(title).
      setMessage(message).
      setCancelable(true).
      setPositiveButton(okButtonTitle, new DialogInterface.OnClickListener() {
        override def onClick(dialog: DialogInterface, which: Int) {
          onConfirmed(dialog)
        }
      }).
      setNegativeButton(cancelButtonTitle, new DialogInterface.OnClickListener() {
        override def onClick(dialog: DialogInterface, which: Int) {
          dialog.dismiss()
        }
      })
    
    alertDialog.create()
  }

}

