package idv.brianhsu.maidroid.plurk.view

import idv.brianhsu.maidroid.plurk._
import idv.brianhsu.maidroid.plurk.TypedResource._

import android.content.Context
import android.widget.LinearLayout
import android.view.LayoutInflater
import android.util.AttributeSet
import android.view.View
import android.graphics.drawable.AnimationDrawable

class LoadingIndicator(context: Context, 
                       attrs: AttributeSet) extends LinearLayout(context, attrs) {

  private lazy val inflater = LayoutInflater.from(context)
  private lazy val imageView = this.findView(TR.viewLoadingIndicatorImage)
  private lazy val animation = imageView.getDrawable.asInstanceOf[AnimationDrawable]

  inflater.inflate(R.layout.view_loading_indicator, this, true)
  animation.start()

  def show() {
    animation.start()
    this.setVisibility(View.VISIBLE)
  }

  def hide() {
    this.setVisibility(View.GONE)
    animation.stop()
  }
}

