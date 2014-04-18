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

  private lazy val webView = getView.findView(TR.fragmentLoginWebView)

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

  def setVisibility(visibility: Int) {
    Option(getView).foreach(_.setVisibility(visibility))
  }

  def plurkAuthWebViewClient = new WebViewClient() {

    override def shouldOverrideUrlLoading(view: WebView, url: String): Boolean = {
      val isCallbackURL = url.startsWith("http://localhost/auth")
      
      DebugLog("====> shouldOverrideUrlLoading.url:" + url)

      url match {
        case "http://www.plurk.com/" => activityCallback.onLoginFailure(new Exception("登入失敗，使用者拒絕授權")) ; false
        case _ if isCallbackURL => startAuth(url); false
        case _ => true
      }
    }

    override def onReceivedError(view: WebView, errorCode: Int, 
                                 description: String, failingUrl: String) {

      if (!failingUrl.startsWith("http://localhost/auth")) {
        activityCallback.onLoginFailure(new Exception(s"登入失敗，${description}"))
      }

    }

    override def onPageFinished(view: WebView, url: String) {
      DebugLog("====> onPageFinished:" + url)
      val shouldShowPage = 
        !url.startsWith("http://localhost/auth") &&
        url != "http://www.plurk.com/" && url != "http://www.plurk.com/m/"

      if (shouldShowPage) {
        activityCallback.onShowAuthorizationPage(url)
        setVisibility(View.VISIBLE)
      }
    }

    def startAuth(url: String) {

      setVisibility(View.GONE)

      val uri = Uri.parse(url)
      val code = uri.getQueryParameter("oauth_verifier")
      val authStatusFuture = future { 
        plurkAPI.authorize(code).get
      }

      authStatusFuture.onSuccessInUI{ _ => 
        setVisibility(View.GONE)
        activityCallback.onLoginSuccess() 
      }

      authStatusFuture.onFailureInUI{ case e: Exception => activityCallback.onLoginFailure(e) }

    }
  }

}

