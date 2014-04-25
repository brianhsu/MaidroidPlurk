package idv.brianhsu.maidroid.plurk.view

import idv.brianhsu.maidroid.plurk._
import idv.brianhsu.maidroid.plurk.TypedResource._
import idv.brianhsu.maidroid.ui.util.CallbackConversions._

import android.content.Context
import android.widget.LinearLayout
import android.view.LayoutInflater
import android.view.View
import android.widget.Button

object LoadMoreFooter {

  sealed trait Status
  object Status {
    case object Idle extends Status
    case object Loading extends Status
    case object Loaded extends Status
    case object Failed extends Status
  }
}

class LoadMoreFooter(context: Context) extends LinearLayout(context) {

  private lazy val inflater = LayoutInflater from context

  private lazy val progress = this.findView(TR.moduleLoadMoreFooterProgress)
  private lazy val retryButton = this.findView(TR.moduleLoadMoreFooterRetry)

  initView()

  private def initView() {
    inflater.inflate(R.layout.module_load_more_footer, this, true)
  }

  def setStatus(status: LoadMoreFooter.Status) {
    status match {
     case LoadMoreFooter.Status.Idle =>
       retryButton.setEnabled(false)
       retryButton.setVisibility(View.GONE)
       progress.setVisibility(View.GONE)
     case LoadMoreFooter.Status.Loading =>
       retryButton.setEnabled(false)
       retryButton.setVisibility(View.GONE)
       progress.setVisibility(View.VISIBLE)
     case LoadMoreFooter.Status.Loaded =>
       progress.setVisibility(View.GONE)
       retryButton.setVisibility(View.GONE)
       retryButton.setEnabled(false)
     case LoadMoreFooter.Status.Failed =>
       progress.setVisibility(View.GONE)
       retryButton.setVisibility(View.VISIBLE)
       retryButton.setEnabled(true)
     }
  }

  def setOnRetryClickListener(callback: Button => Any) {
    retryButton.setOnClickListener { view: View =>
      progress.setVisibility(View.VISIBLE)
      retryButton.setVisibility(View.GONE)
      callback(retryButton)
    }
  }

}

