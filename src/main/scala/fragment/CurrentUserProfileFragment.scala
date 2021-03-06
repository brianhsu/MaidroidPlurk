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
import android.view.Gravity
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import idv.brianhsu.maidroid.plurk._
import idv.brianhsu.maidroid.plurk.activity._
import idv.brianhsu.maidroid.plurk.adapter._
import idv.brianhsu.maidroid.plurk.cache._
import idv.brianhsu.maidroid.plurk.TypedResource._
import idv.brianhsu.maidroid.plurk.util._
import idv.brianhsu.maidroid.ui.util.AsyncUI._
import idv.brianhsu.maidroid.ui.util.CallbackConversions._
import org.bone.soplurk.api.PlurkAPI.OwnProfile
import org.bone.soplurk.constant.TimelinePrivacy
import org.bone.soplurk.model.User
import java.util.Date
import java.text.SimpleDateFormat
import scala.concurrent._
import android.app.ProgressDialog

object CurrentUserProfileFragment {

  def newInstance() = new CurrentUserProfileFragment

  trait Listener {
    def onProfileFetchedOK(): Unit
    def onProfileFetchedFailure(error: Exception): Unit

  }


}

class CurrentUserProfileFragment extends Fragment {

  private implicit def activity = getActivity.asInstanceOf[ActionBarActivity with CurrentUserProfileFragment.Listener]

  private def plurkAPI = PlurkAPIHelper.getPlurkAPI(activity)
  private def loadingIndicatorHolder = Option(getView).map(_.findView(TR.fragmentCurrentUserProfileLoadingIndicator))
  private def errorNoticeHolder = Option(getView).map(_.findView(TR.fragmentCurrentUserProfileErrorNotice))
  private def displayNameHolder = Option(getView).map(_.findView(TR.currentUserProfileDisplayName))
  private def nickNameHolder = Option(getView).map(_.findView(TR.currentUserProfileNickName))
  private def avatarHolder = Option(getView).map(_.findView(TR.currentUserProfileAvatar))
  private def karmaHolder = Option(getView).map(_.findView(TR.currentUserProfileKarma))
  private def postsHolder = Option(getView).map(_.findView(TR.currentUserProfilePosts))
  private def friendsHolder = Option(getView).map(_.findView(TR.currentUserProfileFriends))
  private def fansHolder = Option(getView).map(_.findView(TR.currentUserProfileFans))

  private def plurkCountsHolder = Option(getView).map(_.findView(TR.currentUserProfilePosts))
  private def aboutHolder = Option(getView).map(_.findView(TR.fragmentCurrentUserProfileAbout))
  private def privateMessageToSelfButtonHolder = Option(getView).map(_.findView(TR.fragmentCurrentUserProfilePrivateMessageToSelf))
  private def fullNameViewHolder = Option(getView).map(_.findView(TR.currentUserProfileFullName))
  private def birthdayViewHolder = Option(getView).map(_.findView(TR.currentUserProfileBirthday))
  private def privacyViewHolder = Option(getView).map(_.findView(TR.currentUserProfilePrivacy))
  private def alertCountHolder = Option(getView).map(_.findView(TR.currentUserProfileAlertCount))
  private def alertButtonHolder = Option(getView).map(_.findView(TR.currentUserProfileAlert))

  private var profileHolder: Option[(Int, OwnProfile)] = None
  private var userBirthdayHolder: Option[Date] = None
  private var userPrivacy: Option[TimelinePrivacy] = None
  private var editButtonHolder: Option[MenuItem] = None
  private val dateFormatter = new SimpleDateFormat("yyyy-MM-dd")

