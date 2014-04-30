package idv.brianhsu.maidroid.plurk.fragment

import android.widget.EditText

trait PlurkEditor {
  protected def contentEditor: Option[EditText]

  def insertImage(url: String) {
    contentEditor.foreach { editor =>
      editor.getText.insert(editor.getSelectionStart.min(0), url)
    }
  }
}
