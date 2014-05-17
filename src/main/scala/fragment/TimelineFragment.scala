package idv.brianhsu.maidroid.plurk.fragment

import idv.brianhsu.maidroid.plurk._
import idv.brianhsu.maidroid.plurk.activity._
import idv.brianhsu.maidroid.plurk.dialog._
import idv.brianhsu.maidroid.plurk.adapter._
import idv.brianhsu.maidroid.plurk.activity._
import idv.brianhsu.maidroid.plurk.cache._
import idv.brianhsu.maidroid.plurk.util._
import idv.brianhsu.maidroid.plurk.view._
import idv.brianhsu.maidroid.plurk.TypedResource._
import idv.brianhsu.maidroid.ui.util.AsyncUI._
import idv.brianhsu.maidroid.ui.util.CallbackConversions._

import org.bone.soplurk.api.PlurkAPI.Timeline
import org.bone.soplurk.model.Plurk
import org.bone.soplurk.model.User
import org.bone.soplurk.constant.Filter
import org.bone.soplurk.constant.Filter._

import java.util.Date

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.content.Intent

import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.View
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.AbsListView.OnScrollListener
import android.widget.AbsListView
import android.widget.Button
import android.widget.Toast

import android.support.v4.view.MenuItemCompat
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity

import uk.co.senab.actionbarpulltorefresh.library.ActionBarPullToRefresh
import uk.co.senab.actionbarpulltorefresh.library.Options
import uk.co.senab.actionbarpulltorefresh.library.listeners.OnRefreshListener

import scala.concurrent._
import scala.util.Try

object TimelineFragment {

  private var savedTimeline: Option[Timeline] = None
  private var isUnreadOnly: Boolean = false
  private var plurkFilter: Option[Filter] = None

  var deletedPlurkIDHolder: Option[Long] = None

  trait Listener {
    def onShowTimelinePlurksFailure(e: Exception): Unit
    def onShowTimelinePlurksSuccess(timeline: Timeline, isNewFilter: Boolean, filter: Option[Filter], isOnlyUnread: Boolean): Unit
    def onRefreshTimelineSuccess(newTimeline: Timeline): Unit
    def onRefreshTimelineFailure(e: Exception): Unit
    def onDeletePlurkSuccess(): Unit
  }

  val RequestPostPlurk = 1
  val RequestEditPlurk = 2

}

class TimelineFragment extends Fragment {

  private implicit def activity = getActivity.asInstanceOf[FragmentActivity with TimelineFragment.Listener with ConfirmDialog.Listener with PlurkView.Listener]

  private def plurkAPI = PlurkAPIHelper.getPlurkAPI(activity)

  private def listViewHolder = Option(getView).map(_.findView(TR.fragmentTimelineListView))
  private def pullToRefreshHolder = Option(getView).map(_.findView(TR.fragementTimelinePullToRefresh))
  private def loadingIndicatorHolder = Option(getView).map(_.findView(TR.fragmentTimelineLoadingIndicator))
  private def errorNoticeHolder = Option(getView).map(_.findView(TR.fragmentTimelineErrorNotice))

  private var loadMoreFooter: LoadMoreFooter = _
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

    if (savedInstanceState != null) {
      this.plurkFilter = TimelineFragment.plurkFilter
      this.isUnreadOnly = TimelineFragment.isUnreadOnly
    }

    loadMoreFooter = new LoadMoreFooter(activity)
    listViewHolder.foreach(_.setEmptyView(view.findView(TR.fragmentTimelineEmptyNotice)))
    listViewHolder.foreach(_.addFooterView(loadMoreFooter))
    listViewHolder.foreach { listView =>
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
    }

