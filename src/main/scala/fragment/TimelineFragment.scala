package idv.brianhsu.maidroid.plurk.fragment

import idv.brianhsu.maidroid.plurk._
import idv.brianhsu.maidroid.plurk.adapter._
import idv.brianhsu.maidroid.plurk.cache._
import idv.brianhsu.maidroid.plurk.util._
import idv.brianhsu.maidroid.plurk.view._
import idv.brianhsu.maidroid.plurk.TypedResource._
import idv.brianhsu.maidroid.ui.util.AsyncUI._
import idv.brianhsu.maidroid.ui.util.CallbackConversions._
import org.bone.soplurk.api.PlurkAPI.Timeline

import org.bone.soplurk.api.PlurkAPI.Timeline
import org.bone.soplurk.model.Plurk
import org.bone.soplurk.model.User

import java.util.Date
import org.bone.soplurk.constant.Filter
import org.bone.soplurk.constant.Filter._


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
import android.widget.Button
import android.support.v4.view.MenuItemCompat

import uk.co.senab.actionbarpulltorefresh.library.ActionBarPullToRefresh
import uk.co.senab.actionbarpulltorefresh.library.Options
import uk.co.senab.actionbarpulltorefresh.library.listeners.OnRefreshListener

import scala.concurrent._

object TimelineFragment {
  trait Listener {
    def onPlurkSelected(plurk: Plurk, owner: User): Unit
    def onShowTimelinePlurksFailure(e: Exception): Unit
    def onShowTimelinePlurksSuccess(timeline: Timeline, isNewFilter: Boolean, filter: Option[Filter], isOnlyUnread: Boolean): Unit
    def onRefreshTimelineSuccess(newTimeline: Timeline): Unit
    def onRefreshTimelineFailure(e: Exception): Unit
  }
}

class TimelineFragment extends Fragment {

  private implicit def activity = getActivity
  private def plurkAPI = PlurkAPIHelper.getPlurkAPI

  private lazy val activityCallback = activity.asInstanceOf[TimelineFragment.Listener]

  private lazy val listView = getView.findView(TR.fragmentTimelineListView)
  private lazy val pullToRefresh = getView.findView(TR.fragementTimelinePullToRefresh)
  private lazy val loadingIndicator = getView.findView(TR.moduleLoadingIndicator)
  private lazy val errorNotice = getView.findView(TR.fragmentTimelineErrorNotice)
  private lazy val loadMoreFooter = new LoadMoreFooter(activity)

  private var isLoadingMore = false
  private var hasMoreItem = true

  private var adapterHolder: Option[PlurkAdapter] = None
  private var toggleButtonHolder: Option[MenuItem] = None
  private var filterButtonHolder: Option[MenuItem] = None
  private var filterButtonMap: Map[String, MenuItem] = Map()

