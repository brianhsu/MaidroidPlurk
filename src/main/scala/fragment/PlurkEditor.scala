package idv.brianhsu.maidroid.plurk.fragment

import idv.brianhsu.maidroid.ui.util.AsyncUI._

import scala.concurrent._

import android.widget.EditText

import org.bone.soplurk.api.PlurkAPI
import org.bone.soplurk.constant.WritableCommentSetting
import org.bone.soplurk.constant.Qualifier

object PlurkEditor {
  object NoContentException extends Exception("無內容可以發噗")
}

trait PlurkEditor {

  protected def plurkAPI: PlurkAPI
  protected def contentEditor: Option[EditText]

  protected def limitedTo: List[Long] = Nil
  protected def commentSetting: Option[WritableCommentSetting] = None
  protected def qualifier: Qualifier = Qualifier.::

  def insertImage(url: String) {
    contentEditor.foreach { editor =>
      editor.getText.insert(editor.getSelectionStart.min(0), url)
    }
  }

  def postPlurk() = future {
    val isEmpty = contentEditor.map(_.getText.toString.trim.isEmpty).getOrElse(true)
    if (isEmpty) {
      throw PlurkEditor.NoContentException
    }

    val content = contentEditor.map(_.getText.toString).getOrElse("")
    //plurkAPI.Timeline.plurkAdd(content, qualifier, limitedTo, commentSetting)
    Thread.sleep(1000 * 10)
    content
  }


}
