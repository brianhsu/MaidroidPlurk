package idv.brianhsu.maidroid.plurk.activity

import idv.brianhsu.maidroid.plurk._
import idv.brianhsu.maidroid.plurk.TypedResource._
import idv.brianhsu.maidroid.plurk.adapter._
import idv.brianhsu.maidroid.plurk.dialog._
import idv.brianhsu.maidroid.plurk.fragment._
import idv.brianhsu.maidroid.plurk.util._
import idv.brianhsu.maidroid.ui.util.AsyncUI._
import idv.brianhsu.maidroid.ui.model._

import android.app.Activity
import android.widget.Toast
import android.content.Intent
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
{

  protected lazy val plurkAPI = PlurkAPIHelper.getPlurkAPI(this)
  protected lazy val dialogFrame = findView(TR.activityPostPlurkDialogFrame)
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
    actionbar.addTab(actionbar.newTab().setText("一般").setTabListener(this))
    actionbar.addTab(actionbar.newTab().setText("私噗").setTabListener(this))
    actionbar.addTab(actionbar.newTab().setText("背刺").setTabListener(this))
 
    viewPager.setAdapter(adapter)
    viewPager.setOnPageChangeListener(this)

    dialogFrame.setMessages(
      Message(MaidMaro.Half.Happy, "主人有有趣的事情想要和大家分享嗎？請告訴小鈴，小鈴會幫主人發到噗浪上喔！", None) :: 
      Message(MaidMaro.Half.Smile, "如果主人只想和某些特定的好友分享的話，也可以切換到「私噗」標籤，選擇要和誰分享喲。") ::
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
    inflater.inflate(R.menu.post_plurk, menu)
    super.onCreateOptionsMenu(menu)
  }

  override def onOptionsItemSelected(menuItem: MenuItem): Boolean = menuItem.getItemId match {
    case R.id.postPlurkActionEmoticon => toggleEmoticonSelector(); false
    case R.id.postPlurkActionPhotoFromGallery => startPhotoPicker(); false
    case R.id.postPlurkActionPhotoFromCamera => startCamera(); false
    case R.id.postPlurkActionSend => postPlurk(); false
    case R.id.postPlurkActionLogout => Logout.logout(this); false
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
        Message(MaidMaro.Half.Smile, "主人是不是很好奇「背刺」是什麼呢？其實這是小鈴為主人提供的特別發文服務喲！", None) ::
        Message(MaidMaro.Half.Smile, "不知道主人有沒有過想要說一些不能被家人或同事知道的話，但因為家人是噗浪上的好友，所以沒辦法暢所欲言的情況呢？", None) ::
        Message(MaidMaro.Half.Happy, "小鈴幫主人想了一個辦法，就是這個「背刺」的功能喲。主人除了可以選擇誰可以看到這則發文外，也可以選擇誰不能看喲！", None) ::
        Message(MaidMaro.Half.Happy, "舉例來講，主人可以設定 A 看不到這則發文，而「同學」的小圈圈可以看到，這樣就算 A 在「同學」這個小圈圈裡，他也還是看不到的喔。", None) ::
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
    uploadFile(getFileFromUri(uri))
  }

  def onActionSendMultipleImage(uriList: List[Uri]) {
    uploadFiles(uriList.map(getFileFromUri))
  }

  private def postPlurk() {
    val contentLength = getCurrentEditor.getContentLength

    if (contentLength == 0) {
      dialogFrame.setMessages(
        Message(MaidMaro.Half.Normal, "主人還沒有填寫內容呢，這樣小鈴沒辦法幫主人發到噗浪上喲……", None) :: 
        Nil
      )

    } else if (contentLength > 210) {

      dialogFrame.setMessages(
        Message(MaidMaro.Half.Normal, "對不起，字數超過噗浪的 210 個字元的上限了呢……", None) ::
        Message(MaidMaro.Half.Smile, "主人要不要先刪除一些贅字，或試著寫得精鍊一些呢？", None) ::
        Nil
      )

    } else {

      val progressDialogFragment = new ProgressDialogFragment("發噗中", "請稍候……")
      progressDialogFragment.show(getSupportFragmentManager.beginTransaction, "postProgress")
      val oldRequestedOrientation = getRequestedOrientation
      setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED)

      val postedPlurkFuture = getCurrentEditor.postPlurk()

      postedPlurkFuture.onSuccessInUI { case content =>
        setResult(Activity.RESULT_OK)
        progressDialogFragment.dismiss()
        setRequestedOrientation(oldRequestedOrientation)
        Toast.makeText(this, "已成功發送至噗浪", Toast.LENGTH_LONG).show()
        finish()
      }

      postedPlurkFuture.onFailureInUI { case e =>
        setResult(Activity.RESULT_CANCELED)
        progressDialogFragment.dismiss()
        setRequestedOrientation(oldRequestedOrientation)
        dialogFrame.setMessages(
          Message(MaidMaro.Half.Panic, "對不起！小鈴太沒用了，沒辦法順利幫主人把這則噗放到噗浪上……", None) :: 
          Message(MaidMaro.Half.Normal, s"系統說錯誤的原因是：${e}，可不可以請主人檢查一次之後再重新按發送鍵一次呢？") ::
          Nil
        )
      }
    }
  }

  private def showWarningDialog() {

    val alertDialog = ConfirmDialog.createDialog(
      this, "取消", "確定要取消發噗嗎？這會造成目前的內容永遠消失喲！", "是", "否"
    ) { dialog =>
      setResult(Activity.RESULT_CANCELED)
      dialog.dismiss()
      PostPlurkActivity.this.finish()
    }

    alertDialog.show()
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

}
