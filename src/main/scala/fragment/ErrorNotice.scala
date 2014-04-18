package idv.brianhsu.maidroid.plurk.fragment

import idv.brianhsu.maidroid.plurk._
import idv.brianhsu.maidroid.plurk.util._
import idv.brianhsu.maidroid.plurk.TypedResource._
import idv.brianhsu.maidroid.ui.util.CallbackConversions._

import android.app.Activity
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.View

object ErrorNotice {

  trait Listener {
    def onHideOtherUI() {}
  }

}

class ErrorNotice extends Fragment {

  private var activityCallback: ErrorNotice.Listener = _

  private lazy val errorMessage = getView.findView(TR.fragmentErrorNoticeText)
  private lazy val errorNotice = getView.findView(TR.fragmentErrorNotice)
  private lazy val errorRetryButton = getView.findView(TR.fragmentErrorNoticeRetryButton)

  override def onAttach(activity: Activity) {
    super.onAttach(activity)

    try {
      activityCallback = activity.asInstanceOf[ErrorNotice.Listener]
    } catch {
      case e: ClassCastException => throw new ClassCastException(s"${activity} must mixed with ErrorNotice.Listener")
    }
  }

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, 
                            savedInstanceState: Bundle): View = {
    inflater.inflate(R.layout.fragment_error_notice, container, false)
  }

  def showMessageWithRetry(message: String, cause: Exception)(retry: => Any) {
    showMessage(message, cause)
    errorRetryButton.setEnabled(true)
    errorRetryButton.setOnClickListener { view: View =>
      retry
    }
  }

  def showMessage(message: String, cause: Exception) {
    DebugLog("====> ErrorNotice.showMessage:" + cause.getMessage, cause)
    activityCallback.onHideOtherUI()
    errorMessage.setText(message)
    errorNotice.setVisibility(View.VISIBLE)
    errorRetryButton.setEnabled(false)
  }

  def setVisibility(visibility: Int) {
    Option(this.getView).foreach(_.setVisibility(visibility))
  }

}

