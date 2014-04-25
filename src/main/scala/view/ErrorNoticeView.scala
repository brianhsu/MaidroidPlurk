package idv.brianhsu.maidroid.plurk.view

import idv.brianhsu.maidroid.plurk._
import idv.brianhsu.maidroid.plurk.TypedResource._
import idv.brianhsu.maidroid.ui.util.CallbackConversions._

import android.content.Context
import android.widget.LinearLayout
import android.view.LayoutInflater
import android.view.View
import android.util.AttributeSet
import android.widget.Button

class ErrorNoticeView(context: Context, attrs: AttributeSet) extends LinearLayout(context, attrs) {

  private lazy val inflater = LayoutInflater from context

  private lazy val messageTextView = this.findView(TR.moduleErrorNoticeText)
  private lazy val retryButton = this.findView(TR.moduleErrorNoticeRetryButton)

  initView()

  private def initView() {
    inflater.inflate(R.layout.module_error_notice, this, true)
  }

  def setMessage(message: String) {
    messageTextView.setText(message)
  }

  def setMessageWithRetry(message: String)(callback: Button => Any) {
    messageTextView.setText(message)
    retryButton.setOnClickListener { view: View => callback(retryButton) }
    retryButton.setEnabled(true)
  }

}

