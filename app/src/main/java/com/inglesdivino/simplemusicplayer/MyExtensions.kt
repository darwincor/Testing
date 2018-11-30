package com.inglesdivino.simplemusicplayer

import android.content.res.Resources
import android.text.Editable
import android.text.TextWatcher
import android.util.DisplayMetrics
import android.widget.EditText
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity

//Implementation of my extension
fun EditText.afterTextChanged(afterTextChanged: (String)->Unit) {
    this.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(p0: Editable?) {
            afterTextChanged(p0.toString())
        }
        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        }
        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        }
    })
}

//On seek listener
fun SeekBar.onSeekBarChangeListener(onSBChange: (Int) -> Unit) {
    this.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
        override fun onStartTrackingTouch(p0: SeekBar?) {}
        override fun onStopTrackingTouch(p0: SeekBar?) {}

        override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
            onSBChange(p1)
        }
    })
}

//Extension to convert dp to pixels and vice
val Int.dp: Int get() = (this / Resources.getSystem().displayMetrics.density).toInt()   //Call it this way 88.dp

val Int.px: Int get() = (this * Resources.getSystem().displayMetrics.density).toInt()   //Call it this way 88.px

//Convert sp to float
val Int.sp: Float get() = (this*Resources.getSystem().displayMetrics.scaledDensity*1f)

//Extension to get dimensions of the screen
//val AppCompatActivity.screenWidth: Int get() = ((this.windowManager.defaultDisplay.getMetrics(DisplayMetrics() as DisplayMetrics))
fun AppCompatActivity.screenWidth(): Int{
    val displayMetrics = DisplayMetrics()
    this.windowManager.defaultDisplay.getMetrics(displayMetrics)
    return displayMetrics.widthPixels
}
fun AppCompatActivity.screenHeight(): Int{
    val displayMetrics = DisplayMetrics()
    this.windowManager.defaultDisplay.getMetrics(displayMetrics)
    return displayMetrics.heightPixels
}

/*
val displayMetrics = DisplayMetrics()
            windowManager.defaultDisplay.getMetrics(displayMetrics)

            var width = displayMetrics.widthPixels
            var height = displayMetrics.heightPixels
* */

