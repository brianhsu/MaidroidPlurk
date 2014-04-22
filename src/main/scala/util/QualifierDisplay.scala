package idv.brianhsu.maidroid.plurk.util

import org.bone.soplurk.model.Plurk
import org.bone.soplurk.constant.Qualifier._

object QualifierDisplay {

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

