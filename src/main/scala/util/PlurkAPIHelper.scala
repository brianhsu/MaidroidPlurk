package idv.brianhsu.maidroid.plurk.util

import org.bone.soplurk.api.PlurkAPI
import org.bone.soplurk.model._
import org.bone.soplurk.constant.PlurkType


import android.content.Context
import org.scribe.model.Token

object PlurkAPIHelper {

  private var plurkAPIHolder: Option[PlurkAPI] = None
  private val apiKey = "6T7KUTeSbwha"
  private val apiSecret = "AZIpUPdkTARzbDmdKBsu4kpxhHUJ3eWX"
  
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
        Some(PlurkAPI.withAccessToken(apiKey, apiSecret, token.getToken, token.getSecret))
      case None =>
        Some(PlurkAPI.withCallback(apiKey, apiSecret, "http://localhost/auth"))
    }
  }


  def getNewPlurkAPI = {
    plurkAPIHolder = Some(PlurkAPI.withCallback(apiKey, apiSecret, "http://localhost/auth"))
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

  def isMinePlurk(plurk: Plurk): Boolean = {

    val isAnonymous = {
      plurk.plurkType == PlurkType.Anonymous ||
      plurk.plurkType == PlurkType.AnonymousResponded
    }

    !isAnonymous && plurk.ownerID == plurk.userID && plurk.ownerID != 99999
  }
}
