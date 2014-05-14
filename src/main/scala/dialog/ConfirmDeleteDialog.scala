package idv.brianhsu.maidroid.plurk.fragment

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Context

object ConfirmDeleteDialog {

  def createDialog(context: Context, message: String)(onConfirmed: => Any) = {

    val alertDialog = new AlertDialog.Builder(context)

    alertDialog.setTitle("確定要刪除嗎？").
      setMessage("請問確定要刪除這則噗浪嗎？此動作無法回復喲！").
      setCancelable(true).
      setPositiveButton("刪除", new DialogInterface.OnClickListener() {
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

