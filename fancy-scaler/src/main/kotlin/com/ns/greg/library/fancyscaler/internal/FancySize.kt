package com.ns.greg.library.fancyscaler.internal

import com.ns.greg.library.fancyscaler.ResolutionRatio
import com.ns.greg.library.fancyscaler.ResolutionRatio.RATIO_FOUR_X_THREE
import com.ns.greg.library.fancyscaler.ResolutionRatio.RATIO_MATCH_PARENT
import com.ns.greg.library.fancyscaler.ResolutionRatio.RATIO_ONE_X_ONE
import com.ns.greg.library.fancyscaler.ResolutionRatio.RATIO_SIXTEEN_X_NINE
import com.ns.greg.library.fancyscaler.internal.FancyFactor.ScaleFactor
import com.ns.greg.library.fancyscaler.internal.FancyFactor.TransFactor

/**
 * @author gregho
 * @since 2018/8/28
 */
internal class FancySize(
  srcWidth: Int,
  srcHeight: Int,
  private val ratio: ResolutionRatio?

) : FrameSize(srcWidth, srcHeight) {

  lateinit var fancyFactorX: FancyFactor
  lateinit var fancyFactorY: FancyFactor

  fun applyFrame(
    size: FrameSize
  ) {
    val ratioSize = getRatioSize(size, ratio)
    /* defined fancy factor x */
    val scaleFactorX = ScaleFactor(width, ratioSize.width, size.width)
    fancyFactorX = FancyFactor(scaleFactorX, TransFactor(size.width, scaleFactorX))
    /* defined fancy factor y */
    val scaleFactorY = ScaleFactor(height, ratioSize.height, size.height)
    fancyFactorY = FancyFactor(scaleFactorY, TransFactor(size.height, scaleFactorY))
  }

  private fun getRatioSize(
    size: FrameSize,
    ratio: ResolutionRatio?
  ): FrameSize {
    return ratio?.run {
      when (this) {
        RATIO_MATCH_PARENT -> {
          size
        }
        RATIO_ONE_X_ONE -> {
          val min = Math.min(size.width, size.height)
          FrameSize(min, min)
        }
        RATIO_FOUR_X_THREE, RATIO_SIXTEEN_X_NINE -> {
          FrameSize(size.width, getHeight(size.width.toFloat()))
        }
      }
    } ?: run {
      this
    }
  }
}