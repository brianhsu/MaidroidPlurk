package idv.brianhsu.maidroid.plurk.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.ActionBarActivity
import idv.brianhsu.maidroid.plurk._
import idv.brianhsu.maidroid.plurk.adapter._
import idv.brianhsu.maidroid.plurk.fragment._
import idv.brianhsu.maidroid.plurk.TypedResource._
import idv.brianhsu.maidroid.ui.util.CallbackConversions._
import org.bone.soplurk.model.User


object UserTimelineActivity {

  val ExtraUserID = "ExtraUserID"
  val ExtraUserDisplayName = "ExtraDisplayName"
  val ExtraUserNickname = "ExtraUserNickname"

  def startActivity(context: Context, user: User) {
    val intent = new Intent(context, classOf[UserTimelineActivity])
    intent.putExtra(ExtraUserID, user.id)
    intent.putExtra(ExtraUserNickname, user.nickname)
    user.displayName.foreach(intent.putExtra(ExtraUserDisplayName, _))
    context.startActivity(intent)
  }
}


class UserTimelineActivity extends ActionBarActivity with TypedViewHolder
{

  import UserTimelineActivity._

  private lazy val userID = getIntent.getLongExtra(ExtraUserID, -1)

  private lazy val viewPager = findView(TR.activityUserTimelineViewPager)
  private lazy val pagerIndicator = findView(TR.activityUserTimelinePagerIndicator)
  private lazy val pageAdapter = new UserTimelineAdapter(this, getSupportFragmentManager, userID)

  override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)

    setContentView(R.layout.activity_user_timeline)
    val intent = getIntent
    val nickname = Option(intent.getStringExtra(ExtraUserNickname))
    val displayName = Option(intent.getStringExtra(ExtraUserDisplayName))
    val titleName = (displayName orElse nickname).getOrElse(userID)
    val activityTitle = this.getString(R.string.titleUserTimeline).format(titleName)

    setTitle(activityTitle)
    viewPager.setAdapter(pageAdapter)
    pagerIndicator.setViewPager(viewPager)
  }
}
