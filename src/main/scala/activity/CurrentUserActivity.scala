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
                          with ConfirmDialog.Listener
                          with TypedViewHolder 
{
  private lazy val viewPager = findView(TR.activityCurrentUserViewPager)
  private lazy val pagerIndicator = findView(TR.activityCurrentUserPagerIndicator)
  private lazy val pageAdapter = new CurrentUserAdapter(this, getSupportFragmentManager)
  private lazy val dialogFrame = ToggleView.setupAngryBehavior(this, findView(TR.activityCurrentUserDialogFrame))


  override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_current_user)
    viewPager.setAdapter(pageAdapter)
    pagerIndicator.setViewPager(viewPager)
    dialogFrame.setMessages(
      Message(MaidMaro.Half.Smile, getString(R.string.activityCurrentUserWelcome01)) ::
      Nil
    )

  }

  override def onResume() {
    super.onResume()
    ToggleView.syncDialogVisibility(this, dialogFrame)
  }

  override def onCreateOptionsMenu(menu: Menu): Boolean = {
    val inflater = getMenuInflater
    inflater.inflate(R.menu.activity_current_user, menu)
    super.onCreateOptionsMenu(menu)
  }

  override def onOptionsItemSelected(menuItem: MenuItem): Boolean = menuItem.getItemId match {
    case R.id.activityCurrentUserLogout => Logout.logout(this); false
    case R.id.activityCurrentUserAbout => AboutActivity.startActivity(this); false
    case R.id.activityCurrentUserToggleMaid => ToggleView(this, dialogFrame); false
    case _ => super.onOptionsItemSelected(menuItem)
  }

  override def onDialogOKClicked(dialogName: Symbol, dialog: DialogInterface, data: Bundle) {
    dialogName match {
      case 'LogoutConfirm => 
        dialog.dismiss()
        this.finish()
        Logout.doLogout(this)
    }
  }


}
