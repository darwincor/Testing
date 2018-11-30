package com.inglesdivino.simplemusicplayer

import android.media.MediaPlayer
import java.lang.Exception
import java.util.*
import kotlin.concurrent.schedule

class Player {
    val mp: MediaPlayer = MediaPlayer()
    var prepared = false
    var lastAudioPath = ""

    //Listeners
    var onError: ((Int) -> Unit)? = null
    var mOnPlayingProgress: ((Int, Int, Int) -> Unit)? = null   //Progress in percentage, current position (millis), duration (millis)

    private val maxPoints = 200f   //E.g. points of the SeekerView

    fun setAudio(path: String) {
        if (!lastAudioPath.equals(path)) {  //Set audio only if it's the path is different
            mp.reset()
            mp.setDataSource(path)
            try {
                mp.prepare()
                prepared = true
                lastAudioPath = path
            } catch (e: Exception) {
                prepared = false
                lastAudioPath = ""
                onError?.invoke(0)
            }
        }
    }

    fun play() {
        if(prepared)
            mp.start()

        //Start getting current time
        startGettingCurrentTime()
    }

    fun pause() {
        if(prepared)
            mp.pause()
    }

    fun stop() {
        if(prepared)
            mp.stop()
    }

    fun seekTo(progress: Int) { //Progress goes from 0 to maxPoints
        val milliseconds: Float = mp.duration*progress/maxPoints
        mp.seekTo(milliseconds.toInt())
    }

    fun isPlaying(): Boolean {
        return mp.isPlaying
    }

    //Functions to update the seeker time
    private fun startGettingCurrentTime() {
        this.mOnPlayingProgress?.invoke(((mp.currentPosition*maxPoints/mp.duration).toInt()), mp.currentPosition, mp.duration)
        if (mp.isPlaying) {
            Timer("PlayingProgress", false).schedule(300) {
                startGettingCurrentTime()
            }
        }
    }

    fun getDuration(): Int {
        return mp.duration
    }

    fun release() {
        mp.release()
    }

    //Listeners
    fun setOnErrorListener(onError: (Int) -> Unit){
        this.onError = onError
    }

    fun setOnCompletionListener(onCompletion: (Unit) -> Unit) {
        //this.onCompletion = onCompletion
        mp.setOnCompletionListener {
            onCompletion.invoke(Unit)
        }
    }

    fun setOnPlayingProgress(onPlayingProgress: (Int, Int, Int) -> Unit) {
        this.mOnPlayingProgress = onPlayingProgress
    }
}