import android.Keys._
import android.Dependencies.{apklib,aar}

android.Plugin.androidBuild
 
name := "MaidroidPlurk"

version := "0.1.6"
 
scalaVersion := "2.11.6"

organization := "idv.brianhsu.maidroid.plurk"

scalacOptions := Seq("-feature", "-deprecation")

resolvers ++= Seq(
  "populov" at "http://dl.bintray.com/populov/maven",
  "brianhsu" at "http://brianhsu.moe/ivy",
  "scribe-java-mvn-repo" at "https://raw.github.com/fernandezpablo85/scribe-java/mvn-repo/",
  "staging" at "https://oss.sonatype.org/content/groups/staging/"
)

libraryDependencies ++= Seq(
  aar("idv.brianhsu.maidroid.ui" % "maidroidui_2.11" % "0.0.7"),
  aar("net.rdrei.android.viewpagerindicator" % "library" % "2.5.0-SNAPSHOT"),
  aar("com.github.chrisbanes.actionbarpulltorefresh" % "library" % "0.9.3"),
  aar("com.github.chrisbanes.actionbarpulltorefresh" % "extra-abc" % "0.9.3"),
  aar("com.github.castorflex.smoothprogressbar" % "library" % "0.2.0"),
  aar("com.google.android.gms" % "play-services" % "5.0.89"),
  "com.android.support" % "support-v4" % "19.1.+",
  "com.android.support" % "appcompat-v7" % "19.1.+"
)

dependencyOverrides += "com.android.support" % "support-v4" % "19.1.+"

dependencyOverrides += "com.android.support" % "appcompat-v7" % "19.1.+"


libraryDependencies ++= Seq(
  "org.bone" %% "soplurk" % "0.3.6",
  "com.typesafe.akka" %% "akka-actor" % "2.3.2"
)

platformTarget in Android := "android-19"

proguardScala in Android := true

proguardOptions in Android ++= Seq(
  "-dontwarn sun.misc.Unsafe",
  "-dontwarn sun.reflect.Reflection",
  "-dontwarn javax.xml.bind.DatatypeConverter",
  "-dontwarn javax.inject.Named",
  "-dontwarn org.apache.commons.codec.binary.Base64",
  "-dontwarn akka.actor.**"
)
 
run <<= run in Android
 
install <<= install in Android
