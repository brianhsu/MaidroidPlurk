package idv.brianhsu.maidroid.plurk.adapter

import idv.brianhsu.maidroid.plurk._
import idv.brianhsu.maidroid.plurk.TypedResource._
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

class ResponseAdapter(activity: Activity, plurk: Plurk, owner: User) extends BaseAdapter {

  private implicit val mActivity = activity

  private lazy val replurker = for {
    replurkerID <- plurk.replurkInfo.replurkerID
    friends <- friendsHolder
    replurkUser <- friends.get(replurkerID)
  } yield replurkUser

  private var responsesHolder: Option[Vector[Response]] = None
  private var friendsHolder: Option[Map[Long, User]] = None
  private val textViewImageGetter = new PlurkImageGetter(activity, this)

  def getCount = responsesHolder.map(_.size).getOrElse(0) + 2

  def getItem(position: Int) = position match {
    case 0 => plurk
    case 1 => None
    case n => responsesHolder.get(n-2)
  }

  def getItemId(position: Int) = position match {
    case 0 => plurk.plurkID
    case 1 => None.hashCode
    case n => responsesHolder.get(n-2).id
  }

  def getPlurkView(convertView: View): View = {

    val itemView = convertView match {
      case view: PlurkView => view
      case _ => new PlurkView(true)
    }

    itemView.update(plurk, owner, replurker, textViewImageGetter)
    itemView
  }

  def getResponseView(response: Response, convertView: View): View = {

    val itemView = convertView match {
      case view: ResponseView => view
      case _ => new ResponseView(this)
    }

    val owner = friendsHolder.get(response.userID)
    val isDeletable = 
      (plurk.userID == plurk.ownerID)  || // Comment is in current user's plurk
      (plurk.userID == response.userID)   // Comment wrote by current user

    itemView.update(response, owner, isDeletable, textViewImageGetter)
    itemView
  }

  def getHeaderView(convertView: View, parent: ViewGroup): View = {
    val infalter = activity.getLayoutInflater
    val view = infalter.inflate(R.layout.item_header_response, parent, false)
    val loadingIndicator = view.findView(TR.itemHeaderResponseLoadingIndicator)
    val emptyNotice = view.findView(TR.itemHeaderResponseEmptyNotice)

    if (responsesHolder.isEmpty) {
      loadingIndicator.setVisibility(View.VISIBLE)
      emptyNotice.setVisibility(View.GONE)
    } else if (responsesHolder.map(_.isEmpty).getOrElse(false)){
      loadingIndicator.setVisibility(View.GONE)
      emptyNotice.setVisibility(View.VISIBLE)
    } else {
      loadingIndicator.setVisibility(View.GONE)
      emptyNotice.setVisibility(View.GONE)
    }

    view
  }
  
  def getView(position: Int, convertView: View, parent: ViewGroup): View = {

    position match {
      case 0 => getPlurkView(convertView)
      case 1 => getHeaderView(convertView, parent)
      case n => getResponseView(responsesHolder.get(n-2), convertView)
    }
  }

  def update(responses: List[Response], friends: Map[Long, User]) {
    this.responsesHolder = Some(responses.toVector)
    this.friendsHolder = Some(friends)
    notifyDataSetChanged()
  }

  def deleteResponse(responseID: Long) {
    this.responsesHolder = this.responsesHolder.map(_.filterNot(_.id == responseID))
    PlurkView.updatePlurkCommentInfo(
      plurk.plurkID, 
      this.responsesHolder.map(_.size).getOrElse(0), 
      true
    )
    notifyDataSetChanged()
  }

}

