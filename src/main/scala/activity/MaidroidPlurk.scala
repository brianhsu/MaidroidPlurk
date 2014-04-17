package idv.brianhsu.maidroid.plurk.activity

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.net.Uri
import android.webkit.WebViewClient
import android.webkit.WebView
import android.support.v7.app.ActionBarActivity

import idv.brianhsu.maidroid.plurk._
import idv.brianhsu.maidroid.plurk.util._
import idv.brianhsu.maidroid.ui.model._
import idv.brianhsu.maidroid.ui.util.AsyncUI._

import org.bone.soplurk.api._
import scala.concurrent._

class MaidroidPlurk extends ActionBarActivity with TypedViewHolder
{
  implicit val activity = this

  private lazy val dialogFrame = findView(TR.dialogFrame)
  private lazy val webView = findView(TR.activityMaidroidPlurkWebView)
  private lazy val loadingIndicator = findView(TR.moduleLoadingIndicator)

  private lazy val plurkAPI = PlurkAPI.withCallback(
    appKey = "6T7KUTeSbwha", 
    appSecret = "AZIpUPdkTARzbDmdKBsu4kpxhHUJ3eWX", 
    callbackURL = "http://localhost/auth"
  )

  private lazy val authorizationURL: Future[String] = future {
    val authURL = plurkAPI.getAuthorizationURL
    DebugLog("Getting Plurk authorization URL..." + authURL)
    authURL.get
  }.map(_.replace("OAuth", "m"))

  private def showErrorMessage(message: String, cause: Throwable) {
    val errorMessageView = findView(TR.moduleErrorMessage)
    val errorMessageText = findView(TR.moduleErrorMessageText)
    errorMessageText.setText(s"$message，原因：${cause.getMessage}")
    errorMessageView.setVisibility(View.VISIBLE)
    loadingIndicator.setVisibility(View.GONE)
    webView.setVisibility(View.GONE)
  }

  override def onCreate(savedInstanceState: Bundle) {

    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_maidroid_plurk)
    webView.setWebViewClient(plurkAuthWebViewClient)

    dialogFrame.setMessages(
      Message(MaidMaro.Half.Smile, "歡迎來到 Hello World", None) :: 
      Message(MaidMaro.Half.Angry, "ABCDEFG", None) :: Nil
    )

    authorizationURL.onFailure { case error: Exception => 
      this.runOnUIThread {
        DebugLog(error.getMessage, error) 
        showErrorMessage("無法取得噗浪登入網址", error)
      }
    }

    authorizationURL.onSuccessInUI { url =>
      DebugLog("Get Plurk authorization URL:" + url) 
      loadingIndicator.setVisibility(View.GONE)
      webView.setVisibility(View.VISIBLE)
      webView.getSettings.setJavaScriptEnabled(true)
      webView.loadUrl(url)
    }

  }

  val plurkAuthWebViewClient = new WebViewClient() {

    override def shouldOverrideUrlLoading(view: WebView, url: String): Boolean = {
      url.startsWith("http://localhost/auth") match {
        case true => startAuth(url); false
        case false => true
      }
    }

    def startAuth(url: String) {
      val uri = Uri.parse(url)
      val code = uri.getQueryParameter("oauth_verifier")
      val authStatusFuture = future {
        val authStatus = plurkAPI.authorize(code)
        DebugLog("status:" + authStatus)
      }

      webView.setVisibility(View.GONE)
      authStatusFuture.onSuccessInUI { status => 
        dialogFrame.setMessages(
          Message(MaidMaro.Half.Happy, "看起來是沒有什麼問題呢，已經登入噗浪囉。", None) :: 
          Nil
        )
      }
    }


  }
}
