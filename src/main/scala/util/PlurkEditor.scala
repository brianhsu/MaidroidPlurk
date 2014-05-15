package idv.brianhsu.maidroid.plurk.util

import idv.brianhsu.maidroid.ui.util.AsyncUI._
import idv.brianhsu.maidroid.plurk.view._

import scala.concurrent._

import android.graphics.drawable.Drawable
import android.widget.EditText
import android.widget.TextView

import android.text.Spanned
import android.text.style.ImageSpan
import android.text.style.DynamicDrawableSpan

import org.bone.soplurk.api.PlurkAPI
import org.bone.soplurk.model.Icon
import org.bone.soplurk.constant.WritableCommentSetting
import org.bone.soplurk.constant.Qualifier
import android.text.Editable
import android.text.TextWatcher
import android.graphics.Color

object PlurkEditor {
  object NoContentException extends Exception("無內容可以發噗")
}

trait PlurkEditor {

  protected def plurkAPI: PlurkAPI
  protected def contentEditorHolder: Option[EditText]
  protected def qualifierSpinnerHolder: Option[QualifierSpinner]
  protected def responseTypeSpinnerHolder: Option[ResponseTypeSpinner]
  protected def charCounterHolder: Option[TextView]

  protected def limitedTo: List[Long] = Nil

  protected def setupCharCounter() {
    for {
      contentEditor <- contentEditorHolder
      charCounter <- charCounterHolder
    } {
      contentEditor.addTextChangedListener(new TextWatcher() {
        override def beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
        override def onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
        override def afterTextChanged (editable: Editable) {
          val remainChars = 210 - editable.length
          charCounter.setText(remainChars.toString)

          if (remainChars < 0) {
            charCounter.setTextColor(Color.rgb(255, 0, 0))
          } else {
            charCounter.setTextColor(Color.rgb(255, 255, 255))
          }
        }
      })
    }
  }

  def setSelected(cliques: Set[String], users: Set[(Long, String)]) {}
  def setBlocked(cliques: Set[String], users: Set[(Long, String)]) {}

  def getContentLength = contentEditorHolder.map(_.getText.toString.size) getOrElse 0

  def setEditorContent(content: (Editable, Int)) {
    contentEditorHolder.foreach { editor => 
      editor.setText(content._1, android.widget.TextView.BufferType.SPANNABLE) 
      editor.setSelection(content._2)
    }
  }

  def setEditorContent(content: String) {
    contentEditorHolder.foreach { editor => 
      editor.setText(content) 
    }
  }

  def getEditorContent = contentEditorHolder.map { editor => 
    (editor.getText, editor.getSelectionStart.max(0))
  }

  def insertDrawable(originString: String, drawable: Drawable) {
    contentEditorHolder.foreach { editor =>
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
    contentEditorHolder.foreach { editor =>
      editor.getEditableText.insert(editor.getSelectionStart.max(0), text)
    }
  }


  def postPlurk() = future {

    val isEmpty = contentEditorHolder.map(_.getText.toString.trim.isEmpty).getOrElse(true)

    if (isEmpty) {
      throw PlurkEditor.NoContentException
    }

    val content = contentEditorHolder.map(_.getText.toString).getOrElse("")
    val language = plurkAPI.Users.currUser.get._1.basicInfo.defaultLanguage
    val qualifier = qualifierSpinnerHolder.map(_.getSelectedQualifier).getOrElse(Qualifier.::)
    val commentSetting = responseTypeSpinnerHolder.map(_.getSelectedCommentSetting).getOrElse(None)

    plurkAPI.Timeline.plurkAdd(
      content, qualifier, 
      limitedTo, commentSetting, Some(language)
    ).get
  }

  def postResponse (plurkID: Long) = future {

    val isEmpty = contentEditorHolder.map(_.getText.toString.trim.isEmpty).getOrElse(true)

    if (isEmpty) {
      throw PlurkEditor.NoContentException
    }

    val content = contentEditorHolder.map(_.getText.toString).getOrElse("")
    val qualifier = qualifierSpinnerHolder.map(_.getSelectedQualifier).getOrElse(Qualifier.::)
    plurkAPI.Responses.responseAdd(plurkID, content, qualifier).get
  }


}
