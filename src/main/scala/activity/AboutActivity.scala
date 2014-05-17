package idv.brianhsu.maidroid.plurk.activity

import idv.brianhsu.maidroid.plurk._
import idv.brianhsu.maidroid.plurk.TypedResource._
import idv.brianhsu.maidroid.plurk.adapter._
import idv.brianhsu.maidroid.ui.util.CallbackConversions._

import android.os.Bundle
import android.content.Intent
import android.content.Context
import android.net.Uri
import android.view.View
import android.text.Html
import android.text.method.LinkMovementMethod

import android.support.v7.app.ActionBarActivity

import scala.util.Try

object AboutActivity {
  def startActivity(context: Context) {
    val intent = new Intent(context, classOf[AboutActivity])
    context.startActivity(intent)
  }
}

class AboutActivity extends ActionBarActivity with TypedViewHolder
{

  private lazy val viewPager = findView(TR.activityAboutViewPager)
  private lazy val pagerIndicator = findView(TR.activityAboutPagerIndicator)

  private def startBrowser(url: String) {
    val intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url))
    startActivity(intent)
  }

  private def getVersionName = Try {
    val packageName = getApplicationContext.getPackageName
    val packageManager = getApplicationContext.getPackageManager
    packageManager.getPackageInfo(packageName, 0).versionName
  }

  private def createAboutVersionPage() = {
    val aboutVersion = getLayoutInflater.inflate(R.layout.about_version, null)
    val versionText  = aboutVersion.findView(TR.aboutVersionText)
    val authorButton = aboutVersion.findView(TR.aboutVersionAuthor)
    val githubButton = aboutVersion.findView(TR.aboutVersionGithub)
    val marketButton = aboutVersion.findView(TR.aboutVersionMarket)

    val appNameWithVersion = getString(R.string.AppName) + getVersionName.getOrElse("O.O.O")

    versionText.setText(appNameWithVersion)
    authorButton.setOnClickListener { view: View => startBrowser("http://about.me/brianhsu") }
    githubButton.setOnClickListener { view: View => startBrowser("http://github.com/brianhsu/MaidroidPlurk") }
    marketButton.setOnClickListener { view: View =>
      val appPackage = getApplicationContext.getPackageName
      val uri = s"market://details?id=${appPackage}"
      val intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri))

      if (getPackageManager.queryIntentActivities(intent, 0).size > 0) {
        startActivity(intent)
      } else {
        val storeURL = s"http://play.google.com/store/apps/details?id=$appPackage"
        val intent = new Intent(Intent.ACTION_VIEW, Uri.parse(storeURL))
        startActivity(intent)
      }
    }

    aboutVersion
  }

  private def createAboutLibraryLicensePage = {
    val aboutLicense = getLayoutInflater.inflate(R.layout.about_library_licenses, null)
    val licenseTextView = aboutLicense.findView(TR.aboutLibraryLicenseTextView)
    val htmlContent = """
      | <div><big>Notice for <a href="https://github.com/chrisbanes/ActionBar-PullToRefresh">ActionBar-PullToRefresh</a>:</big></div>
      |
      |   <p>Copyright 2013 Chris Banes</p>
      |   
      |   <p>
      |   Licensed under the Apache License, Version 2.0 (the "License");
      |   you may not use this file except in compliance with the License.
      |   You may obtain a copy of the License at </p>
      |   
      |      <p>http://www.apache.org/licenses/LICENSE-2.0</p>
      |   <p>
      |   Unless required by applicable law or agreed to in writing, software
      |   distributed under the License is distributed on an "AS IS" BASIS,
      |   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
      |   See the License for the specific language governing permissions and
      |   limitations under the License.
      |   </p>
      |
      | <div> -------- </div>
      |
      | <div><big>Notice for <a href="http://viewpagerindicator.com/">ViewPagerIndicator</a>:</big></div>
      |
      |<p>Copyright 2012 Jake Wharton<br/>
      |Copyright 2011 Patrik Åkerfeldt<br/>
      |Copyright 2011 Francisco Figueiredo Jr.<br/>
      |<p>
      |Licensed under the Apache License, Version 2.0 (the "License");
      |you may not use this file except in compliance with the License.
      |You may obtain a copy of the License at</p>
      |
      |<p>   http://www.apache.org/licenses/LICENSE-2.0</p>
      |<p>
      |Unless required by applicable law or agreed to in writing, software
      |distributed under the License is distributed on an "AS IS" BASIS,
      |WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
      |See the License for the specific language governing permissions and
      |limitations under the License.</p>
      |
      | <div> -------- </div>
      |
      | <div><big>Notice for <a href="https://github.com/castorflex/SmoothProgressBar">SmoothProgressBar</a>:</big></div>
      |
      |<p>Copyright 2014 Antoine Merle</p>
      |<p>
      |Licensed under the Apache License, Version 2.0 (the "License");
      |you may not use this file except in compliance with the License.
      |You may obtain a copy of the License at</p>
      |
      |<p>   http://www.apache.org/licenses/LICENSE-2.0</p>
      |<p>
      |Unless required by applicable law or agreed to in writing, software
      |distributed under the License is distributed on an "AS IS" BASIS,
      |WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
      |See the License for the specific language governing permissions and
      |limitations under the License.</p>
      |
      | <div> -------- </div>
      |
      | <div><big>Notice for <a href="https://github.com/fernandezpablo85/scribe-java/">Scribe Java</a>:</big></div>
      |
      | <p>The MIT License</p>
      | 
      | <p>Copyright (c) 2010 Pablo Fernandez</p>
      | <p>
      | Permission is hereby granted, free of charge, to any person obtaining a copy
      | of this software and associated documentation files (the "Software"), to deal
      | in the Software without restriction, including without limitation the rights
      | to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
      | copies of the Software, and to permit persons to whom the Software is
      | furnished to do so, subject to the following conditions:
      | </p><p>
      | The above copyright notice and this permission notice shall be included in
      | all copies or substantial portions of the Software.
      | </p><p>
      | THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
      | IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
      | FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
      | AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
      | LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
      | OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
      | THE SOFTWARE.</p>
    """.stripMargin
    licenseTextView.setText(Html.fromHtml(htmlContent))
    licenseTextView.setMovementMethod(new LinkMovementMethod()) 
    aboutLicense
  }

  private def createAboutIconLicensePage = {
    val aboutIconLicense = getLayoutInflater.inflate(R.layout.about_icon_licenses, null)
    val holder = aboutIconLicense.findView(TR.aboutIconLicenseHolder)

    val freePickLicense = """Icon made by <a href="http://www.freepik.com">Freepik</a> from <a href="http://www.flaticon.com/free-icon/smiling-emoticon-square-face_42877">www.flaticon.com</a>"""
    holder.addView(createIconLicense(R.drawable.mute, freePickLicense))
    holder.addView(createIconLicense(R.drawable.ic_btn_aboutme, freePickLicense))
    holder.addView(createIconLicense(R.drawable.ic_btn_market, freePickLicense))
    holder.addView(createIconLicense(R.drawable.ic_btn_more, freePickLicense))
    holder.addView(createIconLicense(R.drawable.ic_btn_unviewable, freePickLicense))
    holder.addView(createIconLicense(R.drawable.ic_message_broken, freePickLicense))
    holder.addView(createIconLicense(R.drawable.ic_action_emoticon, freePickLicense))

    val iconMoonLicense = """Icon made by <a href="http://www.flaticon.com/free-icon/github-character-silhouette_23957">Icomoon</a> from <a href="http://www.flaticon.com">www.flaticon.com</a>"""
    holder.addView(createIconLicense(R.drawable.ic_btn_github, iconMoonLicense))
    holder.addView(createIconLicense(R.drawable.ic_btn_viewable, iconMoonLicense))
    holder.addView(createIconLicense(R.drawable.like, iconMoonLicense))

    val simpleIconLicense = """Icon made by <a href="http://www.flaticon.com/free-icon/images_33502">SimpleIcon</a> from <a href="http://www.flaticon.com">www.flaticon.com</a>"""

    holder.addView(createIconLicense(R.drawable.ic_action_photo, simpleIconLicense))

    aboutIconLicense
  }

  private def createIconLicense(drawableID: Int, licenseText: String) = {
    val licenseRow = getLayoutInflater.inflate(R.layout.item_icon_license, null)
    val iconView = licenseRow.findView(TR.itemIconLicenseIcon)
    val textView = licenseRow.findView(TR.itemIconLicenseText)
    iconView.setImageResource(drawableID)
    textView.setText(Html.fromHtml(licenseText))
    textView.setMovementMethod(new LinkMovementMethod()) 
    licenseRow
  }

  private lazy val pageAdapter = {

    val pages = Vector(createAboutVersionPage, createAboutLibraryLicensePage, createAboutIconLicensePage)
    new AboutPageAdapter(pages)
  }

  override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)

    setContentView(R.layout.activity_about)

    viewPager.setAdapter(pageAdapter)
    pagerIndicator.setViewPager(viewPager)
  }
}
