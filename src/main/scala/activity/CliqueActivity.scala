package idv.brianhsu.maidroid.plurk.activity

import android.content.Context
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.ActionBarActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import idv.brianhsu.maidroid.plurk._
import idv.brianhsu.maidroid.plurk.adapter._
import idv.brianhsu.maidroid.plurk.dialog._
import idv.brianhsu.maidroid.plurk.fragment._
import idv.brianhsu.maidroid.plurk.TypedResource._
import idv.brianhsu.maidroid.plurk.util.Logout
import idv.brianhsu.maidroid.plurk.util.PlurkAPIHelper
import idv.brianhsu.maidroid.plurk.view.ToggleView
import idv.brianhsu.maidroid.plurk.view.PlurkView
import idv.brianhsu.maidroid.ui.model._
import idv.brianhsu.maidroid.ui.util.AsyncUI._
import idv.brianhsu.maidroid.ui.util.CallbackConversions._
import org.bone.soplurk.model.User
import org.bone.soplurk.model.Plurk
import org.bone.soplurk.api.PlurkAPI.Timeline
import scala.concurrent._


object CliqueActivity {
  def startActivity(context: Context, cliqueName: String) {
    val intent = new Intent(context, classOf[CliqueActivity])
    intent.putExtra("CLIQUE_NAME", cliqueName)
    context.startActivity(intent)
  }

}

