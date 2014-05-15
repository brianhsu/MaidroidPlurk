package idv.brianhsu.maidroid.plurk.fragment

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Context

object ConfirmDialog {

  def createDialog(context: Context, title: String, message: String, 
                   okButtonTitle: String)(onConfirmed: => Any) = {

    val alertDialog = new AlertDialog.Builder(context)

    alertDialog.setTitle(title).
      setMessage(message).
      setCancelable(true).
      setPositiveButton(okButtonTitle, new DialogInterface.OnClickListener() {
        override def onClick(dialog: DialogInterface, which: Int) {
          onConfirmed
          dialog.dismiss()
        }
      }).
      setNegativeButton("取消", new DialogInterface.OnClickListener() {
        override def onClick(dialog: DialogInterface, which: Int) {
          dialog.dismiss()
        }
      })
  }

}

