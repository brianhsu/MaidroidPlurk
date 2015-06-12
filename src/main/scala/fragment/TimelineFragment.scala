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
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.content.Intent


import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.View
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.AdapterView
import android.widget.AbsListView.OnScrollListener
import android.widget.AbsListView
import android.widget.Button
import android.widget.Toast

import android.support.v4.view.MenuItemCompat
import android.support.v4.app.Fragment
import android.support.v7.app.ActionBar
import android.support.v7.app.ActionBarActivity

import uk.co.senab.actionbarpulltorefresh.library.ActionBarPullToRefresh
import uk.co.senab.actionbarpulltorefresh.library.Options
import uk.co.senab.actionbarpulltorefresh.library.listeners.OnRefreshListener

import scala.concurrent._
import scala.util.Try

import java.util.Date

object TimelineFragment {

  private var savedTimeline: Option[Timeline] = None
  private var isUnreadOnly: Boolean = false
  private var plurkFilter: Option[Filter] = None

  var deletedPlurkIDHolder: Option[Long] = None

  trait Listener {
    def onShowTimelinePlurksFailure(e: Exception): Unit
    def onShowTimelinePlurksSuccess(timeline: Timeline, isNewFilter: Boolean, filter: Option[Filter], isOnlyUnread: Boolean): Unit
    def onDeletePlurkSuccess(): Unit
  }

  val RequestPostPlurk = 1
  val RequestEditPlurk = 2

}

class TimelineFragment extends Fragment with ActionBar.OnNavigationListener {

  private implicit def activity = getActivity.asInstanceOf[ActionBarActivity with TimelineFragment.Listener with ConfirmDialog.Listener with PlurkView.Listener]

  private def plurkAPI = PlurkAPIHelper.getPlurkAPI(activity)

  private def listViewHolder = Option(getView).map(_.findView(TR.fragmentTimelineListView))
  private def pullToRefreshHolder = Option(getView).map(_.findView(TR.fragementTimelinePullToRefresh))
  private def loadingIndicatorHolder = Option(getView).map(_.findView(TR.fragmentTimelineLoadingIndicator))
  private def errorNoticeHolder = Option(getView).map(_.findView(TR.fragmentTimelineErrorNotice))

  private lazy val navigationAdapter = new FilterSpinnerAdapter(this.getActivity)

  private var loadMoreFooter: LoadMoreFooter = _
  private var isLoadingMore = false
  private var hasMoreItem = true

  private var adapterHolder: Option[PlurkAdapter] = None
  private var toggleButtonHolder: Option[MenuItem] = None

  // To avoid race condition, only update listView if adapterVersion is newer than current one.
  private var adapterVersion: Int = 0  
  private var unreadCount: Int = 0
  private var isUnreadOnly = false
  private var plurkFilter: Option[Filter] = None

  private var isInError: Boolean = false
  private var isRecreate: Boolean = false
  private var currentFilter: Option[Filter] = None

  override def onNavigationItemSelected(itemPosition: Int, itemId: Long) = {
    currentFilter = navigationAdapter.getItem(itemPosition).asInstanceOf[Option[Filter]]

    if (isRecreate && !isInError) {
      updateToggleButtonTitle(true, Some(unreadCount).filter(_ != 0))
    } else {
      switchToFilter(currentFilter, isUnreadOnly)
    }

    this.isInError = false
    this.isRecreate = false

    true
  }

