import android.Keys._
import android.Dependencies.{apklib,aar}

android.Plugin.androidBuild
 
name := "MaidroidPlurk"

version := "0.0.1"
 
scalaVersion := "2.10.4"

organization := "idv.brianhsu.maidroid.plurk"

scalacOptions := Seq("-feature")

resolvers ++= Seq(
  "populov" at "http://dl.bintray.com/populov/maven",
  "brianhsu" at "http://bone.twbbs.org.tw/ivy",
  "scribe-java-mvn-repo" at "https://raw.github.com/fernandezpablo85/scribe-java/mvn-repo/"
)

libraryDependencies ++= Seq(
  aar("idv.brianhsu.maidroid.ui" % "maidroidui_2.10" % "0.0.1"),
  aar("com.viewpagerindicator" % "library" % "2.4.1"),
  "com.android.support" % "support-v4" % "19.1.+"
)

libraryDependencies ++= Seq(
  "org.bone" %% "soplurk" % "0.2.1"
)

platformTarget in Android := "android-19"

proguardScala in Android := true

proguardOptions in Android ++= Seq(
  "-dontwarn sun.misc.Unsafe",
  "-dontwarn sun.reflect.Reflection",
  "-dontwarn javax.xml.bind.DatatypeConverter",
  "-dontwarn javax.inject.Named",
  "-dontwarn org.apache.commons.codec.binary.Base64"
)
 
run <<= run in Android
 
install <<= install in Android
