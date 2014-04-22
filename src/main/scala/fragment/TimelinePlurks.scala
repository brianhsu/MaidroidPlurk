package idv.brianhsu.maidroid.plurk.fragment

import idv.brianhsu.maidroid.plurk._
import idv.brianhsu.maidroid.plurk.adapter._
import idv.brianhsu.maidroid.plurk.util._
import idv.brianhsu.maidroid.plurk.util.QualifierDisplay
import idv.brianhsu.maidroid.plurk.TypedResource._
import idv.brianhsu.maidroid.ui.util.AsyncUI._
import idv.brianhsu.maidroid.ui.util.CallbackConversions._

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.net.Uri
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.View
import android.webkit.WebViewClient
import android.webkit.WebView
import android.view.LayoutInflater
import android.widget.ImageView

import org.bone.soplurk.api._
import org.bone.soplurk.api.PlurkAPI.Timeline
import org.bone.soplurk.model._
import scala.concurrent._
import java.net.URL
import java.net.URLConnection
import java.text.SimpleDateFormat

import scala.util.{Try, Success}
import android.text.Html
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.text.method.LinkMovementMethod
import android.content.res.Resources
import android.widget.AbsListView.OnScrollListener
import android.widget.AbsListView

object TimelinePlurksFragment {
  trait Listener {
    def onHideLoadingUI(): Unit
    def onShowTimelinePlurksFailure(e: Exception): Unit
    def onShowTimelinePlurksSuccess(timeline: Timeline): Unit
  }
}

class TimelinePlurksFragment extends Fragment {

  private implicit def activity = getActivity
  private var activityCallback: TimelinePlurksFragment.Listener = _
  private def plurkAPI = PlurkAPIHelper.getPlurkAPI

  private lazy val listView = getView.findView(TR.fragmentTimelinePlurksListView)
  private lazy val adapter = new PlurkAdapter(activity)
  private lazy val footer = activity.getLayoutInflater.
                                     inflate(R.layout.item_loading_footer, null, false)

  private var isLoadingMore: Boolean = false

  override def onAttach(activity: Activity) {
    super.onAttach(activity)
    try {
      activityCallback = activity.asInstanceOf[TimelinePlurksFragment.Listener]
    } catch {
      case e: ClassCastException => 
        throw new ClassCastException(
          s"${activity} must mixed with TimelinePlurksFragment.Listener"
        )
    }
  }

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, 
                            savedInstanceState: Bundle): View = {
    inflater.inflate(R.layout.fragment_timeline_plurks, container, false)
  }

  override def onViewCreated(view: View, savedInstanceState: Bundle) {

    listView.setEmptyView(view.findView(TR.fragmentTimelinePlurksEmptyNotice))
    listView.addFooterView(footer)
    listView.setAdapter(adapter)
    listView.setOnScrollListener(new OnScrollListener() {
      def onScroll(view: AbsListView, firstVisibleItem: Int, 
                   visibleItemCount: Int, totalItemCount: Int) {

        val isLastItem = (firstVisibleItem + visibleItemCount) == totalItemCount
        val shouldLoadingMore = isLastItem && !isLoadingMore
        if (shouldLoadingMore) {
          loadingMoreItem
        }
      }

      def onScrollStateChanged(view: AbsListView, scrollState: Int) { }

    })
    updateTimeline()
  }

  def loadingMoreItem() {
    this.isLoadingMore = true

    val olderTimelineFuture = future {
      plurkAPI.Timeline.getPlurks(offset = Some(adapter.lastPlurkDate)).get
    }

    olderTimelineFuture.onSuccessInUI { timeline => 
      adapter.appendTimeline(timeline)

      timeline.plurks.isEmpty match {
        case true   => footer.setVisibility(View.GONE)
        case faslse => footer.setVisibility(View.VISIBLE)
      }

      this.isLoadingMore = false
    }

  }

  def updateTimeline() {

    val plurksFuture = future { 
      plurkAPI.Timeline.getPlurks().get 
    }

    plurksFuture.onSuccessInUI { timeline => 
      adapter.appendTimeline(timeline)
      activityCallback.onHideLoadingUI()
      activityCallback.onShowTimelinePlurksSuccess(timeline)
    }

    plurksFuture.onFailureInUI { case e: Exception =>
      activityCallback.onShowTimelinePlurksFailure(e)
    }
  }

}

