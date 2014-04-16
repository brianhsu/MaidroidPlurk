package idv.brianhsu.maidroid.plurk.activity

import android.app.Activity
import android.os.Bundle

import idv.brianhsu.maidroid.plurk._
import idv.brianhsu.maidroid.ui.model._

class MaidroidPlurk extends Activity with TypedViewHolder
{
  lazy val dialogFrame = findView(TR.dialogFrame)

  override def onCreate(savedInstanceState: Bundle)
  {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_maidroid_plurk)
    dialogFrame.setMessages(
      Message(MaidMaro.Half.Smile, "歡迎來到 Hello World", None) :: 
      Message(MaidMaro.Half.Angry, "ABCDEFG", None) :: Nil
    )
  }
}
