package idv.brianhsu.maidroid.plurk.fragment

import idv.brianhsu.maidroid.ui.util.AsyncUI._
import idv.brianhsu.maidroid.plurk.view._
import idv.brianhsu.maidroid.plurk.util.DebugLog

import scala.concurrent._

import android.graphics.drawable.Drawable
import android.widget.EditText
import android.text.Spanned
import android.text.style.ImageSpan
import android.text.style.DynamicDrawableSpan

import org.bone.soplurk.api.PlurkAPI
import org.bone.soplurk.model.Icon
import org.bone.soplurk.constant.WritableCommentSetting
import org.bone.soplurk.constant.Qualifier
import android.text.Editable

object PlurkEditor {
  object NoContentException extends Exception("無內容可以發噗")
}

trait PlurkEditor {

  protected def plurkAPI: PlurkAPI
  protected def contentEditor: Option[EditText]
  protected def qualifierSpinner: Option[QualifierSpinner]
  protected def responseTypeSpinner: Option[ResponseTypeSpinner]

  protected def limitedTo: List[Long] = Nil

  def setSelected(cliques: Set[String], users: Set[(Long, String)]) {}
  def setBlocked(cliques: Set[String], users: Set[(Long, String)]) {}

  def setEditorContent(content: (Editable, Int)) {
    contentEditor.foreach { editor => 
      editor.setText(content._1, android.widget.TextView.BufferType.SPANNABLE) 
      editor.setSelection(content._2)
    }
  }

  def getEditorContent = contentEditor.map { editor => 
    (editor.getText, editor.getSelectionStart.max(0))
  }

  def insertDrawable(originString: String, drawable: Drawable) {
    contentEditor.foreach { editor =>
      drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight())
      val imageSpan = new ImageSpan(drawable, DynamicDrawableSpan.ALIGN_BASELINE)

      val start = editor.getSelectionStart.max(0)
      val end = editor.getSelectionEnd.max(0)
      val message = editor.getEditableText
      message.replace(start, end, originString)
      message.setSpan(imageSpan, start, start + originString.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
  }

  def insertIcon(icon: Icon, drawableHolder: Option[Drawable]) {
    drawableHolder match {
      case Some(drawable) => insertDrawable(s" ${icon.insertText} ", drawable)
      case None => insertText(icon.insertText)
    }
  }

  def insertText(text: String) {
    contentEditor.foreach { editor =>
      editor.getEditableText.insert(editor.getSelectionStart.max(0), text)
    }
  }


  def postPlurk() = future {

    val isEmpty = contentEditor.map(_.getText.toString.trim.isEmpty).getOrElse(true)

    if (isEmpty) {
      throw PlurkEditor.NoContentException
    }

    val content = contentEditor.map(_.getText.toString).getOrElse("")
    val language = plurkAPI.Users.currUser.get._1.basicInfo.defaultLanguage
    val qualifier = qualifierSpinner.map(_.getSelectedQualifier).getOrElse(Qualifier.::)
    val commentSetting = responseTypeSpinner.map(_.getSelectedCommentSetting).getOrElse(None)
    //plurkAPI.Timeline.plurkAdd(content, qualifier, limitedTo, commentSetting, Some(language)).get
    DebugLog("====> content:" + content)
    DebugLog("====> limitedTo:" + limitedTo)
  }

}
