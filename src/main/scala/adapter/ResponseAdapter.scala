package idv.brianhsu.maidroid.plurk.adapter

import idv.brianhsu.maidroid.plurk._
import idv.brianhsu.maidroid.plurk.fragment._
import idv.brianhsu.maidroid.plurk.util._
import idv.brianhsu.maidroid.plurk.view.ResponseView
import idv.brianhsu.maidroid.ui.util.AsyncUI._
import idv.brianhsu.maidroid.ui.util.CallbackConversions._

import scala.concurrent._

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.view.LayoutInflater
import android.widget.BaseAdapter
import android.graphics.BitmapFactory

import org.bone.soplurk.api.PlurkAPI._
import org.bone.soplurk.model._

import java.net.URL

class ResponseAdapter(activity: Activity, responses: Vector[Response], friends: Map[Long, User]) extends BaseAdapter {

  private implicit val mActivity = activity
  private val textViewImageGetter = new PlurkImageGetter(activity, this)

  def getCount = responses.size
  def getItem(position: Int) = responses(position)
  def getItemId(position: Int) = responses(position).id
  
  def getView(position: Int, convertView: View, parent: ViewGroup): View = {

    val itemView = convertView match {
      case view: ResponseView => view
      case _ => new ResponseView
    }

    val response = getItem(position)
    val owner = friends(response.userID)
    itemView.update(response, owner, textViewImageGetter)
    itemView
  }
}

