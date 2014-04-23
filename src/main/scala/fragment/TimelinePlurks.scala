package idv.brianhsu.maidroid.plurk.fragment

import idv.brianhsu.maidroid.plurk._
import idv.brianhsu.maidroid.plurk.adapter._
import idv.brianhsu.maidroid.plurk.cache._
import idv.brianhsu.maidroid.plurk.util._
import idv.brianhsu.maidroid.plurk.TypedResource._
import idv.brianhsu.maidroid.ui.util.AsyncUI._

import org.bone.soplurk.api.PlurkAPI.Timeline
import org.bone.soplurk.model.Plurk

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.View
import android.widget.AbsListView.OnScrollListener
import android.widget.AbsListView
import android.view.Menu
import android.view.MenuInflater

import uk.co.senab.actionbarpulltorefresh.library.ActionBarPullToRefresh
import uk.co.senab.actionbarpulltorefresh.library.Options
import uk.co.senab.actionbarpulltorefresh.library.listeners.OnRefreshListener

import scala.concurrent._

object TimelinePlurksFragment {
  trait Listener {
    def onHideLoadingUI(): Unit
    def onShowTimelinePlurksFailure(e: Exception): Unit
    def onShowTimelinePlurksSuccess(timeline: Timeline): Unit
    def onRefreshTimelineSuccess(newTimeline: Timeline): Unit
    def onRefreshTimelineFailure(e: Exception): Unit
  }
}

class TimelinePlurksFragment extends Fragment {

  private implicit def activity = getActivity
  private var activityCallback: TimelinePlurksFragment.Listener = _
  private def plurkAPI = PlurkAPIHelper.getPlurkAPI

  private lazy val listView = getView.findView(TR.fragmentTimelinePlurksListView)
  private lazy val pullToRefresh = getView.findView(TR.fragementTimelinePullToRefresh)
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
    setHasOptionsMenu(true)
    inflater.inflate(R.layout.fragment_timeline_plurks, container, false)
  }

  override def onViewCreated(view: View, savedInstanceState: Bundle) {

    listView.setEmptyView(view.findView(TR.fragmentTimelinePlurksEmptyNotice))
    listView.addFooterView(footer)
    listView.setAdapter(adapter)
    listView.setOnScrollListener(new OnScrollListener() {

      def onScrollStateChanged(view: AbsListView, scrollState: Int) {}
      def onScroll(view: AbsListView, firstVisibleItem: Int, 
                   visibleItemCount: Int, totalItemCount: Int) {

        val isLastItem = (firstVisibleItem + visibleItemCount) == totalItemCount
        val shouldLoadingMore = isLastItem && !isLoadingMore
        if (shouldLoadingMore) {
          loadingMoreItem
        }
      }
    })

    setupPullToRefresh()
    updateTimeline()

  }

  override def onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
    inflater.inflate(R.menu.timeline, menu)
  }

  private def setupPullToRefresh() {
    val options = Options.create.refreshOnUp(true).scrollDistance(0.3f).noMinimize().build()
    val onRefresh = new OnRefreshListener() {
      override def onRefreshStarted(view: View) {

        val newTimelineFuture = future { refreshTimeline(adapter.firstPlurkShow) }

        newTimelineFuture.onFailureInUI {
          case e: Exception => 
            DebugLog(s"error: $e", e)
            activityCallback.onRefreshTimelineFailure(e)
            pullToRefresh.setRefreshComplete()
        }

        newTimelineFuture.onSuccessInUI { newTimeline: Timeline => 
          adapter.prependTimeline(newTimeline)
          activityCallback.onRefreshTimelineSuccess(newTimeline)
          pullToRefresh.setRefreshComplete()
        }
      }
    }

    ActionBarPullToRefresh.
      from(activity).options(options).
      allChildrenArePullable.listener(onRefresh).
      setup(pullToRefresh)
  }

  private def loadingMoreItem() {
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

  
  private def refreshTimeline(latestPlurkShown: Option[Plurk]) = {

    var newTimeline = plurkAPI.Timeline.getPlurks().get
    var newPlurks = newTimeline.plurks
    var newUsers = newTimeline.users
    val latestTimestamp: Option[Long] = latestPlurkShown.map(_.posted.getTime)
    def isOlderEnough = newPlurks.lastOption.map(_.posted.getTime <= (latestTimestamp getOrElse Long.MaxValue)).getOrElse(false)

    while (!isOlderEnough && !newTimeline.plurks.isEmpty) {
      newTimeline = plurkAPI.Timeline.getPlurks(offset = Some(newPlurks.last.posted)).get
      newPlurks ++= newTimeline.plurks
      newUsers ++= newTimeline.users
    }

    import org.bone.soplurk.api.PlurkAPI.Timeline
    new Timeline(
      newUsers,
      newPlurks.takeWhile(_.posted.getTime > latestTimestamp.getOrElse(0L))
    )
  }

  def updateTimeline() {

    val plurksFuture = future { 
      plurkAPI.Timeline.getPlurks().get 
    }

    plurksFuture.onSuccess { 
      case timeline => timeline.users.values.foreach(AvatarCache.getAvatarBitmapFromNetwork)
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

