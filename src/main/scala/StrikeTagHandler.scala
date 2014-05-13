package idv.brianhsu.maidroid.plurk.util

import android.text.Html.TagHandler
import android.text.Editable
import android.text.style.StrikethroughSpan
import android.text.Spanned

import org.xml.sax.XMLReader

object StrikeTagHandler extends TagHandler {

  def handleTag(opening: Boolean, tag: String, output: Editable, xmlReader: XMLReader) {
    if(tag.equalsIgnoreCase("strike") || tag.equals("s")) {
      processStrike(opening, output);
    }
  }

  def processStrike(opening: Boolean, output: Editable) {
    var len = output.length
    if (opening) {
      output.setSpan(new StrikethroughSpan(), len, len, Spanned.SPAN_MARK_MARK);
    } else {

      getLast(output).foreach { span =>
        val where = output.getSpanStart(span)

        output.removeSpan(span)

        if (where != len) {
          output.setSpan(new StrikethroughSpan, where, len, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
      }
    }
  }

  def getLast(text: Editable): Option[StrikethroughSpan] = {
    val spans = text.getSpans(0, text.length(), classOf[StrikethroughSpan])
    spans.reverse.find { span => text.getSpanFlags(span) == Spanned.SPAN_MARK_MARK }
  }
}
