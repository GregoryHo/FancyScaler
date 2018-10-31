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

    const val DEFAULT_SCROLL_FACTOR = 1.5f
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
  private var scaleOnce = false

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
    v: View,
    event: MotionEvent
  ): Boolean {
    var processing = false
    if (::frameSize.isInitialized && ::sourceSize.isInitialized) {
      /* reset scale once to false when action down */
      if (event.actionMasked == MotionEvent.ACTION_DOWN) {
        scaleOnce = false
      }
      /* let scale gesture detector process first */
      processing = scaleGestureDetector.onTouchEvent(event)
      /* pass to the normal gesture only when not scale once */
      if (!scaleOnce) {
        /* check if the scale gesture detector is process or not */
        scaleOnce = scaleGestureDetector.isInProgress
        if (!scaleOnce) {
          processing = gestureDetector.onTouchEvent(event) or processing
        }
      }
    }

    return processing or v.onTouchEvent(event)
  }

  /*--------------------------------
   * Public functions
   *-------------------------------*/

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

  /*--------------------------------
   * Private functions
   *-------------------------------*/

  private fun calculate() {
    if (::frameSize.isInitialized && ::sourceSize.isInitialized) {
      sourceSize.applyFrame(frameSize)
      /* x */
      with(sourceSize.fancyFactorX) {
        transFactor.updateMinTrans()
        transFactor.applyCentralTrans()
      }
      /* y */
      with(sourceSize.fancyFactorY) {
        transFactor.updateMinTrans()
        transFactor.applyCentralTrans()
      }

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
          /* processing x */
          with(sourceSize.fancyFactorX) {
            /* scale x with new scale */
            scaleFactor.applyScale(scaleX * ratioX)
            /* sync with new scale */
            transFactor.updateMinTrans()
            /* translate x to focus point */
            if (transFactor.isExceedFit()) {
              transFactor.applyFocusTrans(focusX)
            } else {
              transFactor.applyCentralTrans()
            }
          }
          /* processing y */
          with(sourceSize.fancyFactorY) {
            /* scale y with new scale*/
            sourceSize.fancyFactorY.scaleFactor.applyScale(scaleY * ratioY)
            /* sync with new scale */
            transFactor.updateMinTrans()
            /* translate y to focus point */
            if (transFactor.isExceedFit()) {
              transFactor.applyFocusTrans(focusY)
            } else {
              transFactor.applyCentralTrans()
            }
          }

          updateMatrix()
        }
      }

      return true
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
      e?.run {
        doubleTap(e)
      }

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
          val exceedX = fancyFactorX.transFactor.isExceedFit()
          /* drag x */
          if (exceedX) {
            val transX = matrixValues[Matrix.MTRANS_X] - distanceX
            sourceSize.fancyFactorX.transFactor.applyTrans(transX)
          }
          /* drag y */
          val exceedY = fancyFactorY.transFactor.isExceedFit()
          if (exceedY) {
            val transY = matrixValues[Matrix.MTRANS_Y] - distanceY
            sourceSize.fancyFactorY.transFactor.applyTrans(transY)
          }

          dragging = exceedX or exceedY
        }

        if (dragging) {
          updateMatrix()
        }
      }
    }

    fun doubleTap(touchPoint: MotionEvent) {
      /* processing x */
      with(scaler.sourceSize.fancyFactorX) {
        /* scale x to min or max */
        with(scaleFactor) {
          if (current < max) {
            applyMaxScale()
          } else {
            applyMinScale()
          }
        }
        /* translate x to touch point */
        with(transFactor) {
          updateMinTrans()
          applyFocusTrans(touchPoint.x)
        }
      }
      /* processing y */
      with(scaler.sourceSize.fancyFactorY) {
        /* scale y to min or max */
        with(scaleFactor) {
          if (current < max) {
            applyMaxScale()
          } else {
            applyMinScale()
          }
        }
        /* translate y to touch point */
        with(transFactor) {
          updateMinTrans()
          applyFocusTrans(touchPoint.y)
        }
      }

      scaler.updateMatrix()
    }
  }
}