package com.ns.greg.library.fancyscaler.internal

import com.ns.greg.library.fancyscaler.internal.FancyFactor.ScaleFactor
import com.ns.greg.library.fancyscaler.internal.FancyFactor.TransFactor

/**
 * @author gregho
 * @since 2018/8/28
 */
internal class FancySize(
  width: Int,
  height: Int,
  private val widthFitFrame: Boolean,
  private val heightFitFrame: Boolean
) : FrameSize(width, height) {

  lateinit var fancyFactorX: FancyFactor
  lateinit var fancyFactorY: FancyFactor

  fun applyFrame(size: FrameSize) {
    /* defined fancy factor x */
    with(size.width) {
      val scaleFactorX = ScaleFactor(width, this, widthFitFrame)
      fancyFactorX = FancyFactor(scaleFactorX, TransFactor(this, scaleFactorX))
    }
    /* defined fancy factor y */
    with(size.height) {
      val scaleFactorY = ScaleFactor(height, this, heightFitFrame)
      fancyFactorY = FancyFactor(scaleFactorY, TransFactor(this, scaleFactorY))
    }
  }
}