package idv.brianhsu.maidroid.plurk.activity

import idv.brianhsu.maidroid.plurk._
import idv.brianhsu.maidroid.plurk.TypedResource._
import idv.brianhsu.maidroid.plurk.adapter._
import idv.brianhsu.maidroid.plurk.dialog._
import idv.brianhsu.maidroid.plurk.fragment._
import idv.brianhsu.maidroid.plurk.util._
import idv.brianhsu.maidroid.plurk.view._

import idv.brianhsu.maidroid.ui.util.AsyncUI._
import idv.brianhsu.maidroid.ui.model._

import android.app.Activity
import android.widget.Toast
import android.content.Intent
import android.content.DialogInterface
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.net.Uri
import android.text.Editable

import android.support.v7.app.ActionBarActivity
import android.support.v7.app.ActionBar
import android.support.v7.app.ActionBar.TabListener
import android.support.v7.app.ActionBar.Tab
import android.support.v4.app.FragmentTransaction
import android.support.v4.view.ViewPager
import android.support.v4.view.ViewPager.OnPageChangeListener

import scala.concurrent._

class PostPlurkActivity extends ActionBarActivity 
                        with TabListener with OnPageChangeListener
                        with TypedViewHolder 
                        with SelectEmoticonActivity
                        with SelectImageActivity
                        with EmoticonFragment.Listener
                        with SelectLimitedToDialog.Listener
                        with SelectBlockPeopleDialog.Listener
                        with PostPublicFragment.Listener
                        with ConfirmDialog.Listener
{

  protected lazy val plurkAPI = PlurkAPIHelper.getPlurkAPI(this)
  protected lazy val dialogFrame = ToggleView.setupAngryBehavior(this, findView(TR.activityPostPlurkDialogFrame))
  protected val emoticonFragmentHolderResID = R.id.activityPostPlurkEmtoicon

  private lazy val viewPager = findView(TR.activityPostPlurkViewPager)
  private lazy val adapter = new PlurkEditorAdapter(getSupportFragmentManager)

  private lazy val rootView = findView(TR.activityPostPlurkRoot)
  private var currentPage: Int = 0

  private var prevEditorContentHolder: Option[(Editable, Int)] = None
  private var isSliding: Boolean = false

  protected def getCurrentEditor = {
    val tag = s"android:switcher:${R.id.activityPostPlurkViewPager}:${viewPager.getCurrentItem}"
    getSupportFragmentManager().findFragmentByTag(tag).asInstanceOf[PlurkEditor]
  }

  override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_post_plurk)
    val actionbar = getSupportActionBar

    actionbar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS)

    actionbar.addTab(
      actionbar.newTab().setText(R.string.activityPostPlurkPublic).setTabListener(this)
    )
    actionbar.addTab(
      actionbar.newTab().setText(R.string.activityPostPlurkPrivate).setTabListener(this)
    )
    actionbar.addTab(
      actionbar.newTab().setText(R.string.activityPostPlurkBackstab).setTabListener(this)
    )
 
    viewPager.setAdapter(adapter)
    viewPager.setOnPageChangeListener(this)

    dialogFrame.setMessages(
      Message(MaidMaro.Half.Happy, getString(R.string.activityPostPlurkWelcome01)) :: 
      Message(MaidMaro.Half.Smile, getString(R.string.activityPostPlurkWelcome02)) ::
      Nil
    )

    if (savedInstanceState != null) {
      val isEmoticonSelectorShown = 
        savedInstanceState.getBoolean(SelectEmoticonActivity.IsEmoticonSelectorShown, false)

      setSelectorVisibility(isEmoticonSelectorShown)
    }
  }

  override def onCreateOptionsMenu(menu: Menu): Boolean = {
    val inflater = getMenuInflater
    inflater.inflate(R.menu.activity_post_plurk, menu)
    super.onCreateOptionsMenu(menu)
  }

  override def onOptionsItemSelected(menuItem: MenuItem): Boolean = menuItem.getItemId match {
    case R.id.activityPostPlurkActionEmoticon => toggleEmoticonSelector(); false
    case R.id.activityPostPlurkActionPhotoFromGallery => startPhotoPicker(); false
    case R.id.activityPostPlurkActionPhotoFromCamera => startCamera(); false
    case R.id.activityPostPlurkActionSend => postPlurk(); false
    case R.id.activityPostPlurkActionLogout => Logout.logout(this); false
    case R.id.activityPostPlurkActionAbout => AboutActivity.startActivity(this); false
    case R.id.activityPostPlurkActionToggleMaid => ToggleView(this, dialogFrame); false
    case _ => super.onOptionsItemSelected(menuItem)
  }

  override def onTabReselected(tab: Tab, ft: FragmentTransaction) {}
  override def onTabUnselected(tab: Tab, ft: FragmentTransaction) {
  }

  override def onPageScrolled(
    position: Int, positionOffset: Float, 
    positionOffsetPixels: Int) {}

  override def onTabSelected(tab: Tab, ft: FragmentTransaction) {

    if (!isSliding) {
      for {
        editor <- Option(getCurrentEditor)
      } {
        prevEditorContentHolder = editor.getEditorContent
      }
    }

    viewPager.setCurrentItem(tab.getPosition, true)

    if (!isSliding) {
      prevEditorContentHolder.foreach { content =>
        getCurrentEditor.setEditorContent(content)
      }

      showBackstabIntro()
    }

  }

  override def onPageScrollStateChanged(state: Int) {
    if (state == ViewPager.SCROLL_STATE_IDLE) {
      prevEditorContentHolder.foreach { content =>
        getCurrentEditor.setEditorContent(content)
      }

      showBackstabIntro()

    } else if (state == ViewPager.SCROLL_STATE_DRAGGING ) {
      prevEditorContentHolder = getCurrentEditor.getEditorContent
    }
  }

  private def showBackstabIntro() {
    if (currentPage == 2) {
      dialogFrame.setMessages(
        Message(MaidMaro.Half.Smile, getString(R.string.activityPostPlurkBackstabIntro01)) ::
        Message(MaidMaro.Half.Smile, getString(R.string.activityPostPlurkBackstabIntro02)) ::
        Message(MaidMaro.Half.Happy, getString(R.string.activityPostPlurkBackstabIntro03)) ::
        Message(MaidMaro.Half.Happy, getString(R.string.activityPostPlurkBackstabIntro04)) ::
        Nil
      )
    }
  }

  override def onPageSelected(position: Int) {
    isSliding = true
    getSupportActionBar.setSelectedNavigationItem(position)
    isSliding = false
    currentPage = position
  }

  override def onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {

    super.onActivityResult(requestCode, resultCode, data)

    requestCode match {
      case SelectImageActivity.RequestPhotoPicker => processImage(resultCode, data)
      case _ => 
    }
  }

  override def onPeopleSelected(selectedCliques: Set[String], 
                                selectedUsers: Set[(Long, String)]) {
    val editor = getCurrentEditor
    editor.setSelected(selectedCliques, selectedUsers)
  }

  override def onBlockSelected(selectedCliques: Set[String], 
                                selectedUsers: Set[(Long, String)]) {
    val editor = getCurrentEditor
    editor.setBlocked(selectedCliques, selectedUsers)
  }

  override def onActionSendImage(uri: Uri) {
    uploadFile(uri)
  }

  def onActionSendMultipleImage(uriList: List[Uri]) {
    uploadFiles(uriList)
  }

  private def postPlurk() {
    val contentLength = getCurrentEditor.getContentLength

    if (contentLength == 0) {
      dialogFrame.setMessages(
        Message(MaidMaro.Half.Normal, getString(R.string.utilPlurkEditorEmptyNoticeMessage)) :: 
        Nil
      )

    } else if (contentLength > 210) {

      dialogFrame.setMessages(
        Message(MaidMaro.Half.Normal, getString(R.string.utilPlurkEditorOver210Message1)) ::
        Message(MaidMaro.Half.Smile, getString(R.string.utilPlurkEditorOver210Message2)) ::
        Nil
      )

    } else {

      val oldRequestedOrientation = getRequestedOrientation
      setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED)

      val progressDialogFragment = ProgressDialogFragment.createDialog(
        getString(R.string.activityPostPlurkPosting),
        getString(R.string.pleaseWait)
      )
      progressDialogFragment.show(getSupportFragmentManager.beginTransaction, "postProgress")

      val postedPlurkFuture = getCurrentEditor.postPlurk()

      postedPlurkFuture.onSuccessInUI { case content =>
        setResult(Activity.RESULT_OK)
        progressDialogFragment.dismiss()
        setRequestedOrientation(oldRequestedOrientation)
        Toast.makeText(
          this, getString(R.string.activityPostPlurkPosted), 
          Toast.LENGTH_LONG
        ).show()
        finish()
      }

      postedPlurkFuture.onFailureInUI { case e =>
        setResult(Activity.RESULT_CANCELED)
        progressDialogFragment.dismiss()
        setRequestedOrientation(oldRequestedOrientation)
        dialogFrame.setMessages(
          Message(MaidMaro.Half.Panic, getString(R.string.activityPostPlurkFailure01)) :: 
          Message(MaidMaro.Half.Normal, getString(R.string.activityPostPlurkFailure02).format(e.getMessage)) ::
          Nil
        )
      }
    }
  }

  private def showWarningDialog() {

    val alertDialog = ConfirmDialog.createDialog(
      this, 'ExitConfirm, 
      getString(R.string.cancel),
      getString(R.string.activityPostPlurkAbortConfirm),
      getString(R.string.yes), getString(R.string.no)
    ) 

    alertDialog.show(getSupportFragmentManager, "ExitConfirm")
  }

  override def onDialogOKClicked(dialogName: Symbol, dialog: DialogInterface, data: Bundle) {
    dialogName match {
      case 'LogoutConfirm => 
        dialog.dismiss()
        this.finish()
        Logout.doLogout(this)
      case 'ExitConfirm =>
        setResult(Activity.RESULT_CANCELED)
        dialog.dismiss()
        finish()
    }
  }

  override def onBackPressed() {

    val isConsumed = onBackPressedInEmoticonSelector()

    if (!isConsumed) {
      showWarningDialog()
    }
  }

  override def onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putBoolean(SelectEmoticonActivity.IsEmoticonSelectorShown, isSelectorShown)
  }

  override def onResume() {
    super.onResume()
    ToggleView.syncDialogVisibility(this, dialogFrame)
  }


}
