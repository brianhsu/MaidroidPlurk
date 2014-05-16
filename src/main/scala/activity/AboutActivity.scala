package idv.brianhsu.maidroid.plurk.activity

import idv.brianhsu.maidroid.plurk._

import android.os.Bundle
import android.content.Intent
import android.content.Context

import android.support.v7.app.ActionBarActivity

object AboutActivity {
  def startActivity(context: Context) {
    val intent = new Intent(context, classOf[AboutActivity])
    context.startActivity(intent)
  }
}

class AboutActivity extends ActionBarActivity
{
  override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_about)
  }
}
