package idv.brianhsu.maidroid.plurk.util

import org.bone.soplurk.api.PlurkAPI
import android.content.Context
import org.scribe.model.Token

object PlurkAPIHelper {

  private var plurkAPIHolder: Option[PlurkAPI] = None
  
  private def savedAccessToken(context: Context) = {
    val preference = context.getSharedPreferences("AccessToken", Context.MODE_PRIVATE)
    for {
      token <- Option(preference.getString("token", null))
      secret <- Option(preference.getString("secret", null))
    } yield { new Token(token, secret) }
  }


  private def createNewPlurkAPI(context: Context) = {
    savedAccessToken(context) match {
      case Some(token) =>
        Some(PlurkAPI.withAccessToken("6T7KUTeSbwha", "AZIpUPdkTARzbDmdKBsu4kpxhHUJ3eWX", token.getToken, token.getSecret))
      case None =>
        Some(PlurkAPI.withCallback("6T7KUTeSbwha", "AZIpUPdkTARzbDmdKBsu4kpxhHUJ3eWX", "http://localhost/auth"))
    }
  }


  def getNewPlurkAPI = {
    plurkAPIHolder = Some(PlurkAPI.withCallback("6T7KUTeSbwha", "AZIpUPdkTARzbDmdKBsu4kpxhHUJ3eWX", "http://localhost/auth"))
    plurkAPIHolder.get
  }

  def getPlurkAPI(context: Context) = {
    plurkAPIHolder match {
      case Some(plurkAPI) => plurkAPI
      case None => 
        plurkAPIHolder = createNewPlurkAPI(context)
        plurkAPIHolder.get
    }
  }

  def isLoggedIn(context: Context) = savedAccessToken(context).isDefined

  def logout(context: Context) {

    plurkAPIHolder = None

    val preferenceEditor = 
      context.getSharedPreferences("AccessToken", Context.MODE_PRIVATE).edit

    preferenceEditor.clear()
    preferenceEditor.commit()

  }

  def saveAccessToken(context: Context) {
    val preferenceEditor = 
      context.getSharedPreferences("AccessToken", Context.MODE_PRIVATE).edit
    
    for {
      plurkAPI <- plurkAPIHolder
      accessToken <- plurkAPI.getAccessToken
    } {
      preferenceEditor.putString("token", accessToken.getToken)
      preferenceEditor.putString("secret", accessToken.getSecret)
      preferenceEditor.commit()
    }
    
  }
}
