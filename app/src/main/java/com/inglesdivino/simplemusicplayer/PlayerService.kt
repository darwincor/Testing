package com.inglesdivino.simplemusicplayer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import android.os.SystemClock.elapsedRealtime
import android.app.AlarmManager.ELAPSED_REALTIME
import android.app.AlarmManager
import android.util.Log


public class PlayerService : Service() {

    companion object {

        //Generic variables
        public val PROGRESS = "progress"
        public val DURATION = "duration"
        public val CURRENT_TIME = "current_time"
        public val PLAYLIST = "playlist"

        //Player status
        public val STATUS = "status"
        public val STATUS_ERROR = "status_error"
        public val STATUS_COMPLETED = "status_completed"
        public val STATUS_PROGRESS = "status_progress"
        public val INTENT_FROM_SERVICE = "intentFromService"

        //Player commands
        public val PLAYER_CMD = "player_cmd"
        public val CMD_START_PLAYLIST = "cmd_start_playlist"
        public val CMD_PLAY = "cmd_play"
        public val CMD_PAUSE = "cmd_pause"
        public val CMD_STOP = "cmd_stop"
        public val CMD_NEXT = "cmd_next"
        public val CMD_PREV = "cmd_prev"
    }

    //Audios path
    private var path: String = ""

    //Songs player
    val player = Player()
    var currentPlaylist: List<Song>? = null
    var startPosition: Int = 0  //Position where the playing of the playlist starts
    var currentPosition: Int = 0 //Current position being played in the playlist

    private val CHANNEL_ID = "playerChannel"
    private var notificationManager: NotificationManagerCompat? = null
    private val NOTIFICATION_ID = 10

    private var restartOnce= true

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        val restarting = intent?.getBooleanExtra("restarting", false)?:false   //Indicates if the service is being restarted

        if (restarting) {   //If service is being restarted
            Log.i("Darwincio_serv", "Player is being restarted")
            val playlist = intent?.getSerializableExtra("playlist") as ArrayList<*>
            val playlistArr = ArrayList<Song>()
            playlist.forEach { playlistArr.add((it as Song)) }
            startPlayList(playlistArr, 0)
            restartOnce = false
        } else {
            Log.i("Darwincio_serv", "Player is not being restarted")
        }

        val cmd = intent?.getStringExtra(PLAYER_CMD) ?: ""
        when (cmd) {
            CMD_START_PLAYLIST -> { //Start playlist
                val playlist = intent?.getSerializableExtra("playlist") as ArrayList<*>

                val playlistArr = ArrayList<Song>()
                playlist.forEach {playlistArr.add((it as Song))}
                startPlayList(playlistArr, 0)
            }
            CMD_PLAY -> {        //Play audio
                play()
            }
            CMD_PAUSE -> {       //Pause audio
                pause()
            }
            CMD_NEXT -> {        //Play next audio
                playNextSong()
            }
            CMD_PREV -> {        //Play previous audio
                playPreviousSong()
            }
            CMD_STOP -> {        //Stop audio

            }
        }

        val notiTitle = "Darwincio"
        val notiContent = "Darwino dj en la pista :)"

        //Create the channel for API >=26
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {

            val mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // The user-visible name of the channel.
            val name = "Player"
            // The user-visible description of the channel.
            val description = "Simple music player"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val mChannel = NotificationChannel(CHANNEL_ID, name, importance)
            // Configure the notification channel.
            mChannel.description = description
            mNotificationManager.createNotificationChannel(mChannel)
        }