    loadMoreFooter.setOnRetryClickListener { retryButton: Button => loadingMoreItem() }
    updateListAdapter()
    setupPullToRefresh()
  }

  override def onViewStateRestored (savedInstanceState: Bundle) {
    val isInError = (savedInstanceState != null && savedInstanceState.getBoolean("isInError", false))
    val isRecreate = (savedInstanceState != null)

    if (isRecreate && !isInError) {
      updateTimeline(isRecreate = true)
    } else {
      updateTimeline()
    }
    super.onViewStateRestored(savedInstanceState)
  }


  override def onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
    val actionMenu = inflater.inflate(R.menu.fragment_timeline, menu)
    this.toggleButtonHolder = Option(menu.findItem(R.id.fragmentTimelineActionToggleUnreadOnly))
    this.filterButtonHolder = Option(menu.findItem(R.id.fragmentTimelineActionFilter))
    this.filterButtonMap = Map(
      "all" -> menu.findItem(R.id.fragmentTimelineActionAll),
      "my" -> menu.findItem(R.id.fragmentTimelineActionMine),
      "private" -> menu.findItem(R.id.fragmentTimelineActionPrivate),
      "responded" -> menu.findItem(R.id.fragmentTimelineActionResponded),
      "favorite" -> menu.findItem(R.id.fragmentTimelineActionFavorite)
    )

    val aboutFromActivity = menu.findItem(R.id.activityMaidroidPlurkActionAbout)
    aboutFromActivity.setVisible(false)
    updateToggleButtonTitle(false, None)
    actionMenu
  }

  def deletePlurk(plurkID: Long) {
    adapterHolder.foreach(_.deletePlurk(plurkID))
  }

  override def onResume() {

    for {
      plurkID <- TimelineFragment.deletedPlurkIDHolder
    } {
      deletePlurk(plurkID)
      TimelineFragment.deletedPlurkIDHolder = None
      activity.onDeletePlurkSuccess()
    }

    adapterHolder.foreach { adapter => adapter.updatePlurkContent() }
    super.onResume()
  }

  override def onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    val isInError = errorNoticeHolder.map(_.getVisibility == View.VISIBLE).getOrElse(false)
    outState.putBoolean("isInError", isInError)
  }

  private def showErrorNotice(message: String) {
    loadingIndicatorHolder.foreach(_.hide())
    errorNoticeHolder.foreach(_.setVisibility(View.VISIBLE))
    errorNoticeHolder.foreach { errorNotice =>
      errorNotice.setMessageWithRetry(message) { retryButton =>
        retryButton.setEnabled(false)
        errorNoticeHolder.foreach(_.setVisibility(View.GONE))
        loadingIndicatorHolder.foreach(_.show())
        updateTimeline()
      }
    }
  }

  private def setupPullToRefresh() {
    val options = Options.create.refreshOnUp(true).scrollDistance(0.3f).noMinimize().build()
    val onRefresh = new OnRefreshListener() {
      override def onRefreshStarted(view: View) { 
        refreshTimeline() 
      }
    }

    pullToRefreshHolder.foreach { view =>
      ActionBarPullToRefresh.
        from(activity).options(options).
        allChildrenArePullable.listener(onRefresh).
        setup(view)
    }
  }

  private def refreshTimeline() {
    val newTimelineFuture = future { refreshTimeline(adapterHolder.get.firstPlurkShow) }

    newTimelineFuture.onFailureInUI {
      case e: Exception => 
        DebugLog(s"error: $e", e)
        activity.onRefreshTimelineFailure(e)
        pullToRefreshHolder.foreach(_.setRefreshComplete())
      }

    newTimelineFuture.onSuccessInUI { newTimeline: Timeline => 
      adapterHolder.foreach(_.prependTimeline(newTimeline))
      activity.onRefreshTimelineSuccess(newTimeline)
      pullToRefreshHolder.foreach(_.setRefreshComplete())
    }
  }

  private def loadingMoreItem() {

    this.isLoadingMore = true

    val olderTimelineFuture = future { getPlurks(offset = adapterHolder.flatMap(_.lastPlurkDate)) }

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

  private def getPlurks(offset: Option[Date] = None, isRecreate: Boolean = false) = {
    val shouldRecreate = isRecreate && TimelineFragment.savedTimeline.isDefined
    isUnreadOnly match {
      case _ if shouldRecreate => TimelineFragment.savedTimeline.get
      case true  => plurkAPI.Timeline.getUnreadPlurks(filter = plurkFilter, offset = offset).get
      case false => plurkAPI.Timeline.getPlurks(filter = plurkFilter, offset = offset).get
    }
  }

  private def updateListAdapter() {
    this.adapterHolder = Some(new PlurkAdapter(activity, false))
    for {
      adapter <- this.adapterHolder
      listView <- this.listViewHolder
    } { 
      listView.setAdapter(adapter)
    }
  }

  override def onDestroy() {
    TimelineFragment.savedTimeline = adapterHolder.map(_.getTimeline)
    TimelineFragment.plurkFilter = this.plurkFilter
    TimelineFragment.isUnreadOnly = this.isUnreadOnly
    super.onDestroy()
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

  override def onLowMemory() {
    TimelineFragment.savedTimeline = None
    TimelineFragment.plurkFilter = None
    AvatarCache.clearCache()
    ImageCache.clearCache()
  }

  override def onOptionsItemSelected(item: MenuItem) = item.getItemId match {
    case R.id.fragmentTimelineActionAll => switchToFilter(None, this.isUnreadOnly)
    case R.id.fragmentTimelineActionMine => switchToFilter(Some(OnlyUser), this.isUnreadOnly)
    case R.id.fragmentTimelineActionPrivate => switchToFilter(Some(OnlyPrivate), this.isUnreadOnly)
    case R.id.fragmentTimelineActionResponded => switchToFilter(Some(OnlyResponded), this.isUnreadOnly)
    case R.id.fragmentTimelineActionFavorite => switchToFilter(Some(OnlyFavorite), this.isUnreadOnly)
    case R.id.fragmentTimelineActionToggleUnreadOnly => switchToFilter(plurkFilter, !this.isUnreadOnly)
    case R.id.fragmentTimelineActionPost => startPostPlurkActivity(); false
    case R.id.fragmentTimelineActionLogout => Logout.logout(activity); false
    case R.id.fragmentTimelineActionAbout => AboutActivity.startActivity(activity); false
    case _ => super.onOptionsItemSelected(item)
  }

  override def onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {

    requestCode match {
      case TimelineFragment.RequestPostPlurk if resultCode == Activity.RESULT_OK =>
        Toast.makeText(this.getActivity, "發噗成功", Toast.LENGTH_SHORT)
        pullToRefreshHolder.foreach { pullToRefresh =>
          pullToRefresh.setRefreshing(true)
          refreshTimeline()
        }
      case TimelineFragment.RequestEditPlurk if resultCode == Activity.RESULT_OK =>

        val plurkID = data.getLongExtra(EditPlurkActivity.PlurkIDBundle, -1)
        val newContent = data.getStringExtra(EditPlurkActivity.EditedContentBundle)
        val newContentRaw = Option(data.getStringExtra(EditPlurkActivity.EditedContentBundle))

        if (plurkID != -1) {
          adapterHolder.foreach(_.updatePlurk(plurkID, newContent, newContentRaw))
        }
      case _ =>
    }
  }

  private def startPostPlurkActivity() = {
    val intent = new Intent(activity, classOf[PostPlurkActivity])
    startActivityForResult(intent, TimelineFragment.RequestPostPlurk)
    false
  }

  def updateTimeline(isNewFilter: Boolean = false, isRecreate: Boolean = false) {

    this.isLoadingMore = false
    this.hasMoreItem = true
    this.loadMoreFooter.setStatus(LoadMoreFooter.Status.Idle)

    if (isNewFilter) {
      adapterVersion += 1
    }

    val plurksFuture = future { 
      val plurks = getPlurks(isRecreate = isRecreate)
      val unreadCounts = plurkAPI.Polling.getUnreadCount.get
      (plurks, unreadCounts, adapterVersion) 
    }

    plurksFuture.onSuccessInUI { case (timeline, unreadCounts, adapterVersion) => 

      if (adapterVersion >= this.adapterVersion) {

        if (isNewFilter) { 
          updateListAdapter() 
        }

        adapterHolder.foreach(_.appendTimeline(timeline))
        activity.onShowTimelinePlurksSuccess(timeline, isNewFilter, plurkFilter, isUnreadOnly)
        filterButtonHolder.foreach { _.setEnabled(true) }
        updateToggleButtonTitle(true, Some(unreadCounts.all).filter(_ != 0))
        loadingIndicatorHolder.foreach(_.hide())
      }
    }

    plurksFuture.onFailureInUI { case e: Exception =>
      activity.onShowTimelinePlurksFailure(e)
      showErrorNotice("無法讀取噗浪河道資料")
      updateToggleButtonTitle(false)
    }
  }

  private def updateToggleButtonEnabled(isEnabled: Boolean) {
    toggleButtonHolder.foreach { button =>
      button.setEnabled(isEnabled)
    }
  }

  private def updateToggleButtonTitle(isEnabled: Boolean, unreadCount: Option[Int] = None) {
    toggleButtonHolder.foreach { menuItem =>
      val infalter = this.getActivity.getLayoutInflater
      val button = infalter.inflate(R.layout.view_toggle_button, null).asInstanceOf[android.widget.Button]
      unreadCount match {
        case Some(count) if !isUnreadOnly => 
          button.setText(s"所有噗 ($count)")
          button.setBackgroundResource(R.drawable.rounded_red)
        case _ =>
          button.setText(if (isUnreadOnly) "未讀噗" else "所有噗")
          button.setBackgroundResource(R.drawable.rounded_blue)
      }

      if (!isEnabled) {
        button.setEnabled(false)
        button.setBackgroundResource(R.drawable.rounded_gray)
      }

      button.setOnClickListener { view: View =>
        switchToFilter(plurkFilter, !this.isUnreadOnly)
      }

      MenuItemCompat.setActionView(menuItem, button)
    }
  }

  def startEditActivity(plurk: Plurk) {
    val intent = new Intent(activity, classOf[EditPlurkActivity])
    intent.putExtra(EditPlurkActivity.PlurkIDBundle, plurk.plurkID)
    intent.putExtra(EditPlurkActivity.ContentRawBundle, plurk.contentRaw getOrElse "")
    startActivityForResult(intent, TimelineFragment.RequestEditPlurk)
  }
}

