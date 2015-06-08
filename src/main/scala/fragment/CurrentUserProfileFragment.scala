package idv.brianhsu.maidroid.plurk.fragment

import android.app.AlertDialog
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.ActionBarActivity
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Menu
import android.view.MenuItem
import android.view.MenuInflater
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import idv.brianhsu.maidroid.plurk._
import idv.brianhsu.maidroid.plurk.activity.UserTimelineActivity
import idv.brianhsu.maidroid.plurk.activity.PostPlurkActivity
import idv.brianhsu.maidroid.plurk.adapter._
import idv.brianhsu.maidroid.plurk.cache._
import idv.brianhsu.maidroid.plurk.TypedResource._
import idv.brianhsu.maidroid.plurk.util._
import idv.brianhsu.maidroid.ui.util.AsyncUI._
import idv.brianhsu.maidroid.ui.util.CallbackConversions._
import org.bone.soplurk.api.PlurkAPI.PublicProfile
import org.bone.soplurk.constant.TimelinePrivacy
import org.bone.soplurk.model.User
import scala.concurrent._

object CurrentUserProfileFragment {

  val SendPrivatePlurk = 1

  def newInstance(userID: Long) = {
    val args = new Bundle
    val fragment = new CurrentUserProfileFragment
    args.putLong(UserTimelineActivity.ExtraUserID, userID)
    fragment.setArguments(args)
    fragment   
  }

  trait Listener {
    def onPostPrivateMessageOK(): Unit
    def onPostPrivateMessageToNotFriend(): Unit
  }


}

class CurrentUserProfileFragment extends Fragment {

  private implicit def activity = getActivity.asInstanceOf[ActionBarActivity with CurrentUserProfileFragment.Listener]

  private def plurkAPI = PlurkAPIHelper.getPlurkAPI(activity)
  private def loadingIndicatorHolder = Option(getView).map(_.findView(TR.fragmentUserProfileLoadingIndicator))
  private def errorNoticeHolder = Option(getView).map(_.findView(TR.fragmentUserProfileErrorNotice))
  private def displayNameHolder = Option(getView).map(_.findView(TR.userProfileDisplayName))
  private def nickNameHolder = Option(getView).map(_.findView(TR.userProfileNickName))
  private def avatarHolder = Option(getView).map(_.findView(TR.userProfileAvatar))
  private def karmaHolder = Option(getView).map(_.findView(TR.userProfileKarma))
  private def postsHolder = Option(getView).map(_.findView(TR.userProfilePosts))
  private def friendsHolder = Option(getView).map(_.findView(TR.userProfileFriends))
  private def fansHolder = Option(getView).map(_.findView(TR.userProfileFans))

  private def plurkCountsHolder = Option(getView).map(_.findView(TR.userProfilePosts))
  private def aboutHolder = Option(getView).map(_.findView(TR.fragmentUserProfileAbout))
  private def privateMessageToSelfButtonHolder = Option(getView).map(_.findView(TR.fragmentUserProfilePrivateMessageToSelf))

  private lazy val userIDHolder = for {
    argument <- Option(getArguments)
    userID   <- Option(argument.getLong(UserTimelineActivity.ExtraUserID))
  } yield userID


  override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)
    setHasOptionsMenu(true)
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

  private def setupBasicInfo(profile: PublicProfile) = {
    val basicInfo = profile.userInfo.basicInfo
    val nickname = basicInfo.nickname
    val displayName = basicInfo.displayName.getOrElse(nickname)

    displayNameHolder.foreach { _.setText(displayName) }
    nickNameHolder.foreach { _.setText(s"@$nickname") }
    karmaHolder.foreach{ _.setText(f"${basicInfo.karma}%.2f") }
    friendsHolder.foreach{ _.setText(profile.friendsCount.toString) }
    fansHolder.foreach{ _.setText(profile.fansCount.toString) }

    for {
      plurksCount <- profile.userInfo.plurksCount
      plurkCountsTextView <- plurkCountsHolder
    } {
      plurkCountsTextView.setText(plurksCount.toString)
    }

    for {
      aboutContent <- profile.userInfo.about
      aboutTextView <- aboutHolder
    } {
      aboutTextView.setText(aboutContent)
      aboutTextView.setMovementMethod(new LinkMovementMethod)
    }

  }

  private def setupPrivateMessageButton(button: Button, profile: PublicProfile) {
    button.setOnClickListener { view: View =>

      if (profile.areFriends.getOrElse(false) || profile.userInfo.basicInfo.id == PlurkAPIHelper.plurkUserID) {
        val intent = new Intent(activity, classOf[PostPlurkActivity])
        val basicInfo = profile.userInfo.basicInfo
        val displayName = basicInfo.displayName getOrElse basicInfo.nickname

        intent.putExtra(PostPlurkActivity.PrivatePlurkUserID, profile.userInfo.basicInfo.id)
        intent.putExtra(PostPlurkActivity.PrivatePlurkFullName, profile.userInfo.basicInfo.fullName)
        intent.putExtra(PostPlurkActivity.PrivatePlurkDisplayName, displayName)

        startActivityForResult(intent, CurrentUserProfileFragment.SendPrivatePlurk)
      } else {
        Toast.makeText(activity, R.string.activityUserTimelineSendPMToNotFriend, Toast.LENGTH_LONG).show()
        activity.onPostPrivateMessageToNotFriend()
      }
    }
  }

  private def updateProfile(userID: Long) {

    val userProfile = Future { plurkAPI.Profile.getPublicProfile(userID).get }

    userProfile.onSuccessInUI { profile =>

      val basicInfo = profile.userInfo.basicInfo

      setupBasicInfo(profile)
      privateMessageToSelfButtonHolder.foreach(button => setupPrivateMessageButton(button, profile))

      AvatarCache.getAvatarBitmapFromCache(activity, basicInfo) match {
        case Some(avatarBitmap) => setAvatarFromCache(avatarBitmap)
        case None => setAvatarFromNetwork(activity, basicInfo)
      }

      loadingIndicatorHolder.foreach(_.hide())
    }

    userProfile.onFailureInUI { case e: Exception =>
      showErrorNotice(activity.getString(R.string.fragmentUserProfileError))
    }
  }

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle): View = {
    val view = inflater.inflate(R.layout.fragment_user_profile, container, false)
    updateProfile(userIDHolder.getOrElse(-1L))
    view
  }
}
