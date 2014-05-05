package idv.brianhsu.maidroid.plurk.fragment

import idv.brianhsu.maidroid.plurk._
import idv.brianhsu.maidroid.plurk.TypedResource._
import idv.brianhsu.maidroid.plurk.util.PlurkAPIHelper
import idv.brianhsu.maidroid.plurk.util.DebugLog
import idv.brianhsu.maidroid.plurk.view.IconView

import idv.brianhsu.maidroid.plurk.TypedResource._
import idv.brianhsu.maidroid.ui.util.AsyncUI._

import android.app.Activity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.View
import android.os.Bundle
import android.widget.BaseAdapter

import android.support.v4.app.Fragment
import scala.concurrent._
import org.bone.soplurk.api.PlurkAPI.EmoticonsList
import org.bone.soplurk.model.Icon


import android.support.v4.view.PagerAdapter

import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.app.FragmentManager

case class EmoticonTabs(
  basicPage: Vector[Icon], morePage: Vector[Icon], 
  hiddenPage: Vector[Icon], customPage: Vector[Icon]
)

object EmoticonFragment {
  val hiddenName = Set(
    "(fireworks)", "(Русский)", "(goal)", "(code)", "(dance_bzz)", "(bzzz)",
    "(yarr)", "(yarr_okok)", "(gym_okok)", "(hungry_okok)", "(music_okok)", 
    "(wave_okok)", "(dance_okok)", "(code_okok)", "(angry_okok)", "(banana_gym)",
    "(dance_yarr)", "(no_dance)", "(taser_okok)", "(droid_dance)", "(fuu)", "(gfuu)",
    "(yay)", "(bah)", "(gyay)", "(gbah)", "(troll)", "(gtroll)", "(aha)", "(gaha)",
    "(whatever)", "(gwhatever)", "(pokerface)", "(gpokerface)", "(yea)", "(gyea)",
    "(jazzhands)", "(v_shy)", "(v_tiffany)", "(v_love)", "(v_perfume)", "(v_mail)",
    "(xmas1)", "(xmas2)", "(xmas3)", "(xmas4)", "(xmas5)", "(xmas6)", "(xmas7)",
    "(xmas8)", "(xmas9)", "(xmas10)", "(xmas11)", "(xmas12)", "(xmas13)", "(xmas14)",
    "(xmas15)", "(cny1)", "(cny2)", "(cny3)", "(cny4)", "(cny5)", "(cny6)", "(cny7)",
    "(cny8)", "(cny9)", "(cny10)", "(cny11)", "(cny12)"
  )
}

import android.widget.LinearLayout
import android.view.LayoutInflater

class IconGrid(activity: Activity, icons: Vector[Icon]) extends LinearLayout(activity) {
  private lazy val inflater = LayoutInflater.from(activity)
  inflater.inflate(R.layout.view_icon_grid, this, true)
}

import android.support.v4.view.PagerAdapter
class IconPagerAdapter(activity: Activity, tabs: EmoticonTabs)  extends PagerAdapter {

  val tabsGrid = Vector(
    tabs.customPage, tabs.basicPage, 
    tabs.morePage, tabs.hiddenPage
  ).map(new IconGrid(activity, _))

  override def getCount = 4
  override def instantiateItem(container: ViewGroup, position: Int): Object = {
    val iconGrid = tabsGrid(position)
    iconGrid.setTag(position)
    container.addView(iconGrid)
    position.toString
  }
  override def destroyItem(container: ViewGroup, position: Int, obj: Object) {
    container.removeView(tabsGrid(position))
  }
  override def isViewFromObject(view: View, obj: Object) = {
    view.getTag.toString == obj.toString
  }
 
  override def getPageTitle(position: Int) = position match {
    case 0 => "自訂表情"
    case 1 => "常用表情"
    case 2 => "更多表情"
    case 3 => "隱藏表情"
  }
}

class EmoticonFragment extends Fragment {

  private implicit def activity = getActivity

  private def loadingIndicatorHolder = Option(getView).map(_.findView(TR.moduleLoadingIndicator))
  private def errorNoticeHolder = Option(getView).map(_.findView(TR.fragmentEmoticonErrorNotice))

  private def viewPagerHolder = Option(getView).map(_.findView(TR.fragmentEmoticonViewPager))
  private def viewPagerIndicatorHolder = Option(getView).map(_.findView(TR.fragmentEmoticonViewPagerIndicator))

  private def plurkAPI = PlurkAPIHelper.getPlurkAPI(getActivity)
  private def getCurrentUserInfo = future { plurkAPI.Users.currUser.get._1 }
  private def getVerifiedEmoticons(userKarma: Double, 
                                   userRecruited: Int): Future[EmoticonTabs] = future {

    val EmoticonsList(custom, recruited, karma) = plurkAPI.Emoticons.get.get
    val verifiedRecruited = recruited.filterKeys(_ <= userRecruited)
    val verifiedKarma = karma.filterKeys(_ <= userKarma)

    val basicAndHidden = verifiedKarma.filterKeys(_ <= 25).flatMap(_._2).toVector
    val morePage = verifiedRecruited.flatMap(_._2).toVector ++ 
                   verifiedKarma.filterKeys(_ > 25).flatMap(_._2).toVector
    val customPage = custom.toVector
    val hiddenPage = basicAndHidden.filter(icon => EmoticonFragment.hiddenName.contains(icon.name))
    val basicPage = basicAndHidden.filterNot(hiddenPage contains _)

    EmoticonTabs(basicPage, morePage, hiddenPage, customPage)
  }

  private def getEmoticonTabs: Future[EmoticonTabs] = for {
    userInfo <- getCurrentUserInfo
    userKarma = userInfo.basicInfo.karma
    userRecruited = userInfo.recruited
    verifiedEmoticons <- getVerifiedEmoticons(userKarma, userRecruited)
  } yield verifiedEmoticons

  override def onActivityCreated(savedInstanceState: Bundle) {
    super.onActivityCreated(savedInstanceState)
  }

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, 
                            savedInstanceState: Bundle): View = {
    inflater.inflate(R.layout.fragment_emoticon, container, false)
  }

  private def setupTabs() {
    val tabFuture = getEmoticonTabs

    tabFuture.onSuccessInUI { iconTabs =>
      
      for {
        viewPager <- viewPagerHolder
        viewPagerIndicator <- viewPagerIndicatorHolder
      } {
        viewPager.setAdapter(new IconPagerAdapter(activity, iconTabs))
        viewPagerIndicator.setViewPager(viewPager)
      }

      loadingIndicatorHolder.foreach(_.setVisibility(View.GONE))
    }

    tabFuture.onFailureInUI { case e: Exception =>
      loadingIndicatorHolder.foreach(_.setVisibility(View.GONE))
      errorNoticeHolder.foreach { errorNotice =>
        errorNotice.setVisibility(View.VISIBLE)
        errorNotice.setMessageWithRetry("取得表情符號失敗") { retryButton => 
          errorNotice.setVisibility(View.GONE)
          loadingIndicatorHolder.foreach(_.setVisibility(View.VISIBLE))
          setupTabs()
        }
      }
    }
  }

  override def onViewCreated(view: View, savedInstanceState: Bundle) {
    setupTabs()
  }
}

