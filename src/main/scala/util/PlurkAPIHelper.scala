package idv.brianhsu.maidroid.plurk.util

import org.bone.soplurk.api.PlurkAPI

object PlurkAPIHelper {

  val plurkAPI = PlurkAPI.withCallback(
    appKey = "6T7KUTeSbwha", 
    appSecret = "AZIpUPdkTARzbDmdKBsu4kpxhHUJ3eWX", 
    callbackURL = "http://localhost/auth"
  )

  def getPlurkAPI = plurkAPI
}