  override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)
    setHasOptionsMenu(true)
  }

  override def onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
    inflater.inflate(R.menu.fragment_current_user_profile, menu)
    editButtonHolder = Option(menu.findItem(R.id.fragmentCurrentUserProfileEdit))
  }

  override def onOptionsItemSelected(item: MenuItem) = item.getItemId match {
    case R.id.fragmentCurrentUserProfileEditFullName => startEditFullName(); false
    case R.id.fragmentCurrentUserProfileEditDisplayName => startEditDisplayName(); false
    case R.id.fragmentCurrentUserProfileEditBirthday => startEditBirthday(); false
    case R.id.fragmentCurrentUserProfileEditAbout => startEditAbout(); false
    case R.id.fragmentCurrentUserProfileEditTimelinePrivacy => startEditPrivacy(); false
    case _ => super.onOptionsItemSelected(item)
  }

  private def startEditPrivacy() {
    import android.app.AlertDialog
    import android.content.DialogInterface
    import android.widget.EditText
    import android.text.TextWatcher
    import android.text.Editable
    import android.widget.Spinner
    import android.widget.ArrayAdapter

    val alertDialogBuilder = new AlertDialog.Builder(activity)
    val spinner = new Spinner(activity)
    val items = Array(
      activity.getString(R.string.timelinePrivacyWorld), 
      activity.getString(R.string.timelinePrivacyOnlyFriends)
    )
    val adapter = new ArrayAdapter(activity, android.R.layout.simple_list_item_1, items)

    spinner.setAdapter(adapter)

    userPrivacy.foreach { privacy =>
      val defaultSelection = privacy match {
        case TimelinePrivacy.World => 0
        case TimelinePrivacy.OnlyFriends => 1
      }
      spinner.setSelection(defaultSelection)
    }

    alertDialogBuilder.setTitle(R.string.profileEditPrivacy)
                      .setView(spinner)

    alertDialogBuilder.setPositiveButton(R.string.ok, null)
    alertDialogBuilder.setNegativeButton(R.string.cancel, null)

    val alertDialog = alertDialogBuilder.create

    def updateTimelinePrivacy(newPrivacy: TimelinePrivacy) {
      val progress = ProgressDialog.show(
        activity, 
        activity.getString(R.string.pleaseWait), 
        activity.getString(R.string.profileSaving),
        true, false
      )
      val future = Future { plurkAPI.Users.update(privacy = Some(newPrivacy)).get }

      future.onSuccessInUI { e: Any => 
        if (activity != null) {
          progress.dismiss() 
          alertDialog.dismiss()
          updateProfile()
        }
      }

      future.onFailureInUI { case e: Exception => 
        if (activity != null) {
          progress.dismiss() 
          Toast.makeText(activity, R.string.profileSavingFailed, Toast.LENGTH_LONG).show()
        }
      }
    }

    alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
      override def onShow(dialog: DialogInterface) {
        val okButton = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE)
        okButton.setOnClickListener { view: View =>
          val newPrivacy = spinner.getSelectedItemPosition match {
            case 0 => TimelinePrivacy.World
            case 1 => TimelinePrivacy.OnlyFriends
          }

          if (newPrivacy == TimelinePrivacy.OnlyFriends) {
            val warningDialog = new AlertDialog.Builder(activity)
            warningDialog.setTitle(R.string.profilePrivateTimelineConfirmTitle)
                         .setMessage(R.string.profilePrivateTimelineConfirmContent)
            warningDialog.setNegativeButton(R.string.ok, null)
            warningDialog.setPositiveButton(R.string.cancel, new DialogInterface.OnClickListener() {
              override def onClick(dialog: DialogInterface, which: Int) {
                updateTimelinePrivacy(newPrivacy)
              }
            })
            warningDialog.show()
          } else {
            updateTimelinePrivacy(newPrivacy)
          }
        }
      }
    })

    alertDialog.show()

  }

  private def startEditBirthday() {
    import android.app.DatePickerDialog
    import android.widget.DatePicker
    import java.util.Calendar

    val calendar = Calendar.getInstance()
    userBirthdayHolder.foreach(date => calendar.setTime(date))
    val mYear = calendar.get(Calendar.YEAR)
    val mMonth = calendar.get(Calendar.MONTH)
    val mDay = calendar.get(Calendar.DAY_OF_MONTH)

    val onDateSetListener = new DatePickerDialog.OnDateSetListener() {
      override def onDateSet(view: DatePicker, year: Int, month: Int, day: Int): Unit = {
        calendar.set(Calendar.YEAR, year)
        calendar.set(Calendar.MONTH, month)
        calendar.set(Calendar.DAY_OF_MONTH, day)
        val progress = ProgressDialog.show(
          activity, 
          activity.getString(R.string.pleaseWait), 
          activity.getString(R.string.profileSaving), 
          true, false
        )
        val future = Future { plurkAPI.Users.update(birthday = Some(calendar.getTime)).get }

        future.onSuccessInUI { e: Any => 
          if (activity != null) {
            progress.dismiss() 
            updateProfile()
          }
        }

        future.onFailureInUI { case e: Exception => 
          if (activity != null) {
            progress.dismiss() 
            Toast.makeText(activity, R.string.profileSavingFailed, Toast.LENGTH_LONG).show()
          }
        }

      }
    }

    val datePickerDialog = new DatePickerDialog(activity, onDateSetListener, mYear,mMonth, mDay);
    datePickerDialog.setTitle(R.string.profileEditBirthday)
    datePickerDialog.show()
  }

  private def startEditDialog(title: String, defaultValue: String, lines: Int, callback: String => Any) {
    import android.app.AlertDialog
    import android.content.DialogInterface
    import android.widget.EditText
    import android.text.TextWatcher
    import android.text.Editable

    val alertDialogBuilder = new AlertDialog.Builder(activity)
    val editText = new EditText(activity)
    editText.setText(defaultValue)
    if (lines == 1) {
      editText.setSingleLine()
    } else {
      editText.setLines(lines)
      editText.setGravity(Gravity.TOP)
    }

    alertDialogBuilder.setTitle(title)
                      .setView(editText)

    alertDialogBuilder.setPositiveButton(R.string.ok, null)
    alertDialogBuilder.setNegativeButton(R.string.cancel, null)

    val alertDialog = alertDialogBuilder.create
    alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
      override def onShow(dialog: DialogInterface) {
        val okButton = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE)
        okButton.setOnClickListener { view: View =>
          val progress = ProgressDialog.show(
            activity, 
            activity.getString(R.string.pleaseWait), 
            activity.getString(R.string.profileSaving), 
            true, false
          )
          val future = Future { callback(editText.getText.toString) }

          future.onSuccessInUI { e: Any => 
            if (activity != null) {
              progress.dismiss() 
              alertDialog.dismiss()
              updateProfile()
            }
          }

          future.onFailureInUI { case e: Exception => 
            if (activity != null) {
              progress.dismiss() 
              Toast.makeText(activity, R.string.profileSavingFailed, Toast.LENGTH_LONG).show()
            }
          }
        }
      }
    })

    editText.addTextChangedListener(new TextWatcher{
      override def afterTextChanged(s: Editable) {
        val shouldEnableOKButton = s.toString.trim.size > 0
        alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(shouldEnableOKButton)
      }
      override def beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
      override def onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
    })

    alertDialog.show()
  }

  private def startEditAbout() {
    val defaultValue = aboutHolder.map(_.getText.toString).getOrElse("")
    startEditDialog(
      activity.getString(R.string.profileEditAbout), 
      defaultValue, 5, 
      newValue => plurkAPI.Users.update(about = Some(newValue)).get
    )
  }

  private def startEditFullName() {
    val defaultValue = fullNameViewHolder.map(_.getText.toString).getOrElse("")
    startEditDialog(
      activity.getString(R.string.profileEditFullName), 
      defaultValue, 1, 
      newValue => plurkAPI.Users.update(fullName = Some(newValue)).get
    )
  }

  private def startEditDisplayName() {
    val defaultValue = displayNameHolder.map(_.getText.toString).getOrElse("")
    startEditDialog(
      activity.getString(R.string.profileEditDisplayName), 
      defaultValue, 1, 
      newValue => plurkAPI.Users.update(displayName = Some(newValue)).get
    )
  }

  private def showErrorNotice(message: String) {
    loadingIndicatorHolder.foreach(_.hide())
    editButtonHolder.foreach(_.setVisible(false))
    errorNoticeHolder.foreach(_.setVisibility(View.VISIBLE))
    errorNoticeHolder.foreach { errorNotice =>
      errorNotice.setMessageWithRetry(message) { retryButton =>
        retryButton.setEnabled(false)
        errorNoticeHolder.foreach(_.setVisibility(View.GONE))
        loadingIndicatorHolder.foreach(_.show())
        updateProfile()
      }
    }
  }

  def setAvatarFromCache(avatarBitmap: Bitmap) {
    avatarHolder.foreach(_.setImageBitmap(avatarBitmap))
  }

  def setAvatarFromNetwork(context: Context, user: User) {
    val avatarFuture = AvatarCache.getAvatarBitmapFromNetwork(context, user)
    avatarFuture.onSuccessInUI { case(userID, bitmap) =>
      if (activity != null) {
        avatarHolder.foreach(_.setImageBitmap(bitmap))
      }
    }
  }

  private val PickFromFile = 0
  private val PickFromCamera = 1
  private val CropImage = 2

  private def startCropIntent(dataIntent: Intent) {
    val photoDataHolder = Option(dataIntent).map(_.getData)
    
    photoDataHolder.foreach { photo => 
      val intent = new Intent("com.android.camera.action.CROP")
      intent.setType("image/*")
      intent.setData(photo)
      intent.putExtra("outputX", 200)
      intent.putExtra("outputY", 200)
      intent.putExtra("aspectX", 1)
      intent.putExtra("aspectY", 1)
      intent.putExtra("scale", true)
      intent.putExtra("return-data", true)
      startActivityForResult(Intent.createChooser(intent, activity.getString(R.string.profileEditCrop)), CropImage)
    }
  }

  private def uploadAvatar(dataIntent: Intent) {
    val photoHolder: Option[Bitmap] = for {
      returnedData <- Option(dataIntent)
      returnedExtra <- Option(returnedData.getExtras)
      returnedBitmap <- Option(returnedExtra.getParcelable[Bitmap]("data"))
    } yield returnedBitmap

    photoHolder.foreach { bitmap =>
      val progressDialog = ProgressDialog.show(
        activity, 
        activity.getString(R.string.pleaseWait), 
        activity.getString(R.string.profileSaving),
        true, false
      )

      val future = Future {
        val fileHolder = DiskCacheHelper.writeBitmapToCache(activity, dataIntent.toString, bitmap)
        fileHolder.foreach { avatarFile =>
          plurkAPI.Users.updatePicture(avatarFile).get
        }
      }

      future.onSuccessInUI { e => 
        if (activity != null) {
          progressDialog.dismiss() 
          updateProfile() 
        }
      }

      future.onFailureInUI { case e: Exception =>
        if (activity != null) {
          progressDialog.dismiss() 
          Toast.makeText(activity, R.string.profileSavingFailed, Toast.LENGTH_LONG).show()
        }
      }
    }
  }

  override protected def onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
    requestCode match {
      case PickFromFile|PickFromCamera => startCropIntent(data)
      case CropImage => uploadAvatar(data)
    }
  }

  private def setupBasicInfo(profile: OwnProfile) = {
    val basicInfo = profile.userInfo.basicInfo
    val nickname = basicInfo.nickname
    val displayName = basicInfo.displayName.getOrElse(nickname)

    displayNameHolder.foreach { _.setText(displayName) }
    nickNameHolder.foreach { _.setText(s"@$nickname") }
    karmaHolder.foreach{ _.setText(f"${basicInfo.karma}%.2f") }
    friendsHolder.foreach{ _.setText(profile.friendsCount.toString) }
    fansHolder.foreach{ _.setText(profile.fansCount.toString) }
    fullNameViewHolder.foreach(_.setText(profile.userInfo.basicInfo.fullName))
    avatarHolder.foreach { avatar =>
      avatar.setOnClickListener { view: View =>
        import android.widget.ArrayAdapter
        import android.provider.MediaStore
        val dialog = new AlertDialog.Builder(activity)
        val adapter = new ArrayAdapter(
          activity, 
          android.R.layout.select_dialog_item, 
          Array(activity.getString(R.string.profileEditGallery), activity.getString(R.string.profileEditCamera))
        )
        dialog.setTitle(R.string.profileEditChooseAvatar)
        dialog.setAdapter(adapter, new DialogInterface.OnClickListener() {
          override def onClick(dialog: DialogInterface, which: Int): Unit = {
            which match {
              case 0 =>
                val intent = new Intent
                intent.setType("image/*")
                intent.setAction(Intent.ACTION_GET_CONTENT)
                val chooser = Intent.createChooser(intent, activity.getString(R.string.profileEditChooseAvatar))
                startActivityForResult(chooser, PickFromFile)
              case 1 =>
                val takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                if (takePictureIntent.resolveActivity(activity.getPackageManager()) != null) {
                  startActivityForResult(takePictureIntent, PickFromCamera)
                }
            }
          }
        })
        dialog.show()
      }
    }
    userBirthdayHolder = profile.userInfo.basicInfo.birthday
    userPrivacy = Some(profile.privacy)

    val privacyText = profile.privacy match {
      case TimelinePrivacy.World => activity.getString(R.string.timelinePrivacyWorld)
      case TimelinePrivacy.OnlyFriends => activity.getString(R.string.timelinePrivacyOnlyFriends)
    }

    privacyViewHolder.foreach(_.setText(privacyText))

    for {
      birthday <- userBirthdayHolder
      birthdayView <- birthdayViewHolder
    } {
      birthdayView.setText(dateFormatter.format(birthday))
    }

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

    val gif = activity.getAssets.open("test.gif")
    println("=====> gif:" + gif)

  }

  private def setupPrivateMessageButton(button: Button, profile: OwnProfile) {
    button.setOnClickListener { view: View =>

      val intent = new Intent(activity, classOf[PostPlurkActivity])
      val basicInfo = profile.userInfo.basicInfo
      val displayName = basicInfo.displayName getOrElse basicInfo.nickname

      intent.putExtra(PostPlurkActivity.PrivatePlurkUserID, profile.userInfo.basicInfo.id)
      intent.putExtra(PostPlurkActivity.PrivatePlurkFullName, profile.userInfo.basicInfo.fullName)
      intent.putExtra(PostPlurkActivity.PrivatePlurkDisplayName, displayName)

      startActivity(intent)
    }
  }

  private def updateProfile() {

    val userProfile = Future { 
      val profile = profileHolder match {
        case None => (plurkAPI.Alerts.getActive.get.size, plurkAPI.Profile.getOwnProfile.get)
        case Some(cache) => cache
      }
      profileHolder = Some(profile)
      profile
    }

    editButtonHolder.foreach(_.setVisible(false))
    userProfile.onSuccessInUI { case (alertCount, profile) =>

      if (activity != null) {

        val basicInfo = profile.userInfo.basicInfo

        setupBasicInfo(profile)
        privateMessageToSelfButtonHolder.foreach(button => setupPrivateMessageButton(button, profile))

        AvatarCache.getAvatarBitmapFromCache(activity, basicInfo) match {
          case Some(avatarBitmap) => setAvatarFromCache(avatarBitmap)
          case None => setAvatarFromNetwork(activity, basicInfo)
        }
        editButtonHolder.foreach(_.setVisible(true))
        loadingIndicatorHolder.foreach(_.hide())
        alertButtonHolder.foreach { button =>
          button.setOnClickListener { view: View => 
            AlertListActivity.startActivity(activity)
          }
        }
        alertCountHolder.foreach { alertCountText =>
          val visibility = if (alertCount == 0) View.GONE else View.VISIBLE
          alertCountText.setText(alertCount.toString)
          alertCountText.setVisibility(visibility)
        }
        activity.onProfileFetchedOK()
      }

    }

    userProfile.onFailureInUI { case e: Exception =>
      if (activity != null) {
        showErrorNotice(activity.getString(R.string.fragmentUserProfileError))
        activity.onProfileFetchedFailure(e)
      }
    }
  }

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle): View = {
    val view = inflater.inflate(R.layout.fragment_current_user_profile, container, false)
    updateProfile()
    view
  }
}
