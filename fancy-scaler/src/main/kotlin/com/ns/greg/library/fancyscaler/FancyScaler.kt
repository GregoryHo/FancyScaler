package com.ns.greg.library.fancyscaler

import android.annotation.SuppressLint
import android.graphics.Matrix
import android.support.v4.view.GestureDetectorCompat
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener
import android.view.TextureView
import android.view.View
import android.view.View.OnLayoutChangeListener
import android.view.View.OnTouchListener
import android.widget.ImageView
import com.ns.greg.library.fancyscaler.internal.FancyFactor.TransFactor
import com.ns.greg.library.fancyscaler.internal.FancySize
import com.ns.greg.library.fancyscaler.internal.FrameSize
import java.lang.ref.WeakReference
import kotlin.LazyThreadSafetyMode.NONE

/**
 * @author gregho
 * @since 2018/8/27
 */
class FancyScaler(private val view: View) : OnLayoutChangeListener, OnTouchListener {

  companion object {

    const val DEFAULT_SCROLL_FACTOR = 2.5f
  }

  private val scaleGestureDetector: ScaleGestureDetector by lazy(NONE) {
    ScaleGestureDetector(
        view.context,
        SimpleScaleGestureListener(this)
    )
  }
  private val gestureDetector: GestureDetectorCompat by lazy(NONE) {
    GestureDetectorCompat(
        view.context,
        SimpleGestureListener(this)
    )
  }
  private val matrix: Matrix by lazy(NONE) {
    Matrix()
  }
  private val matrixValues = FloatArray(9)
  private lateinit var frameSize: FrameSize
  private lateinit var sourceSize: FancySize

  init {
    if (view !is TextureView && view !is ImageView) {
      throw IllegalArgumentException("Scaler only support TextureView or ImageView.")
    }

    enable()
  }

  override fun onLayoutChange(
    v: View?,
    left: Int,
    top: Int,
    right: Int,
    bottom: Int,
    oldLeft: Int,
    oldTop: Int,
    oldRight: Int,
    oldBottom: Int
  ) {
    frameSize = FrameSize(right - left, bottom - top)
    calculate()
  }

  @SuppressLint("ClickableViewAccessibility")
  override fun onTouch(
    v: View?,
    event: MotionEvent?
  ): Boolean {
    var processing = false
    return v?.run {
      if (::frameSize.isInitialized && ::sourceSize.isInitialized) {
        processing = scaleGestureDetector.onTouchEvent(event)
        /* pass to the normal gesture when not scaling */
        if (!scaleGestureDetector.isInProgress) {
          processing = gestureDetector.onTouchEvent(event) or processing
        }
      }

      processing || onTouchEvent(event)
    } ?: processing
  }

  fun enable() {
    view.addOnLayoutChangeListener(this)
    view.setOnTouchListener(this)
  }

  fun disalbe() {
    view.removeOnLayoutChangeListener(this)
    view.setOnTouchListener(null)
  }

  fun setSourceSize(
    width: Int,
    height: Int,
    widthFitFrame: Boolean = false,
    heightFitFrame: Boolean = false
  ) {
    sourceSize = FancySize(width, height, widthFitFrame, heightFitFrame)
    calculate()
  }

  private fun calculate() {
    if (::frameSize.isInitialized && ::sourceSize.isInitialized) {
      sourceSize.applyFrame(frameSize)
      updateMatrix()
    }
  }

  private fun updateMatrix() {
    with(sourceSize) {
      matrix.reset()
      matrix.postScale(fancyFactorX.scaleFactor.current, fancyFactorY.scaleFactor.current)
      matrix.postTranslate(
          fancyFactorX.transFactor.current, fancyFactorY.transFactor.current
      )
      /* apply to view */
      view.post {
        if (view is TextureView) {
          view.setTransform(matrix)
        } else if (view is ImageView) {
          view.imageMatrix = matrix
        }
      }
    }
  }

  /*--------------------------------
  * SCALE GESTURE LISTENER
  *-------------------------------*/

