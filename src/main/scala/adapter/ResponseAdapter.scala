package idv.brianhsu.maidroid.plurk.adapter

import idv.brianhsu.maidroid.plurk._
import idv.brianhsu.maidroid.plurk.fragment._
import idv.brianhsu.maidroid.plurk.util._
import idv.brianhsu.maidroid.plurk.view.ResponseView
import idv.brianhsu.maidroid.plurk.view.PlurkView
import idv.brianhsu.maidroid.ui.util.AsyncUI._
import idv.brianhsu.maidroid.ui.util.CallbackConversions._

import scala.concurrent._

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.view.LayoutInflater
import android.widget.BaseAdapter
import android.widget.TextView

import android.graphics.BitmapFactory

import org.bone.soplurk.api.PlurkAPI._
import org.bone.soplurk.model._

import java.net.URL

class ResponseAdapter(activity: Activity, plurk: Plurk, owner: User, responses: Vector[Response], friends: Map[Long, User]) extends BaseAdapter {

  private implicit val mActivity = activity
  private val textViewImageGetter = new PlurkImageGetter(activity, this)

  def getCount = responses.size + 2

  def getItem(position: Int) = position match {
    case 0 => plurk
    case 1 => "Header"
    case n => responses(n-2)
  }

  def getItemId(position: Int) = position match {
    case 0 => plurk.plurkID
    case 1 => "Header".hashCode
    case n => responses(n-2).id
  }

  def getPlurkView(convertView: View): View = {

    val itemView = convertView match {
      case view: PlurkView => view
      case _ => new PlurkView(true)
    }

    itemView.update(plurk, owner, textViewImageGetter)
    itemView
  }

  def getResponseView(response: Response, convertView: View): View = {

    val itemView = convertView match {
      case view: ResponseView => view
      case _ => new ResponseView
    }

    val owner = friends(response.userID)
    itemView.update(response, owner, textViewImageGetter)
    itemView
  }

  def getHeaderView(convertView: View, parent: ViewGroup): View = {
    convertView match {
      case view: TextView => view
      case _ => 
        val infalter = activity.getLayoutInflater
        infalter.inflate(R.layout.item_header_response, parent, false)
    }
  }
  
  def getView(position: Int, convertView: View, parent: ViewGroup): View = {

    position match {
      case 0 => getPlurkView(convertView)
      case 1 => getHeaderView(convertView, parent)
      case n => getResponseView(responses(n-2), convertView)
    }
  }
}

