package idv.brianhsu.maidroid.plurk.fragment

import idv.brianhsu.maidroid.plurk._
import idv.brianhsu.maidroid.plurk.activity._
import idv.brianhsu.maidroid.plurk.TypedResource._
import idv.brianhsu.maidroid.plurk.adapter._
import idv.brianhsu.maidroid.plurk.dialog._
import idv.brianhsu.maidroid.plurk.util._
import idv.brianhsu.maidroid.plurk.view._
import idv.brianhsu.maidroid.ui.util.AsyncUI._
import idv.brianhsu.maidroid.ui.util.CallbackConversions._
import org.bone.soplurk.constant.AlertType

import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.widget.ArrayAdapter
import android.content.DialogInterface
import android.os.Bundle
import android.net.Uri
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.View
import android.view.Menu
import android.view.MenuItem
import android.view.MenuInflater
import android.widget.AdapterView
import android.widget.Toast
import android.widget.EditText
import android.webkit.WebViewClient
import android.webkit.WebView
import android.support.v4.app.FragmentActivity
import android.support.v7.widget.SearchView
import android.support.v4.view.MenuItemCompat
import android.text.TextWatcher
import android.text.Editable

import org.bone.soplurk.api._
import org.bone.soplurk.api.PlurkAPI._
import org.bone.soplurk.model._

import scala.concurrent._
import scala.util.Try

class CliqueListFragment extends Fragment {

  private implicit def activity = getActivity.asInstanceOf[FragmentActivity]

  private def listViewHolder = Option(getView).map(_.findView(TR.userListListView))
  private def loadingIndicatorHolder = Option(getView).map(_.findView(TR.userListLoadingIndicator))
  private def errorNoticeHolder = Option(getView).map(_.findView(TR.userListErrorNotice))
  private def emptyNoticeHolder = Option(getView).map(_.findView(TR.userListEmptyNotice))
  private def retryButtonHolder = Option(getView).map(_.findView(TR.moduleErrorNoticeRetryButton))
  private var adapterHolder: Option[CliqueListAdapter] = None

  private def plurkAPI = PlurkAPIHelper.getPlurkAPI(activity)

  private def showErrorNotice(message: String) {
    loadingIndicatorHolder.foreach(_.hide())
    errorNoticeHolder.foreach(_.setVisibility(View.VISIBLE))
    errorNoticeHolder.foreach { errorNotice =>
      errorNotice.setMessageWithRetry(message) { retryButton =>
        retryButton.setEnabled(false)
        errorNoticeHolder.foreach(_.setVisibility(View.GONE))
        loadingIndicatorHolder.foreach(_.show())
        updateList()
      }
    }
  }

  private def removeClique(adapter: CliqueListAdapter, cliqueName: String) {
    val dialogBuilder = new AlertDialog.Builder(activity)
    val confirmDialog = 
        dialogBuilder.setTitle(R.string.fragmentCliqueListDeleteTitle)
                     .setMessage(activity.getString(R.string.fragmentCliqueListDeleteMessage).format(cliqueName))
                     .setPositiveButton(R.string.ok, null)
                     .setNegativeButton(R.string.cancel, null)
                     .create()

    confirmDialog.setOnShowListener(new DialogInterface.OnShowListener() {
      override def onShow(dialog: DialogInterface) {
        val okButton = confirmDialog.getButton(DialogInterface.BUTTON_POSITIVE)
        okButton.setOnClickListener { view: View =>
          val progressDialog = ProgressDialog.show(
            activity, 
            activity.getString(R.string.pleaseWait), 
            activity.getString(R.string.fragmentCliqueListDeleting), 
            true, false
          )
          val future = Future { plurkAPI.Cliques.deleteClique(cliqueName).get }

          future.onSuccessInUI { status => 
            if (activity != null) {
              adapter.removeClique(cliqueName) 
              progressDialog.dismiss()
              confirmDialog.dismiss()
            }
          }

          future.onFailureInUI { case e: Exception => 
            if (activity != null) {
              Toast.makeText(activity, R.string.fragmentCliqueListDeleteFailed, Toast.LENGTH_LONG).show() 
              progressDialog.dismiss()
              confirmDialog.dismiss()
            }
          }
        }
      }
    })

    confirmDialog.show()
  }


