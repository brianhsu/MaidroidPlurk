package idv.brianhsu.maidroid.plurk.util

import android.content.Context
import idv.brianhsu.maidroid.plurk.R

import org.bone.soplurk.model.Plurk
import org.bone.soplurk.constant.Qualifier._
import org.bone.soplurk.constant.Qualifier

object QualifierDisplay {

  private def getString(context: Context, resID: Int) = context.getResources.getString(resID)

  def apply(qualifier: Qualifier, context: Context) = qualifier match {
    case Asks      => Some((0xff8361BC, getString(context, R.string.qAsks)))
    case Feels     => Some((0xff2D83BE, getString(context, R.string.qFeels)))
    case Gives     => Some((0xff620E0E, getString(context, R.string.qGives)))
    case Has       => Some((0xff777777, getString(context, R.string.qHas)))
    case Hates     => Some((0xff111111, getString(context, R.string.qHates)))
    case Hopes     => Some((0xffE05BE9, getString(context, R.string.qHopes)))
    case Is        => Some((0xffE57C43, getString(context, R.string.qIs)))
    case Likes     => Some((0xff8C8C8C, getString(context, R.string.qLikes)))
    case Loves     => Some((0xffB20C0C, getString(context, R.string.qLoves)))
    case Needs     => Some((0xff7A9A37, getString(context, R.string.qNeeds)))
    case Says      => Some((0xffE2560B, getString(context, R.string.qSays)))
    case Shares    => Some((0xffA74949, getString(context, R.string.qShares)))
    case Thinks    => Some((0xff689CC1, getString(context, R.string.qThinks)))
    case Wants     => Some((0xff8DB241, getString(context, R.string.qWants)))
    case Was       => Some((0xff525252, getString(context, R.string.qWas)))
    case Whispers  => Some((0xff32007E, getString(context, R.string.qWhispers)))
    case Will      => Some((0xffB46DB9, getString(context, R.string.qWill)))
    case Wishes    => Some((0xff5BB017, getString(context, R.string.qWishes)))
    case Wonders   => Some((0xff2E4E9E, getString(context, R.string.qWonders)))
    case _ => None
  }

  def apply(plurk: Plurk) = plurk.qualifier match {
    case Asks      => Some((0xff8361BC, plurk.qualifierTranslated.getOrElse(plurk.qualifier.name)))
    case Feels     => Some((0xff2D83BE, plurk.qualifierTranslated.getOrElse(plurk.qualifier.name)))
    case Gives     => Some((0xff620E0E, plurk.qualifierTranslated.getOrElse(plurk.qualifier.name)))
    case Has       => Some((0xff777777, plurk.qualifierTranslated.getOrElse(plurk.qualifier.name)))
    case Hates     => Some((0xff111111, plurk.qualifierTranslated.getOrElse(plurk.qualifier.name)))
    case Hopes     => Some((0xffE05BE9, plurk.qualifierTranslated.getOrElse(plurk.qualifier.name)))
    case Is        => Some((0xffE57C43, plurk.qualifierTranslated.getOrElse(plurk.qualifier.name)))
    case Likes     => Some((0xff8C8C8C, plurk.qualifierTranslated.getOrElse(plurk.qualifier.name)))
    case Loves     => Some((0xffB20C0C, plurk.qualifierTranslated.getOrElse(plurk.qualifier.name)))
    case Needs     => Some((0xff7A9A37, plurk.qualifierTranslated.getOrElse(plurk.qualifier.name)))
    case Says      => Some((0xffE2560B, plurk.qualifierTranslated.getOrElse(plurk.qualifier.name)))
    case Shares    => Some((0xffA74949, plurk.qualifierTranslated.getOrElse(plurk.qualifier.name)))
    case Thinks    => Some((0xff689CC1, plurk.qualifierTranslated.getOrElse(plurk.qualifier.name)))
    case Wants     => Some((0xff8DB241, plurk.qualifierTranslated.getOrElse(plurk.qualifier.name)))
    case Was       => Some((0xff525252, plurk.qualifierTranslated.getOrElse(plurk.qualifier.name)))
    case Whispers  => Some((0xff32007E, plurk.qualifierTranslated.getOrElse(plurk.qualifier.name)))
    case Will      => Some((0xffB46DB9, plurk.qualifierTranslated.getOrElse(plurk.qualifier.name)))
    case Wishes    => Some((0xff5BB017, plurk.qualifierTranslated.getOrElse(plurk.qualifier.name)))
    case Wonders   => Some((0xff2E4E9E, plurk.qualifierTranslated.getOrElse(plurk.qualifier.name)))
    case _ => None
  }

}

