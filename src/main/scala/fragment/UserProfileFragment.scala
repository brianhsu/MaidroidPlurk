package idv.brianhsu.maidroid.plurk.fragment

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.ActionBarActivity
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import idv.brianhsu.maidroid.plurk._
import idv.brianhsu.maidroid.plurk.activity.UserTimelineActivity
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
  private def karmaHolder = Option(getView).map(_.findView(TR.userProfileKarma))
  private def postsHolder = Option(getView).map(_.findView(TR.userProfilePosts))
  private def friendsHolder = Option(getView).map(_.findView(TR.userProfileFriends))
  private def fansHolder = Option(getView).map(_.findView(TR.userProfileFans))
  private def plurkCountsHolder = Option(getView).map(_.findView(TR.userProfilePosts))
  private def aboutHolder = Option(getView).map(_.findView(TR.fragmentUserProfileAbout))
  private def friendButtonHolder = Option(getView).map(_.findView(TR.fragmentUserProfileFriendButton))
  private def fanButtonHolder = Option(getView).map(_.findView(TR.fragmentUserProfileFanButton))

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

  private def setupFriendButton(button: Button, areAlreadyFriends: Boolean, userID: Long) {

    def setButtonToAddFriend() {
      button.setEnabled(true)
      button.setText(R.string.fragmentUserProfileAddFreind)
      button.setOnClickListener { view: View => 

        button.setEnabled(false)
        button.setText(R.string.fragmentUserProfileAddFreindRequesting)

        val requestFuture = Future { plurkAPI.FriendsFans.becomeFriend(userID).get }.filter(_ == true)

        requestFuture.onSuccessInUI { case _ =>
          button.setText(R.string.fragmentUserProfileAddFreindSent)
          button.setEnabled(false)
        }

        requestFuture.onFailureInUI { case e: Exception =>
          //! 錯誤通知
          import android.widget.Toast
          val toast = Toast.makeText(activity, "無法送出好友請求", Toast.LENGTH_LONG)
          toast.show()
          setButtonToAddFriend()
        }
      }
    }

    def setButtonToRemoveFriend() {
      button.setEnabled(true)
      button.setText(R.string.fragmentUserProfileRemoveFreind)
      button.setOnClickListener { view: View => 

        button.setEnabled(false)
        button.setText(R.string.fragmentUserProfileAddFreindRequesting)

        val requestFuture = Future { plurkAPI.FriendsFans.removeAsFriend(userID).get }.filter(_ == true)

        requestFuture.onSuccessInUI { case _ => setButtonToAddFriend() }
        requestFuture.onFailureInUI { case e: Exception =>
          //! 錯誤通知
          import android.widget.Toast
          val toast = Toast.makeText(activity, "無法送出取消好友請求", Toast.LENGTH_LONG)
          toast.show()
          setButtonToAddFriend()
        }
      }
    }

    areAlreadyFriends match {
      case true  => setButtonToRemoveFriend()
      case false => setButtonToAddFriend()
    }
  }

  private def setupFollowingButton(button: Button, isPrivateTimeline: Boolean, areFriends: Boolean, 
                                   isAlreadyFollowing: Boolean, userID: Long) {

    def setButtonToUnfollow() {
      button.setEnabled(true)
      button.setText(R.string.fragmentUserProfileIsFollowing)
      button.setOnClickListener { view: View => 

        button.setEnabled(false)
        button.setText(R.string.fragmentUserProfileAddFreindRequesting)

        val requestFuture = Future { plurkAPI.FriendsFans.setFollowing(userID, false).get }.filter(_ == true)

        requestFuture.onSuccessInUI { case _ =>
          button.setText(R.string.fragmentUserProfileNotFollowing)
          setButtonToFollow()
        }

        requestFuture.onFailureInUI { case e: Exception =>
          //! 錯誤通知
          import android.widget.Toast
          val toast = Toast.makeText(activity, "無法追蹤對方河道", Toast.LENGTH_LONG)
          toast.show()
          setButtonToUnfollow()
        }
      }
    }

    def setButtonToFollow() {
      button.setEnabled(true)
      button.setText(R.string.fragmentUserProfileNotFollowing)
      button.setOnClickListener { view: View => 

        button.setEnabled(false)
        button.setText(R.string.fragmentUserProfileAddFreindRequesting)

        val requestFuture = Future { plurkAPI.FriendsFans.setFollowing(userID, true).get }.filter(_ == true)

        requestFuture.onSuccessInUI { case _ =>
          button.setText(R.string.fragmentUserProfileIsFollowing)
          setButtonToUnfollow()
        }

        requestFuture.onFailureInUI { case e: Exception =>
          //! 錯誤通知
          import android.widget.Toast
          val toast = Toast.makeText(activity, "無法追蹤對方河道", Toast.LENGTH_LONG)
          toast.show()
          setButtonToFollow()
        }
      }
     
    }

    if (isPrivateTimeline && !areFriends) {
      // 私密河道，無法擁有粉絲
      button.setEnabled(false)
      button.setText(R.string.fragmentUserProfilePrviate)
    } else if (!isAlreadyFollowing) {
      // 追蹤對方
      setButtonToFollow()
    } else if (isAlreadyFollowing) {
      // 取消追蹤對方
      setButtonToUnfollow()


    }
  }

  private def updateProfile(userID: Long) {

    val userProfile = Future { plurkAPI.Profile.getPublicProfile(userID).get }

    userProfile.onSuccessInUI { profile =>

      val basicInfo = profile.userInfo.basicInfo

      setupBasicInfo(profile)

      if (userID == PlurkAPIHelper.plurkUserID) {
        friendButtonHolder.foreach(_.setVisibility(View.GONE))
        fanButtonHolder.foreach(_.setVisibility(View.GONE))
      } else {
        val areFriends = profile.areFriends.getOrElse(false)
        val isFollowing = profile.isFollowing.getOrElse(false)
        val isPrivateTimeline = profile.privacy == TimelinePrivacy.OnlyFriends

        friendButtonHolder.foreach(button => setupFriendButton(button, areFriends, userID))
        fanButtonHolder.foreach(button => setupFollowingButton(button, isPrivateTimeline, areFriends, isFollowing, userID) )

      }

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
