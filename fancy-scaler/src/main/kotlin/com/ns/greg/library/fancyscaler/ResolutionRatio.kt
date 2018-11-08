package com.ns.greg.library.fancyscaler

/**
 * @author gregho
 * @since 2018/11/8
 */
enum class ResolutionRatio(
  private val widthRatio: Float,
  private val heightRatio: Float
) {

  RATIO_MATCH_PARENT(-1f, -1f),
  RATIO_ONE_X_ONE(1f, 1f),
  RATIO_FOUR_X_THREE(4f, 3f),
  RATIO_SIXTEEN_X_NINE(16f, 9f);

  override fun toString(): String {
    return when (this) {
      RATIO_MATCH_PARENT -> "ratio_match"
      RATIO_ONE_X_ONE -> "ratio_1:1"
      RATIO_FOUR_X_THREE -> "ratio_4:3"
      RATIO_SIXTEEN_X_NINE -> "ratio_16:9"
    }
  }

  fun getHeight(
    width: Float
  ): Int {
    return (width / widthRatio * heightRatio).toInt()
  }
}