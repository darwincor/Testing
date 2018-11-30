package com.inglesdivino.customViews

import android.content.Context
import android.view.View

import android.util.AttributeSet
import android.util.Log
import com.inglesdivino.simplemusicplayer.R
import com.inglesdivino.simplemusicplayer.sp
import android.animation.ValueAnimator
import android.graphics.*
import android.view.animation.LinearInterpolator

class CarouselTextView : View {

    //Carousel text
    var text: String = ""
    private var textColor: Int = Color.WHITE
    private val paint: Paint = Paint()
    private val textBounds = Rect()
    private var animate = true


    //Animator
    var animator = ValueAnimator.ofFloat(0f, 100f)
    var globalX = 0f
    var animationStarted = false
    var resetAnimation = true
    var offset = 0
    private var speed = 0.1f //Speed of the carousel animation

    var mAttrs: AttributeSet? = null

    constructor(context: Context): super(context, null){
        //init(context)
    }
    constructor(context: Context, attrs: AttributeSet?): super(context, attrs, 0){
        //Set Ppint properties
        paint.isAntiAlias = true
        paint.textSize = 18.sp
        paint.color = Color.WHITE

        mAttrs = attrs
        setXmlAttributes()
    }
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)    //Implement this constructor if needed

    private fun setXmlAttributes() {
        /*context.theme.obtainStyledAttributes(mAttrs, R.styleable.CarouselTextView, 0, 0).apply {
            try {
                //Fixme It doesn't work, fix it in the future. But at the moment it's valid, because we set color and text from code. But with xml it doesn't work
                text = getString(R.styleable.CarouselTextView_carouselText)
                textColor = getInt(R.styleable.CarouselTextView_carouselTextColor, Color.GREEN)
                if(text == null)
                    text = ""
            } finally {
                recycle()
            }
        }*/

        //Release the animator previously loaded
        animator.end()
        animator.removeAllUpdateListeners()

        //Add listener to the global animator
        animator.repeatCount = ValueAnimator.INFINITE
        animator.interpolator = LinearInterpolator()

        animator.addUpdateListener {
            if (animate) {
                globalX = it.animatedValue as Float
                invalidate()
                requestLayout()
            }
        }
    }

    override fun onDraw(canvas: Canvas?) {

        //Check if we should animate (if text width is bigger than its container)
        if (resetAnimation) {
            paint.getTextBounds(text, 0, text?.length?:0, textBounds)
            paint.color = textColor
            animator.setFloatValues(this.measuredWidth * 1f, 0f - textBounds.width())

            //Set the speed of the animation
            val t: Float = (this.measuredWidth + textBounds.width())*1f/speed
            animator.duration = t.toLong()
            animator.start()
            animationStarted = true
            /*if(paint.measureText(originalText) > this.measuredWidth){   //Animate

            }*/
            resetAnimation = false
        }
        // __________   _________
        //get half of the width and height as we are working with a circle
        val viewWidthHalf: Float = this.measuredWidth*1f / 2
        val viewHeightHalf: Float = this.measuredHeight*1f / 2

        if(animationStarted)
            canvas?.drawText(text, globalX, viewHeightHalf - textBounds.exactCenterY(), paint)
        else
            canvas?.drawText(text, viewWidthHalf - textBounds.exactCenterX(), viewHeightHalf - textBounds.exactCenterY(), paint)

    }

    //Setters and Getters
    fun getText_(): String? {
        return text
    }

    fun setTextColor(myColor: Int) {
        textColor = myColor
    }

    fun setCarouselText(localText: String) {
        text = "$localText   $localText   $localText   $localText"
        resetAnimation = true
        animationStarted = false
        invalidate()
        requestLayout()
    }

    fun setSpeed(localSpeed: Float) {
        speed = localSpeed
        resetAnimation = true
        animationStarted = false
        invalidate()
        requestLayout()
    }

    fun stopAnimation() {
        animate = false
    }

    fun startAnimation() {
        animate = true
    }
}