package idv.brianhsu.maidroid.plurk.fragment

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.ActionBarActivity
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.AbsListView.OnScrollListener
import android.widget.Button
import idv.brianhsu.maidroid.plurk._
import idv.brianhsu.maidroid.plurk.activity._
import idv.brianhsu.maidroid.plurk.adapter._
import idv.brianhsu.maidroid.plurk.cache._
import idv.brianhsu.maidroid.plurk.dialog._
import idv.brianhsu.maidroid.plurk.TypedResource._
import idv.brianhsu.maidroid.plurk.util._
import idv.brianhsu.maidroid.plurk.view._
import idv.brianhsu.maidroid.ui.util.AsyncUI._
import idv.brianhsu.maidroid.ui.util.CallbackConversions._
import java.util.Date
import org.bone.soplurk.api.PlurkAPI.Timeline
import org.bone.soplurk.constant.TimelinePrivacy
import org.bone.soplurk.model.Plurk
import org.bone.soplurk.model.User
import scala.concurrent._
import scala.util.Try
import uk.co.senab.actionbarpulltorefresh.library.ActionBarPullToRefresh
import uk.co.senab.actionbarpulltorefresh.library.listeners.OnRefreshListener
import uk.co.senab.actionbarpulltorefresh.library.Options

object UserTimelineFragment {

  private var savedTimeline: Option[Timeline] = None

  def newInstance(userID: Long) = {
    val args = new Bundle
    val fragment = new UserTimelineFragment
    args.putLong(UserTimelineActivity.ExtraUserID, userID)
    fragment.setArguments(args)
    fragment
  }

  trait Listener {
    def onShowTimelinePlurksFailure(e: Exception): Unit
    def onShowTimelinePlurksSuccess(timeline: Timeline): Unit
  }
}

class UserTimelineFragment extends Fragment {

  private implicit def activity = 
    getActivity.asInstanceOf[ActionBarActivity with UserTimelineFragment.Listener 
                                               with PlurkView.Listener 
                                               with ConfirmDialog.Listener]

  private def plurkAPI = PlurkAPIHelper.getPlurkAPI(activity)

  private def listViewHolder = Option(getView).map(_.findView(TR.fragmentUserTimelineListView))
  private def pullToRefreshHolder = Option(getView).map(_.findView(TR.fragementUserTimelinePullToRefresh))
  private def loadingIndicatorHolder = Option(getView).map(_.findView(TR.fragmentUserTimelineLoadingIndicator))
  private def errorNoticeHolder = Option(getView).map(_.findView(TR.fragmentUserTimelineErrorNotice))
  private def privateTimelineHolder = Option(getView).map(_.findView(TR.fragmentUserTimelinePrivate))

  private var loadMoreFooter: LoadMoreFooter = _
  private var isLoadingMore = false
  private var hasMoreItem = true

  private var adapterHolder: Option[PlurkAdapter] = None

  private var isInError: Boolean = false
  private var isRecreate: Boolean = false
  private lazy val userIDHolder = for {
    argument <- Option(getArguments)
    userID   <- Option(argument.getLong(UserTimelineActivity.ExtraUserID))
  } yield userID

  override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)
    this.isInError = (savedInstanceState != null && savedInstanceState.getBoolean("isInError", false))
    this.isRecreate = (savedInstanceState != null)
  }

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, 
                            savedInstanceState: Bundle): View = {
    inflater.inflate(R.layout.fragment_user_timeline, container, false)
  }

  override def onViewCreated(view: View, savedInstanceState: Bundle) {

    loadMoreFooter = new LoadMoreFooter(activity)
    updateTimeline(this.isRecreate)
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

  override def onResume() {
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
        updateTimeline(false, true)
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

  
  private def getPlurks(offset: Option[Date] = None, isRecreate: Boolean = false) = {
    val shouldRecreate = isRecreate && UserTimelineFragment.savedTimeline.isDefined

    shouldRecreate match {
      case true => UserTimelineFragment.savedTimeline.get
      case false => plurkAPI.Timeline.getPublicPlurks(userIDHolder.getOrElse(-1).toString, offset = offset).get
    }
  }

  private def updateListAdapter() {
    this.adapterHolder = Some(new PlurkAdapter(activity, false, true))
    for {
      adapter <- this.adapterHolder
      listView <- this.listViewHolder
    } { 
      listView.setAdapter(adapter)
    }
  }

  override def onDestroy() {
    UserTimelineFragment.savedTimeline = adapterHolder.map(_.getTimeline)
    super.onDestroy()
  }

  override def onLowMemory() {
    UserTimelineFragment.savedTimeline = None
    AvatarCache.clearCache()
    ImageCache.clearCache()
  }

  def updateTimeline(isRecreate: Boolean = false, isPullRefresh: Boolean = false) {

    this.isLoadingMore = false
    this.hasMoreItem = true
    this.loadMoreFooter.setStatus(LoadMoreFooter.Status.Idle)

    val plurksFuture = Future { 
      val profile = plurkAPI.Profile.getPublicProfile(userIDHolder.getOrElse(-1L)).get
      val plurks = getPlurks(isRecreate = isRecreate)
      val isPrivateTimeline = profile.privacy == TimelinePrivacy.OnlyFriends && !profile.areFriends.getOrElse(false)
      (plurks, isPrivateTimeline) 
    }

    plurksFuture.onSuccessInUI { case (timeline, isPrivateTimeline) => 

      if (isPrivateTimeline) {
        loadingIndicatorHolder.foreach(_.hide())
        privateTimelineHolder.foreach(_.setVisibility(View.VISIBLE))
      } else if (isAdded) {
        if (isPullRefresh) {
          updateListAdapter()
        }
        adapterHolder.foreach(_.appendTimeline(timeline))
        activity.onShowTimelinePlurksSuccess(timeline)
        loadingIndicatorHolder.foreach(_.hide())
        privateTimelineHolder.foreach(_.setVisibility(View.GONE))
        pullToRefreshHolder.foreach(_.setRefreshComplete())
      }
    }

    plurksFuture.onFailureInUI { case e: Exception =>
      if (isAdded) {
        activity.onShowTimelinePlurksFailure(e)
        showErrorNotice(getString(R.string.fragmentTimelineGetTimelineFailure))
        pullToRefreshHolder.foreach(_.setRefreshComplete())
      }
    }
  }

}