        //Top open an activity when we tap on the notification
        val notificationIntent = Intent(this, MainActivity::class.java)
        notificationIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        notificationManager = NotificationManagerCompat.from(this)
        //notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        val mBuilder = NotificationCompat.Builder(this, CHANNEL_ID)

        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
            mBuilder.setContentTitle(notiTitle)
                .setContentText(notiContent)
                .setColor(resources.getColor(R.color.colorPrimary))
                .setSmallIcon(R.drawable.ic_play_24dp)
                .setOngoing(true)
                .setContentIntent(pendingIntent).priority = NotificationCompat.PRIORITY_LOW
        } else {
            mBuilder.setContentTitle(notiTitle)
                .setContentText(notiContent)
                .setColor(resources.getColor(R.color.colorPrimary))
                .setSmallIcon(R.drawable.ic_play_24dp)
                .setOngoing(true)
                .setContentIntent(pendingIntent).priority = NotificationCompat.PRIORITY_LOW
        }

        val number = 0
        val PROGRESS_MAX = 100
        mBuilder.setProgress(PROGRESS_MAX, number, true)
        val notification = mBuilder.build()
        if (Build.VERSION.SDK_INT >= 26) {
            startForeground(1, notification)
        } else {
            notificationManager?.notify(NOTIFICATION_ID, notification)
        }

        //return super.onStartCommand(intent, flags, startId)
        return Service.START_NOT_STICKY
    }

    override fun onBind(p0: Intent?): IBinder? {
        //("not implemented") //To change body of created functions use File | Settings | File Templates.
        //Listen to errors on the player
        player.setOnErrorListener {
            /*Toast.makeText(this, R.string.cannnot_play_audio, Toast.LENGTH_SHORT).show()
            play?.setImageResource(R.drawable.ic_play_24dp)*/
            val bundle = Bundle()
            bundle.putString(STATUS, STATUS_ERROR)
            broadcastMessage(bundle)
            //todo play the next song if an error is found
        }

        //Set on completion listener
        player.setOnCompletionListener {
            if (!isNextSongTheStart()) {
                playNextSong()
            } else {
                currentPosition++
            }
        }

        //Set on player progress
        player.setOnPlayingProgress { progress, currentTime, duration ->
            val bundle = Bundle()
            bundle.putString(STATUS, STATUS_PROGRESS)
            bundle.putInt(PROGRESS, progress)
            bundle.putInt(CURRENT_TIME, currentTime)
            bundle.putInt(DURATION, duration)
            broadcastMessage(bundle)
        }
        return null
    }

    //It starts playing a playlist from a given position
    fun startPlayList(playlist: List<Song>, pos: Int) {
        currentPlaylist = playlist
        startPosition = pos
        currentPosition = pos
        val songToPlay = playlist[pos]
        playSong(songToPlay)
    }

    //Returns true if the next song is the song where the playlist started
    private fun isNextSongTheStart(): Boolean {
        if (currentPlaylist != null) {
            var curPos = currentPosition + 1
            if (curPos > currentPlaylist?.size!! - 1) {
            }
            curPos = 0

            return curPos == startPosition
        } else {
        }
        return false
    }

    //Plays a song
    private fun playSong(song: Song) {
        player.setAudio(song.path)
        player.play()
    }

    //Plays the next song
    private fun playNextSong() {
        if(player.isPlaying())
            player.stop()

        if (currentPlaylist != null && currentPlaylist?.size!! > 0) {
            currentPosition++
            if (currentPosition > currentPlaylist?.size!! - 1)
                currentPosition = 0

            playSong(currentPlaylist!![currentPosition])
        }
    }

    //Plays the previous song
    private fun playPreviousSong() {
        if(player.isPlaying())
            player.stop()

        if (currentPlaylist != null && currentPlaylist?.size!! > 0) {
            currentPosition--
            if (currentPosition < 0)
                currentPosition = currentPlaylist?.size!!-1

            playSong(currentPlaylist!![currentPosition])
        }
    }

    //Basic player functions
    fun play() {
        player.play()
    }

    fun pause() {
        player.pause()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.i("Darwincio_serv", "On task removed!")
        if (restartOnce) {  //todo remove this condition
            Log.i("Darwincio_serv", "On task removed! (if(restartOnce))")
            val restartServiceIntent = Intent(applicationContext, this.javaClass)
            //Set parameters for the restarted service
            restartServiceIntent.putExtra("restarting", true)
            restartServiceIntent.putExtra("playlist", ArrayList(currentPlaylist))
            restartServiceIntent.putExtra("playlistPosition", currentPosition)

            val restartServicePendingIntent = PendingIntent.getService(
                applicationContext, 1, restartServiceIntent, PendingIntent.FLAG_ONE_SHOT
            )
            val alarmService = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmService.set(
                ELAPSED_REALTIME, elapsedRealtime() + 1000,
                restartServicePendingIntent
            )
            super.onTaskRemoved(rootIntent)
        }
    }

    //todo delete all songs from a folder when this folder is deleted
    //Broadcast a message
    private fun broadcastMessage(bundle: Bundle?) {
        val intent = Intent(INTENT_FROM_SERVICE)
        // You can also include some extra data.
        intent.putExtra("bundle", bundle)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }
}