  // To avoid race condition, only update listView if adapterVersion is newer than current one.
  private var adapterVersion: Int = 0  
  private var isUnreadOnly = false
  private var plurkFilter: Option[Filter] = None

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, 
                            savedInstanceState: Bundle): View = {
    setHasOptionsMenu(true)
    inflater.inflate(R.layout.fragment_timeline_plurks, container, false)
  }

  override def onViewCreated(view: View, savedInstanceState: Bundle) {

    listView.setEmptyView(view.findView(TR.fragmentTimelineEmptyNotice))
    listView.addFooterView(loadMoreFooter)

    updateListAdapter()
    listView.setOnScrollListener(new OnScrollListener() {

      def onScrollStateChanged(view: AbsListView, scrollState: Int) {}
      def onScroll(view: AbsListView, firstVisibleItem: Int, 
                   visibleItemCount: Int, totalItemCount: Int) {

        val isLastItem = (firstVisibleItem + visibleItemCount) == totalItemCount
        val shouldLoadingMore = isLastItem && !isLoadingMore && hasMoreItem

        if (shouldLoadingMore) {
          loadMoreFooter.setStatus(LoadMoreFooter.Status.Loading)
          loadingMoreItem()
        }
      }
    })

    loadMoreFooter.setOnRetryClickListener { retryButton: Button => loadingMoreItem() }

    setupPullToRefresh()
    updateTimeline()
  }

  override def onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
    val actionMenu = inflater.inflate(R.menu.timeline, menu)
    this.toggleButtonHolder = Option(menu.findItem(R.id.timelineActionToggleUnreadOnly))
    this.filterButtonHolder = Option(menu.findItem(R.id.timelineActionFilter))
    this.filterButtonMap = Map(
      "all" -> menu.findItem(R.id.timelineActionAll),
      "my" -> menu.findItem(R.id.timelineActionMine),
      "private" -> menu.findItem(R.id.timelineActionPrivate),
      "responded" -> menu.findItem(R.id.timelineActionResponded),
      "favorite" -> menu.findItem(R.id.timelineActionFavorite)
    )
    actionMenu
  }

  override def onResume() {
    adapterHolder.foreach(_.notifyDataSetChanged())
    super.onResume()
  }

  private def showErrorNotice(message: String) {
    loadingIndicator.setVisibility(View.GONE)
    errorNotice.setVisibility(View.VISIBLE)
    errorNotice.setMessageWithRetry(message) { retryButton =>
      retryButton.setEnabled(false)
      errorNotice.setVisibility(View.GONE)
      loadingIndicator.setVisibility(View.VISIBLE)
      updateTimeline()
    }
  }

  private def setupPullToRefresh() {
    val options = Options.create.refreshOnUp(true).scrollDistance(0.3f).noMinimize().build()
    val onRefresh = new OnRefreshListener() {
      override def onRefreshStarted(view: View) {

        val newTimelineFuture = future { refreshTimeline(adapterHolder.get.firstPlurkShow) }

        newTimelineFuture.onFailureInUI {
          case e: Exception => 
            DebugLog(s"error: $e", e)
            activityCallback.onRefreshTimelineFailure(e)
            pullToRefresh.setRefreshComplete()
        }

        newTimelineFuture.onSuccessInUI { newTimeline: Timeline => 
          adapterHolder.foreach(_.prependTimeline(newTimeline))
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

    val olderTimelineFuture = future { getPlurks(offset = adapterHolder.map(_.lastPlurkDate)) }

    olderTimelineFuture.onSuccessInUI { timeline => 
      adapterHolder.foreach(_.appendTimeline(timeline))
      loadMoreFooter.setStatus(LoadMoreFooter.Status.Loaded)
      this.hasMoreItem = !timeline.plurks.isEmpty
      this.isLoadingMore = false
    }

    olderTimelineFuture.onFailureInUI { case e: Exception => 
      loadMoreFooter.setStatus(LoadMoreFooter.Status.Failed)
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

    new Timeline(
      newUsers,
      newPlurks.takeWhile(_.posted.getTime > latestTimestamp.getOrElse(0L))
    )
  }

  private def getPlurks(offset: Option[Date] = None) = {
    isUnreadOnly match {
      case true  => plurkAPI.Timeline.getUnreadPlurks(filter = plurkFilter, offset = offset).get
      case false => plurkAPI.Timeline.getPlurks(filter = plurkFilter, offset = offset).get
    }
  }

  private def updateListAdapter() {
    this.adapterHolder = Some(new PlurkAdapter(activity, false, Some(activityCallback.onPlurkSelected _)))
    this.adapterHolder.foreach(this.listView.setAdapter)
  }

  override def onPrepareOptionsMenu(menu: Menu) {
    updateFilterMark()
  }

  private def updateFilterMark() {

    val menuKey = plurkFilter.map(_.unreadWord).getOrElse("all")
    val menuItem = filterButtonMap.get(menuKey)

    filterButtonMap.values.foreach { menuItem => menuItem.setIcon(null) }
    menuItem.foreach(_.setIcon(android.R.drawable.ic_menu_view))
  }

  private def switchToFilter(filter: Option[Filter], isUnreadOnly: Boolean) = {

    filterButtonHolder.foreach { _.setEnabled(false) }
    toggleButtonHolder.foreach { button =>
      button.setEnabled(false)
      MenuItemCompat.setActionView(button, R.layout.action_bar_loading)
    }

    this.plurkFilter = filter
    this.isUnreadOnly = isUnreadOnly

    updateFilterMark()
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

  def updateTimeline(isNewFilter: Boolean = false) {

    this.isLoadingMore = false
    this.hasMoreItem = true
    this.loadMoreFooter.setStatus(LoadMoreFooter.Status.Idle)

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

        adapterHolder.foreach(_.appendTimeline(timeline))
        activityCallback.onShowTimelinePlurksSuccess(timeline, isNewFilter, plurkFilter, isUnreadOnly)
        filterButtonHolder.foreach { _.setEnabled(true) }
        toggleButtonHolder.foreach { button =>
          button.setEnabled(true)
          MenuItemCompat.setActionView(button, null)
          button.setTitle(if (isUnreadOnly) "未讀噗" else "所有噗")
        }
        loadingIndicator.setVisibility(View.GONE)
      }
    }

    plurksFuture.onFailureInUI { case e: Exception =>
      activityCallback.onShowTimelinePlurksFailure(e)
      showErrorNotice("無法讀取噗浪河道資料")
    }
  }
}

