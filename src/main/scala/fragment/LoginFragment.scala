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
  private lazy val loadingIndicatorHolder = Option(getView).map(_.findView(TR.fragmentLoginLoadingIndicator))
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
    loadingIndicatorHolder.foreach(_.hide())
    errorNoticeHolder.foreach(_.setVisibility(View.VISIBLE))
    errorNoticeHolder.foreach { errorNotice =>
      errorNotice.setMessageWithRetry(message) { retryButton =>
        retryButton.setEnabled(false)
        errorNoticeHolder.foreach(_.setVisibility(View.GONE))
        loadingIndicatorHolder.foreach(_.show())
        startAuthorization()
      }
    }
  }

  private def startAuthorization() {

    webViewHolder.foreach(_.setWebViewClient(plurkAuthWebViewClient))

    val authorizationURL: Future[String] = Future {
      plurkAPI.getAuthorizationURL.get.replace("OAuth", "m")
    }

    authorizationURL.onFailureInUI { 
      case error: Exception => {
        DebugLog("====> authorizationURL.onFailureInUI:" + error.getMessage, error) 
        if (isAdded) {
          activity.onGetAuthURLFailure(error)
          showErrorNotice(getString(R.string.fragmentLoginGetAuthURLFail))
        }
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
          val message = getString(R.string.fragmentLoginAuthRefused)
          activity.onLoginFailure(new Exception(message))
          showErrorNotice(message)
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
        val message = getString(R.string.fragmentLoginFailed).format(description)
        activity.onLoginFailure(new Exception(message))
      } else {
        super.onReceivedError(view, errorCode, description, failingUrl)
      }

    }

    override def onPageFinished(view: WebView, url: String) {

      val shouldShowPage = 
        !url.startsWith("http://localhost/auth") &&
        url != "http://www.plurk.com/" && url != "http://www.plurk.com/m/"

      if (shouldShowPage) {
        loadingIndicatorHolder.foreach(_.hide())
      } else {
        super.onPageFinished(view, url)
      }
    }

    def startAuth(url: String) {

      val uri = Uri.parse(url)
      val code = uri.getQueryParameter("oauth_verifier")
      val loggedInUserIDFuture = Future { 
        plurkAPI.authorize(code).get 
        val (currentUser, pageTitle, about) = plurkAPI.Users.currUser.get
        currentUser.basicInfo.id
      }

      loadingIndicatorHolder.foreach(_.show())

      loggedInUserIDFuture.onSuccessInUI{ userID => 
        PlurkAPIHelper.saveAccessToken(activity, userID)
        if (isAdded) {
          activity.onLoginSuccess()
        }
      }

      loggedInUserIDFuture.onFailureInUI{ case e: Exception => 
        if (isAdded) {
          activity.onLoginFailure(e)
          showErrorNotice(getString(R.string.fragmentLoginFailedGeneric))
        }
      }
    }
  }
}

