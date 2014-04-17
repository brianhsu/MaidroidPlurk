package idv.brianhsu.maidroid.plurk.util

import android.util.Log

object DebugLog {

  val IsDebugging = true
  val Tag = "MaidroidPlurk"

  def apply(message: String) { 
    if (IsDebugging) { Log.d(Tag, message) }
  }

  def apply(message: String, throwable: Throwable) { 
    if (IsDebugging) { Log.d(Tag, message, throwable) }
  }
  
}

