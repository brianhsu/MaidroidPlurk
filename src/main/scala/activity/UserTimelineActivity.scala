package idv.brianhsu.maidroid.plurk.activity

import org.bone.soplurk.model.User
import idv.brianhsu.maidroid.plurk._
import idv.brianhsu.maidroid.plurk.TypedResource._
import idv.brianhsu.maidroid.plurk.adapter._
import idv.brianhsu.maidroid.ui.util.CallbackConversions._

import android.os.Bundle
import android.content.Intent
import android.content.Context
import android.net.Uri
import android.view.View
import android.text.Html
import android.text.method.LinkMovementMethod

import android.support.v7.app.ActionBarActivity

import scala.util.Try
import android.widget.TextView
import android.graphics.Color

object UserTimelineActivity {

  val ExtraUserID = "ExtraUserID"
  val ExtraUserDisplayName = "ExtraDisplayName"
  val ExtraUserNickname = "ExtraUserNickname"

  def startActivity(context: Context, user: User) {
    val intent = new Intent(context, classOf[UserTimelineActivity])
    intent.putExtra(ExtraUserID, user.id)
    intent.putExtra(ExtraUserNickname, user.nickname)
    user.displayName.foreach(intent.putExtra(ExtraUserDisplayName, _))
    context.startActivity(intent)
  }
}

import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.ViewGroup
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import idv.brianhsu.maidroid.ui.util.AsyncUI._
import idv.brianhsu.maidroid.ui.util.CallbackConversions._
import scala.concurrent._
import idv.brianhsu.maidroid.plurk.util._
import android.graphics.Bitmap
import idv.brianhsu.maidroid.plurk.util._
import idv.brianhsu.maidroid.plurk.cache._


object UserProfileFragment {
  def newInstance(userID: Long) = {
    val args = new Bundle
    val fragment = new UserProfileFragment
    args.putLong(UserTimelineActivity.ExtraUserID, userID)
    fragment.setArguments(args)
    fragment   
  }
}

class UserProfileFragment extends Fragment {

  private implicit def activity = getActivity.asInstanceOf[ActionBarActivity]

  private def plurkAPI = PlurkAPIHelper.getPlurkAPI(activity)
  private def loadingIndicatorHolder = Option(getView).map(_.findView(TR.fragmentUserProfileLoadingIndicator))
  private def errorNoticeHolder = Option(getView).map(_.findView(TR.fragmentUserProfileErrorNotice))
  private def displayNameHolder = Option(getView).map(_.findView(TR.userProfileDisplayName))
  private def nickNameHolder = Option(getView).map(_.findView(TR.userProfileNickName))
  private def avatarHolder = Option(getView).map(_.findView(TR.userProfileAvatar))

  private lazy val userIDHolder = for {
    argument <- Option(getArguments)
    userID   <- Option(argument.getLong(UserTimelineActivity.ExtraUserID))
  } yield userID


  override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)
  }

  private def showErrorNotice(message: String) {
    loadingIndicatorHolder.foreach(_.hide())
    errorNoticeHolder.foreach(_.setVisibility(View.VISIBLE))
    errorNoticeHolder.foreach { errorNotice =>
      errorNotice.setMessageWithRetry(message) { retryButton =>
        retryButton.setEnabled(false)
        errorNoticeHolder.foreach(_.setVisibility(View.GONE))
        loadingIndicatorHolder.foreach(_.show())
        updateProfile(userIDHolder.getOrElse(-1L))
      }
    }
  }

  def setAvatarFromCache(avatarBitmap: Bitmap) {
    avatarHolder.foreach(_.setImageBitmap(avatarBitmap))
  }

  def setAvatarFromNetwork(context: Context, user: User) {
    val avatarFuture = AvatarCache.getAvatarBitmapFromNetwork(context, user)
    avatarFuture.onSuccessInUI { case(userID, bitmap) =>
      avatarHolder.foreach(_.setImageBitmap(bitmap))
    }
  }

  private def updateProfile(userID: Long) {

    val userProfile = Future { plurkAPI.Profile.getPublicProfile(userID).get }

    userProfile.onSuccessInUI { profile =>
      val basicInfo = profile.userInfo.basicInfo
      val nickname = basicInfo.nickname
      val displayName = basicInfo.displayName.getOrElse(nickname)

      displayNameHolder.foreach { _.setText(displayName) }
      nickNameHolder.foreach { _.setText(s"@$nickname") }
      AvatarCache.getAvatarBitmapFromCache(activity, basicInfo) match {
        case Some(avatarBitmap) => setAvatarFromCache(avatarBitmap)
        case None => setAvatarFromNetwork(activity, basicInfo)
      }

      loadingIndicatorHolder.foreach(_.hide())
    }

    userProfile.onFailureInUI { case e: Exception =>
      showErrorNotice("Error")
    }


  }

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle): View = {
    val view = inflater.inflate(R.layout.fragment_user_profile, container, false)
    updateProfile(userIDHolder.getOrElse(-1L))
    view
  }

}

class UserTimelineFragment extends Fragment {
  override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)
  }

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle): View = {
    val t = new TextView(this.getActivity)
    t.setText("Profile")
    t
  }

}

class MyAdapter(context: Context, fm: FragmentManager, userID: Long) extends FragmentPagerAdapter(fm) {
  override def getCount = 2
  override def getItem(position: Int) = position match {
    case 0 => UserProfileFragment.newInstance(userID)
    case 1 => new UserTimelineFragment
  }
  override def getPageTitle(position: Int) = position match {
    case 0 => context.getString(R.string.adapterUserTimelineProfile)
    case 1 => context.getString(R.string.adapterUserTimelineTimeline)
  }
}


class UserTimelineActivity extends ActionBarActivity with TypedViewHolder
{

  import UserTimelineActivity._

  private lazy val userID = getIntent.getLongExtra(ExtraUserID, -1)

  private lazy val viewPager = findView(TR.activityUserTimelineViewPager)
  private lazy val pagerIndicator = findView(TR.activityUserTimelinePagerIndicator)
  private lazy val pageAdapter = new MyAdapter(this, getSupportFragmentManager, userID)

  override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)

    setContentView(R.layout.activity_user_timeline)
    val intent = getIntent
    val nickname = Option(intent.getStringExtra(ExtraUserNickname))
    val displayName = Option(intent.getStringExtra(ExtraUserDisplayName))
    val titleName = (displayName orElse nickname).getOrElse(userID)
    val activityTitle = this.getString(R.string.titleUserTimeline).format(titleName)

    setTitle(activityTitle)
    viewPager.setAdapter(pageAdapter)
    pagerIndicator.setViewPager(viewPager)
  }
}