class CliqueActivity extends ActionBarActivity 
                     with ConfirmDialog.Listener
                     with TypedViewHolder 
                     with SelectLimitedToDialog.Listener
{
  private def plurkAPI = PlurkAPIHelper.getPlurkAPI(this)
  private implicit val mActitiy = this

  private lazy val dialogFrame = ToggleView.setupAngryBehavior(this, findView(TR.activityCliqueDialogFrame))
  private lazy val cliqueName = getIntent.getStringExtra("CLIQUE_NAME")
  private lazy val listView = findView(TR.userListListView)
  private lazy val loadingIndicator = findView(TR.userListLoadingIndicator)
  private lazy val errorNotice = findView(TR.userListErrorNotice)
  private lazy val emptyNotice = findView(TR.userListEmptyNotice)
  private lazy val retryButton = findView(TR.moduleErrorNoticeRetryButton)

  private def showErrorNotice(message: String) {
    loadingIndicator.hide()
    errorNotice.setVisibility(View.VISIBLE)
    errorNotice.setMessageWithRetry(message) { retryButton =>
      retryButton.setEnabled(false)
      errorNotice.setVisibility(View.GONE)
      loadingIndicator.show()
      updateList()
    }
  }

  private def removeUser(adapter: UserListAdapter, user: User) {
    val dialogBuilder = new AlertDialog.Builder(CliqueActivity.this)
    val displayName = (
      user.displayName.filterNot(_.trim.isEmpty) orElse 
      Option(user.fullName).filterNot(_.trim.isEmpty) orElse
      Option(user.nickname).filterNot(_.trim.isEmpty)
    ).getOrElse(user.id)

    val confirmDialog = 
        dialogBuilder.setTitle(R.string.activityCliqueDeleteTitle)
                     .setMessage(getString(R.string.activityCliqueDeleteMessage).format(displayName))
                     .setPositiveButton(R.string.ok, null)
                     .setNegativeButton(R.string.cancel, null)
                     .create()

    confirmDialog.setOnShowListener(new DialogInterface.OnShowListener() {
      override def onShow(dialog: DialogInterface) {
        val okButton = confirmDialog.getButton(DialogInterface.BUTTON_POSITIVE)
        okButton.setOnClickListener { view: View =>
          val progressDialog = ProgressDialog.show(
            CliqueActivity.this, 
            getString(R.string.pleaseWait), 
            getString(R.string.activityCliqueDeleting), 
            true, false
          )
          val future = Future { plurkAPI.Cliques.remove(cliqueName, user.id).get }

          future.onSuccessInUI { status => 
            adapter.removeUser(user.id) 
            progressDialog.dismiss()
            confirmDialog.dismiss()
            dialogFrame.setMessages(
              Message(MaidMaro.Half.Happy, getString(R.string.activityCliqueDeleteOK).format(displayName)) ::
              Nil
            )
          }

          future.onFailureInUI { case e: Exception => 
            Toast.makeText(CliqueActivity.this, R.string.activityCliqueDeleteFailed, Toast.LENGTH_LONG).show() 
            progressDialog.dismiss()
            confirmDialog.dismiss()
            dialogFrame.setMessages(
              Message(MaidMaro.Half.Panic, getString(R.string.activityCliqueDeleteFailed01)) ::
              Message(MaidMaro.Half.Normal, getString(R.string.activityCliqueDeleteFailed02).format(e.getMessage)) ::
              Nil
            )
          }
        }
      }
    })

    confirmDialog.show()
  }


  private def updateList() {
    val future = Future { plurkAPI.Cliques.getClique(cliqueName).get.toVector }

    future.onSuccessInUI { users =>

      val adapter = new UserListAdapter(this, users)

      listView.setAdapter(adapter)
      listView.setEmptyView(emptyNotice)
      listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
        override def onItemClick(parent: AdapterView[_], view: View, position: Int, id: Long) {
          val user = adapter.getItem(position).asInstanceOf[User]
          UserTimelineActivity.startActivity(CliqueActivity.this, user)
        }
      })

      listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
        override def onItemLongClick(parent: AdapterView[_], view: View, position: Int, id: Long): Boolean = {
          val dialog = new AlertDialog.Builder(CliqueActivity.this)
          val itemList = Array(
            getString(R.string.activityCliqueViewTimeline),
            getString(R.string.activityCliqueDelete)
          )
          val itemAdapter = new ArrayAdapter(CliqueActivity.this, android.R.layout.select_dialog_item, itemList)
          val onClickListener = new DialogInterface.OnClickListener {
            override def onClick(dialog: DialogInterface, which: Int) {
              val user = adapter.getItem(position)
              which match {
                case 0 => UserTimelineActivity.startActivity(CliqueActivity.this, user)
                case 1 => removeUser(adapter, user)
              }
            }
          }
          dialog.setTitle(R.string.fragmentBlockListAction)
                .setAdapter(itemAdapter, onClickListener)
                .show()
          true
        }
      })

      loadingIndicator.setVisibility(View.GONE)
      dialogFrame.setMessages(
        Message(MaidMaro.Half.Happy, getString(R.string.activityCliqueFetchOK01).format(cliqueName)) ::
        Message(MaidMaro.Half.Smile, getString(R.string.activityCliqueFetchOK02)) ::
        Nil
      )

    }

    future.onFailureInUI { case e: Exception =>
      showErrorNotice(getString(R.string.activityCliqueFetchError))
      dialogFrame.setMessages(
        Message(MaidMaro.Half.Panic, getString(R.string.activityCliqueFetchFailure01)) ::
        Message(MaidMaro.Half.Normal, getString(R.string.activityCliqueFetchFailure02).format(e.getMessage)) ::
        Nil
      )

    }

  }

  override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_clique)
    setTitle(cliqueName)
    updateList()
    dialogFrame.setMessages(
      Message(MaidMaro.Half.Happy, getString(R.string.activityCliqueWelcome01)) ::
      Nil
    )
  }

  def onPeopleSelected(selectedCliques: Set[String], selectedUsers: Set[(Long, String)]) {

    val progressDialog = ProgressDialogFragment.createDialog(
      getString(R.string.pleaseWait),
      getString(R.string.activityCliqueAddingFriend)
    )

    progressDialog.show(getSupportFragmentManager, "progress")

    val future = Future {
      for {
        clique <- selectedCliques
        user <- plurkAPI.Cliques.getClique(clique).get
      } { 
        plurkAPI.Cliques.add(cliqueName, user.id).get
      }
      selectedUsers.foreach { case(userID, name) =>
        plurkAPI.Cliques.add(cliqueName, userID).get
      }
    }

    future.onSuccessInUI { status =>
      updateList()
      progressDialog.dismiss()
      dialogFrame.setMessages(
        Message(MaidMaro.Half.Happy, getString(R.string.activityCliqueAddOK)) ::
        Nil
      )
    }

    future.onFailureInUI { case e: Exception =>
      Toast.makeText(this, R.string.activityCliqueAddingFriendFailed, Toast.LENGTH_LONG).show()
      progressDialog.dismiss()
      dialogFrame.setMessages(
        Message(MaidMaro.Half.Panic, getString(R.string.activityCliqueAddFailed01)) ::
        Message(MaidMaro.Half.Normal, getString(R.string.activityCliqueAddFailed02).format(e.getMessage)) ::
        Nil
      )


    }
  }

  private def addToClique() {
    val dialog = new SelectPeopleDialog(R.string.activityCliqueChoose, Set.empty, Set.empty) {

      override def onDialogConfirmed(adapter: PeopleListAdapter) {
        val activityCallback = getActivity.asInstanceOf[SelectLimitedToDialog.Listener]
        activityCallback.onPeopleSelected(
          adapter.getSelectedCliques,
          adapter.getSelectedUsersWithTitle
        )
      }
    }

    dialog.show(getSupportFragmentManager, "Selection")
  }

  override def onResume() {
    super.onResume()
    ToggleView.syncDialogVisibility(this, dialogFrame)
  }

  override def onCreateOptionsMenu(menu: Menu): Boolean = {
    val inflater = getMenuInflater
    inflater.inflate(R.menu.activity_clique, menu)
    super.onCreateOptionsMenu(menu)
  }

  override def onOptionsItemSelected(menuItem: MenuItem): Boolean = menuItem.getItemId match {
    case R.id.activityCliqueAdd => addToClique(); false
    case R.id.activityCliqueLogout => Logout.logout(this); false
    case R.id.activityCliqueAbout => AboutActivity.startActivity(this); false
    case R.id.activityCliqueToggleMaid => ToggleView(this, dialogFrame); false
    case _ => super.onOptionsItemSelected(menuItem)
  }

  override def onDialogOKClicked(dialogName: Symbol, dialog: DialogInterface, data: Bundle) {
    dialogName match {
      case 'LogoutConfirm => 
        dialog.dismiss()
        this.finish()
        Logout.doLogout(this)
    }
  }


}
