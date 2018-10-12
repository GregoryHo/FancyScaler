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

    private val fit = dst.toFloat() / src.toFloat()
    private val min = if (matchParent) fit else 1f
    private val max = fit * 4
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

    fun getCurrentValue(): Float {
      return src * current
    }

    fun getFitValue(): Float {
      return src * fit
    }

    fun getMaxValue(): Float {
      return src * max
    }
  }

  data class TransFactor(
    val src: Int,
    private val scaleFactor: ScaleFactor
  ) {

    /**
     * Right side edge which affected by [ScaleFactor.getCurrentValue],
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
      return "src: $src, dst: ${scaleFactor.getCurrentValue()}"
    }

    fun updateMinTrans() {
      min = src - scaleFactor.getCurrentValue()
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
      return scaleFactor.getCurrentValue() > scaleFactor.getFitValue()
    }

    fun transMask(trans: Float): Float {
      return when {
        trans <= minMask -> trans
        trans >= maxMask -> trans * -1
        else -> 0f
      }
    }
  }
}