  def updateList() {
    val future = Future { plurkAPI.Cliques.getCliques.get.toVector }
    future.onSuccessInUI { cliqueList =>

      if (activity != null) {

        val adapter = new CliqueListAdapter(activity, cliqueList)
        adapterHolder = Some(adapter)

        listViewHolder.foreach { listView =>
          listView.setAdapter(adapter)
          emptyNoticeHolder.foreach(view => listView.setEmptyView(view))
          /*
          listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            override def onItemClick(parent: AdapterView[_], view: View, position: Int, id: Long) {
              val alert = adapter.getItem(position).asInstanceOf[Alert]
              UserTimelineActivity.startActivity(activity, alert.user)
            }
          })
          */

          listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

            override def onItemLongClick(parent: AdapterView[_], view: View, position: Int, id: Long): Boolean = {
              val dialog = new AlertDialog.Builder(activity)
              val itemList = Array(
                activity.getString(R.string.fragmentCliqueListView),
                activity.getString(R.string.fragmentCliqueListDelete)
              )
              val itemAdapter = new ArrayAdapter(activity, android.R.layout.select_dialog_item, itemList)
              val onClickListener = new DialogInterface.OnClickListener {
                override def onClick(dialog: DialogInterface, which: Int) {
                  val clique = adapter.getItem(position)
                  which match {
                    case 0 => //UserTimelineActivity.startActivity(activity, user)
                    case 1 => removeClique(adapter, clique)
                  }
                }
              }
              dialog.setTitle(R.string.fragmentCliqueListAction)
                    .setAdapter(itemAdapter, onClickListener)
                    .show()
              true
            }
          })

        }
        loadingIndicatorHolder.foreach(_.setVisibility(View.GONE))
      }
    }

    future.onFailureInUI { case e: Exception =>
      if (activity != null) {
        showErrorNotice(activity.getString(R.string.fragmentCliqueListFetchFailure))
      }
    }

  }

  private def addClique() {
    val dialogBuilder = new AlertDialog.Builder(activity)
    val editText = new EditText(activity)
    editText.setSingleLine(true)
    editText.setHint(R.string.fragmentCliqueListAddHint)
    val confirmDialog = 
        dialogBuilder.setTitle(R.string.fragmentCliqueListAddTitle)
                     .setView(editText)
                     .setPositiveButton(R.string.ok, null)
                     .setNegativeButton(R.string.cancel, null)
                     .create()

    confirmDialog.setOnShowListener(new DialogInterface.OnShowListener() {
      override def onShow(dialog: DialogInterface) {

        val okButton = confirmDialog.getButton(DialogInterface.BUTTON_POSITIVE)

        okButton.setOnClickListener { view: View =>

          val cliqueName = editText.getText.toString
          val progressDialog = ProgressDialog.show(
            activity, 
            activity.getString(R.string.pleaseWait), 
            activity.getString(R.string.fragmentCliqueListAdding), 
            true, false
          )

          val future = Future { plurkAPI.Cliques.createClique(cliqueName).get }

          future.onSuccessInUI { status => 
            if (activity != null) {
              adapterHolder.foreach(_.addClique(cliqueName))
              progressDialog.dismiss()
              confirmDialog.dismiss()
            }
          }

          future.onFailureInUI { case e: Exception => 
            if (activity != null) {
              Toast.makeText(activity, R.string.fragmentCliqueListAddFailed, Toast.LENGTH_LONG).show() 
              progressDialog.dismiss()
              confirmDialog.dismiss()
            }
          }
        }
      }
    })

    editText.addTextChangedListener(new TextWatcher{
      override def afterTextChanged(s: Editable) {
        val shouldEnableOKButton = s.toString.trim.size > 0
        confirmDialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(shouldEnableOKButton)
      }
      override def beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
      override def onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
    })

    confirmDialog.show()

  }

  override def onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
    inflater.inflate(R.menu.fragment_clique_list, menu)
    super.onCreateOptionsMenu(menu, inflater)
  }

  override def onOptionsItemSelected(item: MenuItem) = item.getItemId match {
    case R.id.fragmentCliqueListAdd => addClique(); false
    case _ => super.onOptionsItemSelected(item)
  }
 

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, 
                            savedInstanceState: Bundle): View = {
    val view = inflater.inflate(R.layout.fragment_user_list, container, false)
    setHasOptionsMenu(true)
    updateList()
    view
  }

}

