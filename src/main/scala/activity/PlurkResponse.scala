package idv.brianhsu.maidroid.plurk.activity

import idv.brianhsu.maidroid.plurk._
import idv.brianhsu.maidroid.plurk.fragment._
import org.bone.soplurk.model._

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.support.v7.app.ActionBarActivity

object PlurkResponse {
  var plurk: Plurk = _
  var user: User = _
}

class PlurkResponse extends ActionBarActivity with TypedViewHolder with ErrorNotice.Listener
{

  private lazy val dialogFrame = findView(TR.activityPlurkResponseDialogFrame)
  private lazy val fragmentContainer = findView(TR.activityPlurkResponseFragmentContainer)
  private lazy val errorNoticeFragment = getSupportFragmentManager().findFragmentById(R.id.activityPlurkResponseErrorNotice).asInstanceOf[ErrorNotice]

  override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_plurk_response)

    val fragment = new ResponseList(PlurkResponse.plurk, PlurkResponse.user)

    errorNoticeFragment.setVisibility(View.GONE)

    getSupportFragmentManager.
      beginTransaction.
      replace(R.id.activityPlurkResponseFragmentContainer, fragment).
      commit()

  }

  override def onHideOtherUI() {
    fragmentContainer.setVisibility(View.GONE)
  }



}
