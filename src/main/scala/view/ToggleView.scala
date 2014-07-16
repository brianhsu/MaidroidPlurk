package idv.brianhsu.maidroid.plurk.view

import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.animation.Animator
import android.animation.Animator._

trait ToggleView {

  private def fadeOut(view: View) {

    val anim = new AlphaAnimation(1.0f, 0.0f)
    anim.setDuration(200)
    anim.setRepeatCount(0)
    anim.setRepeatMode(Animation.REVERSE)
    anim.setAnimationListener(new Animation.AnimationListener() {
      override def onAnimationRepeat(animation: Animation) {}
      override def onAnimationStart(animation: Animation) {}
      override def onAnimationEnd(animation: Animation) {
        view.setVisibility(View.GONE)
      }
    })

    view.startAnimation(anim)
  }

  private def fadeIn(view: View) {
    val anim = new AlphaAnimation(0.0f, 1.0f);
    anim.setDuration(200)
    anim.setRepeatCount(0)
    anim.setRepeatMode(Animation.REVERSE)
    anim.setAnimationListener(new Animation.AnimationListener() {
      override def onAnimationRepeat(animation: Animation) {}
      override def onAnimationStart(animation: Animation) {}
      override def onAnimationEnd(animation: Animation) {
        view.setVisibility(View.VISIBLE)
      }
    })

    view.startAnimation(anim)
  }

  def toggleView(view: View) {

    view.getVisibility match {
      case View.VISIBLE => fadeOut(view)
      case _ => fadeIn(view)
    }
  }

}

