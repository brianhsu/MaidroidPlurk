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

object Login {
  trait Listener {
    def onGetPlurkAPI: PlurkAPI
    def onShowAuthorizationPage(url: String): Unit
    def onGetAuthURLFailure(error: Exception): Unit
    def onLoginFailure(error: Exception): Unit
    def onLoginSuccess(): Unit
  }
}

class Login extends Fragment {

  private implicit def activity = getActivity
  private var activityCallback: Login.Listener = _
  private def plurkAPI = activityCallback.onGetPlurkAPI

  private lazy val webView = getView.findView(TR.activityMaidroidPlurkWebView)

  private lazy val authorizationURL: Future[String] = future {
    plurkAPI.getAuthorizationURL.map(_.replace("OAuth", "m")).get
  }

  override def onAttach(activity: Activity) {
    super.onAttach(activity)
    try {
      activityCallback = activity.asInstanceOf[Login.Listener]
    } catch {
      case e: ClassCastException => throw new ClassCastException(s"${activity} must mixed with Login.Listener")
    }
  }

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, 
                            savedInstanceState: Bundle): View = {
    inflater.inflate(R.layout.fragment_login, container, false)
  }

  def startAuthorization() {

    webView.setWebViewClient(plurkAuthWebViewClient)

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

  def setVisibility(visibility: Int) {
    Option(getView).foreach(_.setVisibility(visibility))
  }

  val plurkAuthWebViewClient = new WebViewClient() {

    override def shouldOverrideUrlLoading(view: WebView, url: String): Boolean = {
      val isCallbackURL = url.startsWith("http://localhost/auth")
      val isAuthorizationURL = url.startsWith("http://plurk.com/m/authorize")
      
      DebugLog("====> shouldOverrideUrlLoading.url:" + url)

      url match {
        case _ if isCallbackURL => startAuth(url); false
        case _ if isAuthorizationURL => true
        case _ => activityCallback.onLoginFailure(new Exception("登入失敗，使用者拒絕授權")) ; false
      }
    }

    override def onPageFinished(view: WebView, url: String) {
      DebugLog("====> onPageFinished:" + url)
      if (url.startsWith("http://www.plurk.com/m/authorize")) {
        activityCallback.onShowAuthorizationPage(url)
        webView.setVisibility(View.VISIBLE)
      }
    }

    def startAuth(url: String) {

      webView.setVisibility(View.GONE)

      val uri = Uri.parse(url)
      val code = uri.getQueryParameter("oauth_verifier")
      val authStatusFuture = future { 
        plurkAPI.authorize(code).get
      }

      authStatusFuture.onSuccessInUI{ _ => 
        webView.setVisibility(View.GONE)
        activityCallback.onLoginSuccess() 
      }

      authStatusFuture.onFailureInUI{ case e: Exception => activityCallback.onLoginFailure(e) }

    }
  }

}

