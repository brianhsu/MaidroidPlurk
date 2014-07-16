package idv.brianhsu.maidroid.plurk.view

import idv.brianhsu.maidroid.plurk.R
import android.content.Context
import idv.brianhsu.maidroid.ui.model._

import idv.brianhsu.maidroid.ui.DialogFrame
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.animation.Animator
import android.animation.Animator._

object ToggleView {

  private var haveTouched = 0

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
    view.setVisibility(View.INVISIBLE)

    anim.setAnimationListener(new Animation.AnimationListener() {
      override def onAnimationRepeat(animation: Animation) {}
      override def onAnimationStart(animation: Animation) {}
      override def onAnimationEnd(animation: Animation) {
        view.setVisibility(View.VISIBLE)
      }
    })

    view.startAnimation(anim)
  }

  def apply(view: View) {

    view.getVisibility match {
      case View.VISIBLE => fadeOut(view)
      case _ => fadeIn(view)
    }
  }

  def setupAngryBehavior(context: Context, dialogFrame: DialogFrame): DialogFrame = {
    dialogFrame.setOnImageClick { image =>
      haveTouched match {
        case 0 => 
          dialogFrame.setMessages(Message(MaidMaro.Half.Smile, context.getResources.getString(R.string.maidTouched0)):: Nil) 
          haveTouched += 1
        case 1 => 
          dialogFrame.setMessages(Message(MaidMaro.Half.Normal, context.getResources.getString(R.string.maidTouched1)):: Nil)
          haveTouched += 1
        case 2 => 
          dialogFrame.setMessages(Message(MaidMaro.Half.Panic, context.getResources.getString(R.string.maidTouched2)):: Nil)
          haveTouched += 1
        case 3 => 
          dialogFrame.setMessages(Message(MaidMaro.Half.Angry, context.getResources.getString(R.string.maidTouched3)):: Nil)
          haveTouched += 1
        case 4 =>
          apply(dialogFrame)
          haveTouched = 0
          dialogFrame.setMessages(Message(MaidMaro.Half.Normal, context.getResources.getString(R.string.maidTouched4)):: Nil)

      }
    }
    dialogFrame
  }

}

