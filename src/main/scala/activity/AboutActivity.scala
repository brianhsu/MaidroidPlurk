package idv.brianhsu.maidroid.plurk.activity

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.ActionBarActivity
import android.text.Html
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.View
import android.widget.Button
import com.google.android.gms.ads._
import idv.brianhsu.maidroid.plurk._
import idv.brianhsu.maidroid.plurk.adapter._
import idv.brianhsu.maidroid.plurk.TypedResource._
import idv.brianhsu.maidroid.ui.util.CallbackConversions._
import scala.util.Try

object AboutActivity {
  def startActivity(context: Context) {
    val intent = new Intent(context, classOf[AboutActivity])
    context.startActivity(intent)
  }

  val AboutMeURL = "http://bone.twbbs.org.tw/"
  val GitHubURL  = "http://github.com/brianhsu/MaidroidPlurk"
}

class AboutActivity extends ActionBarActivity with TypedViewHolder
{

  import idv.brianhsu.maidroid.billing._

  private val TAG = "AboutActivity"
  private val RC_REQUEST = 10001

  private lazy val viewPager = findView(TR.activityAboutViewPager)
  private lazy val pagerIndicator = findView(TR.activityAboutPagerIndicator)
  private lazy val adView = findView(TR.adView)
  private lazy val mHelper = new IabHelper(this, "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAln/knFGoApRevyKwhU0It4xS+/cRogPnHJavJKaHrFvbAI0RD4dkrWXq7N+QieEGJf2oEPbXtC2nq8Z4aWlyRFIsI/GgqedLTyG4j6XsE/lN0F5pZN6q38dtg/5U7fa8gdd9Sy3FcU88KDgaNTLJQLnPDQwQeGG947XPlsls61gRQXJf7de298uqqggOvlbOILg2HL7+e2FgVNpAp2jveeeJ2+8pX6p/+/Xqe9lp1ShAMtjw684DpcObHDrXeQDtVGT7aviP6eRtWIm4neSm9d5sJWlFi7DWWR5YbIZxCBSYTkcEmT14UYXLBewv8/g63FgV9+cCTEjBARjZg8ymXwIDAQAB")

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

    val appNameWithVersion = getString(R.string.appName) + getVersionName.getOrElse("O.O.O")

    versionText.setText(appNameWithVersion)
    authorButton.setOnClickListener { view: View => startBrowser(AboutActivity.AboutMeURL) }
    githubButton.setOnClickListener { view: View => startBrowser(AboutActivity.GitHubURL) }
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

  private def createAboutDonationPage = {
    val donationPage = getLayoutInflater.inflate(R.layout.about_donation, null)
    val donateText = donationPage.findView(TR.aboutDonationText)
    val donate30Button = donationPage.findView(TR.aboutDonation30)
    val donate50Button = donationPage.findView(TR.aboutDonation50)
    val donate100Button = donationPage.findView(TR.aboutDonation100)

    donateText.setText(Html.fromHtml(getString(R.string.aboutDonationText)))

    def setButtonState(isEnable: Boolean) {
      donate30Button.setEnabled(isEnable)
      donate50Button.setEnabled(isEnable)
      donate100Button.setEnabled(isEnable)
    }

    val mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
      override def onIabPurchaseFinished(result:IabResult, purchase: Purchase) = {
        if (result.isFailure) {
          Log.e(TAG, "====> Error purchasing: " + result)
          if (result.getResponse != -1005) {
            showMessageDialog(
              getString(R.string.aboutDonationCancelTitle),
              getString(R.string.aboutDonationCancelContent).format(result.getMessage)
            )    
          }
          setButtonState(true)
        } else {
          Option(purchase).foreach { purchase => 
            showMessageDialog(
              getString(R.string.aboutDonationThankTitle),
              getString(R.string.aboutDonationThankContent).format(result)
            )    
            consumeDonation(purchase) 
            setButtonState(true)
          }
        }
      }
    }

    def setupDonationButton(button: Button, sku: String) = {
      button.setOnClickListener { view: View =>
        setButtonState(false)
        mHelper.launchPurchaseFlow(this, sku, RC_REQUEST, mPurchaseFinishedListener, "")
      }
    }