  private class SimpleScaleGestureListener(reference: FancyScaler) :
      SimpleOnScaleGestureListener() {

    private val scaler: FancyScaler by lazy(NONE) {
      WeakReference<FancyScaler>(reference).get()!!
    }
    private var beginFocusX = 0f
    private var beginFocusY = 0f

    override fun onScaleBegin(detector: ScaleGestureDetector?): Boolean {
      detector?.run {
        beginFocusX = focusX
        beginFocusY = focusY
      }

      return true
    }

    override fun onScale(detector: ScaleGestureDetector?): Boolean {
      detector?.run {
        val ratioX =
          (currentSpan + currentSpanX) / (previousSpan + previousSpanX)
        val ratioY =
          (currentSpan + currentSpanY) / (previousSpan + previousSpanY)
        with(scaler) {
          /* get current matrix values */
          matrix.getValues(matrixValues)
          /* get current scale x/y */
          val scaleX = matrixValues[Matrix.MSCALE_X]
          val scaleY = matrixValues[Matrix.MSCALE_Y]
          zoom(sourceSize, scaleX, ratioX, scaleY, ratioY)
          /* get new scale x/y after zoomed */
          val newScaleX = sourceSize.fancyFactorX.scaleFactor.current
          val newScaleY = sourceSize.fancyFactorY.scaleFactor.current
          /* get current translate x/y */
          val transX = matrixValues[Matrix.MTRANS_X]
          val transY = matrixValues[Matrix.MTRANS_Y]
          translate(sourceSize.fancyFactorX.transFactor, transX, beginFocusX, scaleX, newScaleX)
          translate(sourceSize.fancyFactorY.transFactor, transY, beginFocusY, scaleY, newScaleY)
          updateMatrix()
        }
      }

      return true
    }

    private fun zoom(
      sourceSize: FancySize,
      scaleX: Float,
      ratioX: Float,
      scaleY: Float,
      ratioY: Float
    ) {
      with(sourceSize) {
        /* zoom width */
        fancyFactorX.scaleFactor.applyScale(scaleX * ratioX)
        /* zoom height */
        fancyFactorY.scaleFactor.applyScale(scaleY * ratioY)
      }
    }

    private fun translate(
      transFactor: TransFactor,
      current: Float,
      focus: Float,
      scale: Float,
      newScale: Float
    ) {
      with(transFactor) {
        updateMinTrans()
        applyCentralTrans()
        /*if (isExceedFit()) {
          applyFocusTrans(focus)
        } else {
          applyCentralTrans()
        }*/
      }
    }
  }

  /*--------------------------------
 * BASIC GESTURE LISTENER
 *-------------------------------*/

  private class SimpleGestureListener(reference: FancyScaler) : SimpleOnGestureListener() {

    private val scaler: FancyScaler by lazy(NONE) {
      WeakReference<FancyScaler>(reference).get()!!
    }

    override fun onScroll(
      e1: MotionEvent?,
      e2: MotionEvent?,
      distanceX: Float,
      distanceY: Float
    ): Boolean {
      drag(distanceX * DEFAULT_SCROLL_FACTOR, distanceY * DEFAULT_SCROLL_FACTOR)
      return true
    }

    override fun onDoubleTap(e: MotionEvent?): Boolean {
      return true
    }

    fun drag(
      distanceX: Float,
      distanceY: Float
    ) {
      with(scaler) {
        matrix.getValues(matrixValues)
        var dragging = false
        with(sourceSize) {
          dragging = fancyFactorX.transFactor.isExceedFit()
          /* drag x */
          if (dragging) {
            val transX = matrixValues[Matrix.MTRANS_X] - distanceX
            sourceSize.fancyFactorX.transFactor.applyTrans(transX)
          }
          /* drag y */
          dragging = fancyFactorY.transFactor.isExceedFit()
          if (dragging) {
            val transY = matrixValues[Matrix.MTRANS_Y] - distanceY
            sourceSize.fancyFactorY.transFactor.applyTrans(transY)
          }
        }

        if (dragging) {
          updateMatrix()
        }
      }
    }

    fun doubleClick() {
      // TODO: should implement this
    }
  }
}