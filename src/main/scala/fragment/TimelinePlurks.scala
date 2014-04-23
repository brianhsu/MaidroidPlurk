package idv.brianhsu.maidroid.plurk.fragment

import idv.brianhsu.maidroid.plurk._
import idv.brianhsu.maidroid.plurk.adapter._
import idv.brianhsu.maidroid.plurk.cache._
import idv.brianhsu.maidroid.plurk.util._
import idv.brianhsu.maidroid.plurk.TypedResource._
import idv.brianhsu.maidroid.ui.util.AsyncUI._
import idv.brianhsu.maidroid.ui.util.CallbackConversions._

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
import android.view.MenuItem

import uk.co.senab.actionbarpulltorefresh.library.ActionBarPullToRefresh
import uk.co.senab.actionbarpulltorefresh.library.Options
import uk.co.senab.actionbarpulltorefresh.library.listeners.OnRefreshListener

import scala.concurrent._

object TimelinePlurksFragment {
  trait Listener {
    def onShowLoadingUI(): Unit
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
  private lazy val footerProgress = getView.findView(TR.item_loading_footer_progress)
  private lazy val footerRetry = getView.findView(TR.item_loading_footer_retry)
  private lazy val footer = activity.getLayoutInflater.
                                     inflate(R.layout.item_loading_footer, null, false)

  private var isLoadingMore = false
  private var adapter: PlurkAdapter = _
  private var toggleButtonHolder: Option[MenuItem] = None
  private var filterButtonHolder: Option[MenuItem] = None

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
    DebugLog("===> onViewCreated")

    listView.setEmptyView(view.findView(TR.fragmentTimelinePlurksEmptyNotice))
    listView.addFooterView(footer)
    updateListAdapter()
    listView.setOnScrollListener(new OnScrollListener() {

      def onScrollStateChanged(view: AbsListView, scrollState: Int) {}
      def onScroll(view: AbsListView, firstVisibleItem: Int, 
                   visibleItemCount: Int, totalItemCount: Int) {

        val isLastItem = (firstVisibleItem + visibleItemCount) == totalItemCount
        val shouldLoadingMore = isLastItem && !isLoadingMore

        if (shouldLoadingMore) {
          footerRetry.setEnabled(false)
          footerRetry.setVisibility(View.GONE)
          footerProgress.setVisibility(View.VISIBLE)
          loadingMoreItem
        }
      }
    })

    footerRetry.setOnClickListener { view: View => loadingMoreItem() }

    setupPullToRefresh()
    updateTimeline()
  }

  override def onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
    val actionMenu = inflater.inflate(R.menu.timeline, menu)
    this.toggleButtonHolder = Option(menu.findItem(R.id.timelineActionToggleUnreadOnly))
    this.filterButtonHolder = Option(menu.findItem(R.id.timelineActionFilter))
    DebugLog("filterButtonHolder:" + this.filterButtonHolder)
    actionMenu
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

    footerProgress.setVisibility(View.VISIBLE)

    val olderTimelineFuture = future { getPlurks(offset = Some(adapter.lastPlurkDate)) }

    olderTimelineFuture.onSuccessInUI { timeline => 
      adapter.appendTimeline(timeline)

      footerRetry.setVisibility(View.GONE)
      footerRetry.setEnabled(true)

      timeline.plurks.isEmpty match {
        case true   => footer.setVisibility(View.GONE)
        case faslse => footer.setVisibility(View.VISIBLE)
      }

      this.isLoadingMore = false
    }