    setupDonationButton(donate30Button, "donate_30")
    setupDonationButton(donate50Button, "donate_50")
    setupDonationButton(donate100Button, "donate_100")

    donationPage
  }

  private def showMessageDialog(title: String, message: String): Unit = {

    val okListener = new DialogInterface.OnClickListener() {
      override def onClick(dialog: DialogInterface, which: Int) {}
    } 

    val alertDialog = new AlertDialog.Builder(this)

    alertDialog.setTitle(title)
               .setMessage(message)
               .setPositiveButton(R.string.ok, okListener)

    alertDialog.show();
  }

  override def onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
    // Pass on the activity result to the helper for handling
    if (!mHelper.handleActivityResult(requestCode, resultCode, data)) {
      super.onActivityResult(requestCode, resultCode, data);
    }
  }

  private def createAboutAppLicensePage = {
    val aboutLicense = getLayoutInflater.inflate(R.layout.about_library_licenses, null)
    val licenseTextView = aboutLicense.findView(TR.aboutLibraryLicenseTextView)
    val htmlContent = """
      | <h2>Copyright (C) 2014-2015 BrianHsu</h2>
      | <p>
      | This program is free software; you can redistribute it and/or
      | modify it under the terms of the GNU General Public License
      | as published by the Free Software Foundation; either version 2
      | of the License, or (at your option) any later version.
      | </p>
      | <p>
      | This program is distributed in the hope that it will be useful,
      | but WITHOUT ANY WARRANTY; without even the implied warranty of
      | MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
      | </p>
      | <p>
      | See the <a href="http://www.gnu.org/licenses/gpl.html">GNU General Public License</a> for more details.
      | </p>
    """.stripMargin
    licenseTextView.setText(Html.fromHtml(htmlContent))
    licenseTextView.setMovementMethod(new LinkMovementMethod()) 
    aboutLicense
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

    val pages = Vector(
      createAboutVersionPage, 
      createAboutDonationPage, 
      createAboutAppLicensePage, 
      createAboutLibraryLicensePage, 
      createAboutIconLicensePage
    )
    new AboutPageAdapter(getApplicationContext, pages)
  }

  private val noAction: () => Any = () => {}

  private def consumeDonation(donationPurchase: Purchase, callback: () => Any = noAction) {
    mHelper.consumeAsync(donationPurchase, new IabHelper.OnConsumeFinishedListener() {
      override def onConsumeFinished(purchase: Purchase, result: IabResult) {
        Log.d(TAG, "====> Consumption finished. Purchase: " + purchase + ", result: " + result)
      }
    })
  }

  override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)

    setContentView(R.layout.activity_about)
    viewPager.setAdapter(pageAdapter)
    pagerIndicator.setViewPager(viewPager)

    val adRequest = new AdRequest.Builder().addTestDevice("6A4C0B7C2BA5CCDB7258476B0F49F059").build()
    adView.loadAd(adRequest)

    mHelper.enableDebugLogging(true)
    mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
      override def onIabSetupFinished(result: IabResult) {
        Log.d(TAG, "====> Setup finished.");

        if (!result.isSuccess()) {
          // Oh noes, there was a problem.
          Log.e(TAG, "====> Problem setting up in-app billing: " + result);
          return;
        }

        // Hooray, IAB is fully set up. Now, let's get an inventory of stuff we own.
        Log.d(TAG, "====> Setup successful. Querying inventory.");
        mHelper.queryInventoryAsync(new IabHelper.QueryInventoryFinishedListener(){
          override def onQueryInventoryFinished(result: IabResult, inventory:Inventory) {
            if (result.isFailure()) {
              Log.e(TAG, "=====> Failed to query inventory: " + result);
            } else {
              Option(inventory.getPurchase("donate_30")).foreach(purchase => consumeDonation(purchase))
              Option(inventory.getPurchase("donate_50")).foreach(purchase => consumeDonation(purchase))
              Option(inventory.getPurchase("donate_100")).foreach(purchase => consumeDonation(purchase))
            }
          }
        })

      }
    })
  }

  override def onPause() {
    adView.pause()
    super.onPause()
  }

  override def onResume() {
    adView.resume()
    super.onResume()
  }

  override def onDestroy() {
    adView.destroy()
    super.onDestroy()
  }


}
