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

import org.bone.soplurk.api._
import scala.concurrent._

object LoginFragment {
  trait Listener {
    def onShowAuthorizationPage(url: String): Unit
    def onGetAuthURLFailure(error: Exception): Unit
    def onLoginFailure(error: Exception): Unit
    def onLoginSuccess(): Unit
  }
}

class LoginFragment extends Fragment {

  private implicit def activity = getActivity
  private def plurkAPI = PlurkAPIHelper.getPlurkAPI

  private lazy val webView = getView.findView(TR.fragmentLoginWebView)
  private lazy val loadingIndicator = getView.findView(TR.moduleLoadingIndicator)
  private lazy val activityCallback = activity.asInstanceOf[LoginFragment.Listener]

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, 
                            savedInstanceState: Bundle): View = {
    inflater.inflate(R.layout.fragment_login, container, false)
  }

  override def onViewCreated(view: View, savedInstatnceState: Bundle) {
    startAuthorization()
  }

  def startAuthorization() {

    webView.setWebViewClient(plurkAuthWebViewClient)

    val authorizationURL: Future[String] = future {
      plurkAPI.getAuthorizationURL.get.replace("OAuth", "m")
    }

    authorizationURL.onFailureInUI { 
      case error: Exception => {
        DebugLog("====> authorizationURL.onFailureInUI:" + error.getMessage, error) 
        activityCallback.onGetAuthURLFailure(error)
      }
    }

    authorizationURL.onSuccessInUI { url =>
      webView.getSettings.setJavaScriptEnabled(true)
      webView.loadUrl(url)
    }
  }

  def plurkAuthWebViewClient = new WebViewClient() {

    override def shouldOverrideUrlLoading(view: WebView, url: String): Boolean = {
      val isCallbackURL = url.startsWith("http://localhost/auth")
      
      url match {
        case "http://www.plurk.com/" => activityCallback.onLoginFailure(new Exception("登入失敗，使用者拒絕授權")) ; false
        case _ if isCallbackURL => startAuth(url); false
        case _ => super.shouldOverrideUrlLoading(view, url)
      }
    }

    override def onReceivedError(view: WebView, errorCode: Int, 
                                 description: String, failingUrl: String) {

      if (!failingUrl.startsWith("http://localhost/auth")) {
        activityCallback.onLoginFailure(new Exception(s"登入失敗，${description}"))
      } else {
        super.onReceivedError(view, errorCode, description, failingUrl)
      }

    }

    override def onPageFinished(view: WebView, url: String) {

      val shouldShowPage = 
        !url.startsWith("http://localhost/auth") &&
        url != "http://www.plurk.com/" && url != "http://www.plurk.com/m/"

      if (shouldShowPage) {
        activityCallback.onShowAuthorizationPage(url)
        loadingIndicator.setVisibility(View.GONE)
      } else {
        loadingIndicator.setVisibility(View.VISIBLE)
        super.onPageFinished(view, url)
      }
    }

    def startAuth(url: String) {

      val uri = Uri.parse(url)
      val code = uri.getQueryParameter("oauth_verifier")
      val authStatusFuture = future { plurkAPI.authorize(code).get }

      loadingIndicator.setVisibility(View.VISIBLE)

      authStatusFuture.onSuccessInUI{ _ => 
        activityCallback.onLoginSuccess() 
      }

      authStatusFuture.onFailureInUI{ case e: Exception => activityCallback.onLoginFailure(e) }
    }
  }
}

