package com.ns.greg.library.fancyscaler.internal

/**
 * @author gregho
 * @since 2018/8/28
 */
internal class FancyFactor(
  val scaleFactor: ScaleFactor,
  val transFactor: TransFactor
) {

  class ScaleFactor(
    val src: Int,
    ratioSrc: Int,
    parent: Int
  ) {

    val fit = parent.toFloat() / src.toFloat()
    val min = ratioSrc / src.toFloat()
    val max = fit * 4f
    var current = min

    override fun toString(): String {
      return "min: $min, fit: $fit, max: $max, current: $current"
    }

    fun applyMinScale() {
      current = min
    }

    fun applyFitScale() {
      current = fit
    }

    fun applyMaxScale() {
      current = max
    }

    fun applyScale(scale: Float) {
      current = when {
        scale <= min -> min
        scale >= max -> max
        else -> scale
      }
    }

    fun getCurrentFrame(): Float {
      return src * current
    }

    fun getFitFrame(): Float {
      return src * fit
    }

    fun getMaxFrame(): Float {
      return src * max
    }
  }

  class TransFactor(
    val parent: Int,
    private val scaleFactor: ScaleFactor
  ) {

    /**
     * right/bottom side edge which affected by [ScaleFactor.getCurrentFrame],
     * you can see [updateMinTrans].
     */
    var min = 0f
    /* current trans */
    var current = 0f
    /* left/top side edge always be zero */
    val max = 0f

    init {
      updateMinTrans()
      applyCentralTrans()
    }

    override fun toString(): String {
      return "src: $parent, dst: ${scaleFactor.getCurrentFrame()}"
    }

    fun updateMinTrans() {
      min = parent - scaleFactor.getCurrentFrame()
    }

    fun applyCentralTrans() {
      applyTrans(min / 2)
    }

    fun applyFocusTrans(focus: Float) {
      val ratio = focus / parent
      applyTrans(min * ratio)
    }

    fun applyTrans(trans: Float) {
      /* means the src of scale factor less than src of trans factor frame */
      current = if (min > 0) {
        trans
      } else {
        when {
          trans <= min -> min
          trans >= max -> max
          else -> trans
        }
      }
    }

    fun isExceedFit(): Boolean {
      return scaleFactor.getCurrentFrame() > scaleFactor.getFitFrame()
    }
  }
}