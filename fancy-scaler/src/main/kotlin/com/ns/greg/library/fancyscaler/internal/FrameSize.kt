package com.ns.greg.library.fancyscaler.internal

/**
 * @author gregho
 * @since 2018/8/27
 */
internal open class FrameSize(
  val width: Int,
  val height: Int
) {

  override fun toString(): String {
    return "width: $width, height: $height"
  }
}