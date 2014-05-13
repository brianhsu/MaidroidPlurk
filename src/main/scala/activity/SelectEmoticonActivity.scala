package idv.brianhsu.maidroid.plurk.activity

import idv.brianhsu.maidroid.plurk.fragment._

import org.bone.soplurk.model.Icon

import android.graphics.drawable.Drawable

import android.support.v7.app.ActionBarActivity
import android.support.v4.app.FragmentTransaction

trait SelectEmoticonActivity {
  this: ActionBarActivity with EmoticonFragment.Listener =>

  protected def getCurrentEditor: PlurkEditor
  protected val emoticonFragmentHolderResID: Int

  protected def toggleEmoticonSelector() {
    val fm = getSupportFragmentManager
    val selectorHolder = Option(fm.findFragmentById(emoticonFragmentHolderResID))

    selectorHolder match {
      case Some(selector) if selector.isHidden =>
        fm.beginTransaction.
          setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN).
          show(selector).commit()

      case Some(selector)  =>
        fm.beginTransaction.
          setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN).
          hide(selector).commit()

      case None =>
        fm.beginTransaction.
          setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN).
          replace(emoticonFragmentHolderResID, new EmoticonFragment).
          commit()
    }
  }

  override def onIconSelected(icon: Icon, drawable: Option[Drawable]) {
    toggleEmoticonSelector()
    getCurrentEditor.insertIcon(icon, drawable)
  }

  def onBackPressedInEmoticonSelector(): Boolean = {
    val isEmoticonSelectedShown = Option(
      getSupportFragmentManager.findFragmentById(emoticonFragmentHolderResID)
    ).exists(_.isVisible)

    if (isEmoticonSelectedShown) {
      toggleEmoticonSelector()
      true
    } else {
      false
    }
  }

}

