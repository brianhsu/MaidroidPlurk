package idv.brianhsu.maidroid.plurk.fragment

import idv.brianhsu.maidroid.plurk._
import idv.brianhsu.maidroid.plurk.util._
import idv.brianhsu.maidroid.plurk.TypedResource._
import idv.brianhsu.maidroid.ui.util.AsyncUI._

import android.app.Activity
import android.os.Bundle
import android.net.Uri
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.View
import android.webkit.WebViewClient
import android.webkit.WebView
import android.widget.Button

import org.bone.soplurk.api._
import scala.concurrent._
import scala.util.Try

object LoginFragment {
  trait Listener {
    def onGetAuthURLFailure(error: Exception): Unit
    def onLoginFailure(error: Exception): Unit
    def onLoginSuccess(): Unit
  }
}

class LoginFragment extends Fragment {

  private implicit def activity = getActivity.asInstanceOf[Activity with LoginFragment.Listener]

  private lazy val plurkAPI = PlurkAPIHelper.getNewPlurkAPI
  private lazy val webViewHolder = Option(getView).map(_.findView(TR.fragmentLoginWebView))
  private lazy val loadingIndicatorHolder = Option(getView).map(_.findView(TR.moduleLoadingIndicator))
  private lazy val errorNoticeHolder = Option(getView).map(_.findView(TR.fragmentLoginErrorNotice))

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, 
                            savedInstanceState: Bundle): View = {
    inflater.inflate(R.layout.fragment_login, container, false)
  }

  override def onStart() {
    super.onStart()
    startAuthorization()
  }

  private def showErrorNotice(message: String){
    loadingIndicatorHolder.foreach(_.setVisibility(View.GONE))
    errorNoticeHolder.foreach(_.setVisibility(View.VISIBLE))
    errorNoticeHolder.foreach { errorNotice =>
      errorNotice.setMessageWithRetry(message) { retryButton =>
        retryButton.setEnabled(false)
        errorNoticeHolder.foreach(_.setVisibility(View.GONE))
        loadingIndicatorHolder.foreach(_.setVisibility(View.VISIBLE))
        startAuthorization()
      }
    }
  }

  private def startAuthorization() {

    webViewHolder.foreach(_.setWebViewClient(plurkAuthWebViewClient))

    val authorizationURL: Future[String] = future {
      plurkAPI.getAuthorizationURL.get.replace("OAuth", "m")
    }

    authorizationURL.onFailureInUI { 
      case error: Exception => {
        DebugLog("====> authorizationURL.onFailureInUI:" + error.getMessage, error) 
        activity.onGetAuthURLFailure(error)
        showErrorNotice("無法取得噗浪登入網址")
      }
    }

    authorizationURL.onSuccessInUI { url =>
      webViewHolder.foreach(_.getSettings.setJavaScriptEnabled(true))
      webViewHolder.foreach(_.loadUrl(url))
    }
  }

  private def plurkAuthWebViewClient = new WebViewClient() {

    override def shouldOverrideUrlLoading(view: WebView, url: String): Boolean = {
      val isCallbackURL = url.startsWith("http://localhost/auth")
      
      url match {
        case "http://www.plurk.com/" => 
          activity.onLoginFailure(new Exception("登入失敗，使用者拒絕授權"))
          showErrorNotice("登入失敗，使用者拒絕授權")
          false
        case _ if isCallbackURL => 
          startAuth(url)
          false
        case _ => 
          super.shouldOverrideUrlLoading(view, url)
      }
    }

    override def onReceivedError(view: WebView, errorCode: Int, 
                                 description: String, failingUrl: String) {

      if (!failingUrl.startsWith("http://localhost/auth")) {
        activity.onLoginFailure(new Exception(s"登入失敗，${description}"))
      } else {
        super.onReceivedError(view, errorCode, description, failingUrl)
      }

    }

    override def onPageFinished(view: WebView, url: String) {

      val shouldShowPage = 
        !url.startsWith("http://localhost/auth") &&
        url != "http://www.plurk.com/" && url != "http://www.plurk.com/m/"

      if (shouldShowPage) {
        loadingIndicatorHolder.foreach(_.setVisibility(View.GONE))
      } else {
        super.onPageFinished(view, url)
      }
    }

    def startAuth(url: String) {

      val uri = Uri.parse(url)
      val code = uri.getQueryParameter("oauth_verifier")
      val authStatusFuture = future { plurkAPI.authorize(code).get }

      loadingIndicatorHolder.foreach(_.setVisibility(View.VISIBLE))

      authStatusFuture.onSuccessInUI{ _ => 
        PlurkAPIHelper.saveAccessToken(activity)
        activity.onLoginSuccess()
      }

      authStatusFuture.onFailureInUI{ case e: Exception => 
        showErrorNotice("無法正確登入噗浪")
        activity.onLoginFailure(e)
      }
    }
  }
}

