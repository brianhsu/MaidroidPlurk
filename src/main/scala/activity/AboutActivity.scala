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

import android.support.v7.app.ActionBarActivity

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

  private def createAboutVersionPage() = {
    val aboutVersion = getLayoutInflater.inflate(R.layout.about_version, null)
    val authorButton = aboutVersion.findView(TR.aboutVersionAuthor)
    val githubButton = aboutVersion.findView(TR.aboutVersionGithub)
    val marketButton = aboutVersion.findView(TR.aboutVersionMarket)

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

  private lazy val pageAdapter = {
    import android.widget.TextView

    val view2 = new TextView(this)
    val view3 = new TextView(this)
    view2.setText("B")
    view3.setText("C")
    new AboutPageAdapter(Vector(createAboutVersionPage, view2, view3))
  }

  override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)

    setContentView(R.layout.activity_about)

    viewPager.setAdapter(pageAdapter)
    pagerIndicator.setViewPager(viewPager)
  }
}