  override def onCreate(savedInstanceState: Bundle) {

    super.onCreate(savedInstanceState)

    if (savedInstanceState != null) {
      this.plurkFilter = TimelineFragment.plurkFilter
      this.isUnreadOnly = TimelineFragment.isUnreadOnly
      this.unreadCount = savedInstanceState.getInt("unreadCount", 0)
    }

    this.isInError = (savedInstanceState != null && savedInstanceState.getBoolean("isInError", false))
    this.isRecreate = (savedInstanceState != null)

  }

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, 
                            savedInstanceState: Bundle): View = {
    val actionBar = activity.getSupportActionBar
    actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST)
    actionBar.setListNavigationCallbacks(navigationAdapter, this)
    actionBar.setDisplayShowTitleEnabled(false)
    setHasOptionsMenu(true)
    inflater.inflate(R.layout.fragment_timeline_plurks, container, false)
  }

  override def onViewCreated(view: View, savedInstanceState: Bundle) {

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

    if (isRecreate && !isInError) {
      updateTimeline(isRecreate = true)
    }
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
    outState.putInt("unreadCount", unreadCount)
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
        updateTimeline(true, false)
      }
    }

    pullToRefreshHolder.foreach { view =>
      ActionBarPullToRefresh.
        from(activity).options(options).
        allChildrenArePullable.listener(onRefresh).
        setup(view)
    }
  }

  private def loadingMoreItem() {

    this.isLoadingMore = true

    val olderTimelineFuture = Future { getPlurks(offset = adapterHolder.flatMap(_.lastPlurkDate)) }

    olderTimelineFuture.onSuccessInUI { timeline => 
      if (activity != null) {
        adapterHolder.foreach(_.appendTimeline(timeline))
        loadMoreFooter.setStatus(LoadMoreFooter.Status.Loaded)
        this.hasMoreItem = !timeline.plurks.isEmpty
        this.isLoadingMore = false
      }
    }

    olderTimelineFuture.onFailureInUI { case e: Exception => 
      if (activity != null) {
        loadMoreFooter.setStatus(LoadMoreFooter.Status.Failed)
        this.isLoadingMore = true
      }
    }

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

  private def switchToFilter(filter: Option[Filter], isUnreadOnly: Boolean, offset: Option[Date] = None) = {
    this.plurkFilter = filter
    this.isUnreadOnly = isUnreadOnly
    toggleButtonHolder.foreach { button =>
      button.setEnabled(false)
      MenuItemCompat.setActionView(button, R.layout.action_bar_loading)
    }
    updateTimeline(true, false, offset)
    true
  }

  override def onLowMemory() {
    TimelineFragment.savedTimeline = None
    TimelineFragment.plurkFilter = None
    AvatarCache.clearCache()
    ImageCache.clearCache()
  }

  private def showTimeMachine() {
    import android.app.DatePickerDialog
    import android.widget.DatePicker
    import java.util.Calendar
    
    val calendar = Calendar.getInstance()
    val mYear = calendar.get(Calendar.YEAR)
    val mMonth = calendar.get(Calendar.MONTH)
    val mDay = calendar.get(Calendar.DAY_OF_MONTH)
    val onDateSetListener = new DatePickerDialog.OnDateSetListener() {
      override def onDateSet(view: DatePicker, year: Int, month: Int, day: Int): Unit = {
        calendar.set(Calendar.YEAR, year)
        calendar.set(Calendar.MONTH, month)
        calendar.set(Calendar.DAY_OF_MONTH, day)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        println("===========> calendar.getTime:" + calendar.getTime)
        switchToFilter(plurkFilter, isUnreadOnly, Some(calendar.getTime))
      }
    }
    val datePickerDialog = new DatePickerDialog(activity, onDateSetListener, mYear,mMonth, mDay);
    datePickerDialog.show()
  }

  override def onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
    val actionMenu = inflater.inflate(R.menu.fragment_timeline, menu)
    this.toggleButtonHolder = Option(menu.findItem(R.id.fragmentTimelineActionToggleUnreadOnly))

    val aboutFromActivity = menu.findItem(R.id.activityMaidroidPlurkActionAbout)
    aboutFromActivity.setVisible(false)
    updateToggleButtonTitle(false, None)
    actionMenu
  }

  override def onOptionsItemSelected(item: MenuItem) = item.getItemId match {
    case R.id.fragmentTimelineActionTimeMachine => showTimeMachine(); false
    case R.id.fragmentTimelineActionToggleUnreadOnly => switchToFilter(plurkFilter, !this.isUnreadOnly)
    case R.id.fragmentTimelineActionPost => startPostPlurkActivity(); false
    case R.id.fragmentTimelineActionMarkAllAsRead => markAllAsRead(); false
    case R.id.fragmentTimelineActionLogout => Logout.logout(activity); false
    case R.id.fragmentTimelineActionAbout => AboutActivity.startActivity(activity); false
    case R.id.fragmentTimelineActionCurrentUser => CurrentUserActivity.startActivity(activity); false
    case _ => super.onOptionsItemSelected(item)
  }

  override def onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {

    requestCode match {
      case TimelineFragment.RequestPostPlurk if resultCode == Activity.RESULT_OK =>
        Toast.makeText(
          this.getActivity, 
          R.string.fragmentTimelinePosted, 
          Toast.LENGTH_SHORT
        )
        pullToRefreshHolder.foreach { pullToRefresh =>
          pullToRefresh.setRefreshing(true)
          updateTimeline(true, true)
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

  private def markAllAsRead() {
    val data = new Bundle
    data.putString("filterName", plurkFilter.map(_.word).getOrElse(null))
    val dialog = ConfirmDialog.createDialog(
      activity, 'MarkAllAsReadConfirm, 
      getString(R.string.fragmentTimelineMarkAllAsReadConfirmTitle),
      getString(R.string.fragmentTimelineMarkAllAsReadConfirm),
      getString(R.string.ok),
      getString(R.string.cancel),
      Some(data)
    )
    dialog.show(activity.getSupportFragmentManager, "markAllAsRead")
  }

  private def getUnreadPlurks(offset: Option[Date] = None, filter: Option[Filter]) = {
    plurkAPI.Timeline.getUnreadPlurks(offset, filter = filter, minimalData = true)
                     .get.plurks.sortBy(_.posted.getTime)
  }

  def markAllAsRead(filter: Option[Filter]) {

    val oldRequestedOrientation = activity.getRequestedOrientation
    activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED)

    val progressDialogFragment = ProgressDialogFragment.createDialog(
      getString(R.string.fragmentTimelineMarking), 
      getString(R.string.pleaseWait)
    )

    progressDialogFragment.show(
      activity.getSupportFragmentManager.beginTransaction, 
      "markProgress"
    )

    val markFuture = Future {
      var plurkIDs: List[Long] = Nil
      var plurks = getUnreadPlurks(None, filter)
      while (!plurks.isEmpty) {
        val firstDate = plurks.head.posted
        plurkIDs :::= plurks.map(_.plurkID)
        plurks = getUnreadPlurks(Some(firstDate), filter)
      }
      plurkAPI.Timeline.markAsRead(plurkIDs)
    }

    markFuture.onSuccessInUI { case plurk =>
      if (activity != null) {

        if (isAdded) {

          progressDialogFragment.dismiss()
          activity.setRequestedOrientation(oldRequestedOrientation)

          Toast.makeText(
            activity, 
            getString(R.string.fragmentTimelineMarkedToast), 
            Toast.LENGTH_LONG
          ).show()

          switchToFilter(filter, isUnreadOnly)
        }
      }
    }

    markFuture.onFailureInUI { case e =>
      if (activity != null) {
        if (isAdded) {
          progressDialogFragment.dismiss()
          activity.setRequestedOrientation(oldRequestedOrientation)
        }
      }
    }
  }

  private def startPostPlurkActivity() = {
    val intent = new Intent(activity, classOf[PostPlurkActivity])
    startActivityForResult(intent, TimelineFragment.RequestPostPlurk)
    false
  }

  def updateTimeline(isNewFilter: Boolean = false, isRecreate: Boolean = false, startDate: Option[Date] = None) {

    this.isLoadingMore = false
    this.hasMoreItem = true
    this.loadMoreFooter.setStatus(LoadMoreFooter.Status.Idle)

    if (isNewFilter) {
      adapterVersion += 1
    }

    val plurksFuture = Future { 
      val plurks = getPlurks(isRecreate = isRecreate, offset = startDate)
      val rawUnreadCount = plurkAPI.Polling.getUnreadCount.get
      val unreadCount = isRecreate match {
        case false if currentFilter == None => rawUnreadCount.all
        case false if currentFilter == Some(OnlyFavorite) => rawUnreadCount.favorite
        case false if currentFilter == Some(OnlyPrivate) => rawUnreadCount.privatePlurks
        case false if currentFilter == Some(OnlyResponded) => rawUnreadCount.responded
        case false if currentFilter == Some(OnlyUser) => rawUnreadCount.my
        case _  => this.unreadCount
      }
      this.unreadCount = unreadCount
      navigationAdapter.setUnreadCount(rawUnreadCount)
      (plurks, unreadCount, adapterVersion) 
    }

    plurksFuture.onSuccessInUI { case (timeline, unreadCount, adapterVersion) => 

      if (activity != null) {
        if (isAdded && adapterVersion >= this.adapterVersion) {

          if (isNewFilter) { 
            updateListAdapter() 
          }

          adapterHolder.foreach(_.appendTimeline(timeline))
          activity.onShowTimelinePlurksSuccess(timeline, isNewFilter, plurkFilter, isUnreadOnly)
          updateToggleButtonTitle(true, Some(unreadCount).filter(_ != 0))
          loadingIndicatorHolder.foreach(_.hide())
          pullToRefreshHolder.foreach(_.setRefreshComplete())
        }
      }
    }

    plurksFuture.onFailureInUI { case e: Exception =>
      if (activity != null) {
        if (isAdded) {
          activity.onShowTimelinePlurksFailure(e)
          showErrorNotice(getString(R.string.fragmentTimelineGetTimelineFailure))
          updateToggleButtonTitle(false)
          pullToRefreshHolder.foreach(_.setRefreshComplete())
        }
      }
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
          button.setText(getString(R.string.unreadWithCount).format(count))
          button.setBackgroundResource(R.drawable.rounded_red)
        case _ =>
          button.setText(if (isUnreadOnly) R.string.unreadPlurk else R.string.allPlurk)
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

