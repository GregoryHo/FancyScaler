package com.ns.greg.fancyscaler

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.ImageView
import com.ns.greg.library.fancyscaler.FancyScaler

/**
 * @author gregho
 * @since 2018/8/31
 */
class DemoActivity : AppCompatActivity() {

  private lateinit var displayView: ImageView
  private lateinit var source: Bitmap

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    /* define display view */
    displayView = findViewById(R.id.display_iv)
    /* decode source as bitmap */
    source = BitmapFactory.decodeResource(resources, R.drawable.ic_android)
    displayView.setImageBitmap(source)
    val scaler = FancyScaler(displayView)
    scaler.scaleAs(source.width, source.height)
  }
}