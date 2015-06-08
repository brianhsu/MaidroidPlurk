package idv.brianhsu.maidroid.plurk.activity

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.ActionBarActivity
import android.view.Menu
import android.view.MenuItem
import idv.brianhsu.maidroid.plurk._
import idv.brianhsu.maidroid.plurk.adapter._
import idv.brianhsu.maidroid.plurk.dialog._
import idv.brianhsu.maidroid.plurk.fragment._
import idv.brianhsu.maidroid.plurk.TypedResource._
import idv.brianhsu.maidroid.plurk.util.Logout
import idv.brianhsu.maidroid.plurk.view.ToggleView
import idv.brianhsu.maidroid.plurk.view.PlurkView
import idv.brianhsu.maidroid.ui.model._
import idv.brianhsu.maidroid.ui.util.CallbackConversions._
import org.bone.soplurk.model.User
import org.bone.soplurk.model.Plurk
import org.bone.soplurk.api.PlurkAPI.Timeline


object CurrentUserActivity {
  def startActivity(context: Context) {
    val intent = new Intent(context, classOf[CurrentUserActivity])
    context.startActivity(intent)
  }

}

class CurrentUserActivity extends ActionBarActivity 
                           with TypedViewHolder 
{
  /*
  private lazy val viewPager = findView(TR.activityUserTimelineViewPager)
  private lazy val pagerIndicator = findView(TR.activityUserTimelinePagerIndicator)
  private lazy val pageAdapter = new UserTimelineAdapter(this, getSupportFragmentManager, userID)
  */

  override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_current_user)
  }
}
