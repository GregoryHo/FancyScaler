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
    dst: Int,
    matchParent: Boolean
  ) {

    val fit = dst.toFloat() / src.toFloat()
    val min = if (matchParent) fit else 1f
    val max = fit * 4
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
    val src: Int,
    private val scaleFactor: ScaleFactor
  ) {

    /**
     * Right side edge which affected by [ScaleFactor.getCurrentFrame],
     * you can see [updateMinTrans].
     */
    var min = 0f
    /* current trans */
    var current = 0f
    /* left side edge always be zero */
    val max = 0f
    /* translate mask region */
    private val minMask = src / 3
    private val maxMask = src - minMask

    init {
      updateMinTrans()
      applyCentralTrans()
    }

    override fun toString(): String {
      return "src: $src, dst: ${scaleFactor.getCurrentFrame()}"
    }

    fun updateMinTrans() {
      min = src - scaleFactor.getCurrentFrame()
    }

    fun applyCentralTrans() {
      applyTrans(min / 2)
    }

    fun applyFocusTrans(focus: Float) {
      val ratio = focus / src
      applyTrans(min * ratio)
    }

    fun applyTrans(trans: Float) {
      current = when {
        trans <= min -> min
        trans >= max -> max
        else -> trans
      }
    }

    fun isExceedFit(): Boolean {
      return scaleFactor.getCurrentFrame() > scaleFactor.getFitFrame()
    }
  }
}