    olderTimelineFuture.onFailureInUI { case e: Exception => 
      footerProgress.setVisibility(View.GONE)
      footerRetry.setVisibility(View.VISIBLE)
      footerRetry.setEnabled(true)
      this.isLoadingMore = true
    }

  }

  
  private def refreshTimeline(latestPlurkShown: Option[Plurk]) = {

    var newTimeline = getPlurks()
    var newPlurks = newTimeline.plurks
    var newUsers = newTimeline.users
    val latestTimestamp: Option[Long] = latestPlurkShown.map(_.posted.getTime)
    def isOlderEnough = newPlurks.lastOption.map(_.posted.getTime <= (latestTimestamp getOrElse Long.MaxValue)).getOrElse(false)

    while (!isOlderEnough && !newTimeline.plurks.isEmpty) {
      newTimeline = getPlurks(offset = Some(newPlurks.last.posted))
      newPlurks ++= newTimeline.plurks
      newUsers ++= newTimeline.users
    }

    import org.bone.soplurk.api.PlurkAPI.Timeline
    new Timeline(
      newUsers,
      newPlurks.takeWhile(_.posted.getTime > latestTimestamp.getOrElse(0L))
    )
  }

  import java.util.Date
  import org.bone.soplurk.constant.Filter
  import org.bone.soplurk.constant.Filter._

  private var isUnreadOnly = false
  private var plurkFilter: Option[Filter] = None

  private def getPlurks(offset: Option[Date] = None) = {
    isUnreadOnly match {
      case true  => plurkAPI.Timeline.getUnreadPlurks(filter = plurkFilter, offset = offset).get
      case false => plurkAPI.Timeline.getPlurks(filter = plurkFilter, offset = offset).get
    }
  }

  private def updateListAdapter() {
    this.adapter = new PlurkAdapter(activity)
    this.listView.setAdapter(adapter)
  }

  private def switchToFilter(filter: Option[Filter], isUnreadOnly: Boolean) = {

    DebugLog(s"====> switchToFilter:${filter}, ${isUnreadOnly}" )
    filterButtonHolder.foreach { _.setEnabled(false) }
    toggleButtonHolder.foreach { button =>
      button.setEnabled(false)
      button.setActionView(R.layout.action_bar_loading)
    }

    this.plurkFilter = filter
    this.isUnreadOnly = isUnreadOnly
    updateTimeline(true)
    true
  }

  override def onOptionsItemSelected(item: MenuItem) = item.getItemId match {
    case R.id.timelineActionAll => switchToFilter(None, this.isUnreadOnly)
    case R.id.timelineActionMine => switchToFilter(Some(OnlyUser), this.isUnreadOnly)
    case R.id.timelineActionPrivate => switchToFilter(Some(OnlyPrivate), this.isUnreadOnly)
    case R.id.timelineActionResponded => switchToFilter(Some(OnlyResponded), this.isUnreadOnly)
    case R.id.timelineActionFavorite => switchToFilter(Some(OnlyFavorite), this.isUnreadOnly)
    case R.id.timelineActionToggleUnreadOnly => switchToFilter(plurkFilter, !this.isUnreadOnly)
    case _ => super.onOptionsItemSelected(item)
  }


  private var adapterVersion: Int = 0

  def updateTimeline(isNewFilter: Boolean = false) {

    if (isNewFilter) {
      adapterVersion += 1
    }

    val plurksFuture = future { (getPlurks(), adapterVersion) }

    plurksFuture.onSuccess { case (timeline, adapterVersion) => 
      if (adapterVersion >= this.adapterVersion) {
        timeline.users.values.foreach(AvatarCache.getAvatarBitmapFromNetwork)
      }
    }

    plurksFuture.onSuccessInUI { case (timeline, adapterVersion) => 
      if (adapterVersion >= this.adapterVersion) {

        if (isNewFilter) { 
          updateListAdapter() 
        }

        adapter.appendTimeline(timeline)
        activityCallback.onHideLoadingUI()
        activityCallback.onShowTimelinePlurksSuccess(timeline)
        filterButtonHolder.foreach { _.setEnabled(true) }
        toggleButtonHolder.foreach { button =>
          button.setEnabled(true)
          button.setActionView(null)
          button.setTitle(if (isUnreadOnly) "未讀噗" else "所有噗")
        }
      }
    }

    plurksFuture.onFailureInUI { case e: Exception =>
      activityCallback.onShowTimelinePlurksFailure(e)
    }
  }
}

