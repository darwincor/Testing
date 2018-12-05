package com.inglesdivino.simplemusicplayer

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.appcompat.app.AppCompatActivity;
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.MenuItemCompat
import androidx.lifecycle.ViewModelProviders

import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_player.*
import org.jetbrains.anko.longToast
import org.jetbrains.anko.searchView


class MainActivity : AppCompatActivity() {

    //Player View Model
    var mPlayerViewModel: PlayerViewModel? = null

    var mAudiosFragment: AudiosFragment? = null
    var mFoldersFragment: FoldersFragment? = null
    var mSongsFragment: SongsFragment? = null
    val STORAGE_REQUEST_CODE = 1

    var currentPlaylist: List<Song>? = null
    var startPosition: Int = 0  //Position where the playing of the playlist starts
    var currentPosition: Int = 0 //Current position being played in the playlist
    //Songs player
    val player = Player()

    //Variable to check if user is seeking manually
    var seekingManually = false

    //Filter search
    private var filter = ""
    private var searchMenu: Menu? = null
    var searchMenuItem: MenuItem? = null
    companion object {
        var searchViewIsVisible: Boolean = false
    }

    //Listeners
    var mOnQueryText: ((String) -> Unit)? = null

    //Timestamp when the animation start
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        //Set the observer of live data
        mPlayerViewModel = ViewModelProviders.of(this).get(PlayerViewModel::class.java)


        //Place the main fragment programmatically
        if (mAudiosFragment == null)
            mAudiosFragment = AudiosFragment()

        //Listen when show folders is tapped
        setOnShowFoldersListener()

        //Listen when play selected songs in the main list is tapped
        setOnPlaySelectedSongsListener()

        //Set listeners to the AudiosFragment
        mAudiosFragment?.setOnNewAudio {
            onPlayNewAudio(it)
        }

        //Listen to errors on the player
        player.setOnErrorListener {
            Toast.makeText(this, R.string.cannnot_play_audio, Toast.LENGTH_SHORT).show()
            play?.setImageResource(R.drawable.ic_play_24dp)
            //todo play the next song if an error is found
        }

        //Set on completion listener
        player.setOnCompletionListener {
            play?.setImageResource(R.drawable.ic_play_24dp)
            mAudiosFragment?.stopCarouselAnimation()
            if (!isNextSongTheStart()) {
                playNextSong()
            } else {
                currentPosition++
            }
        }

        //Set on player progress
        player.setOnPlayingProgress {progress, currentTime, duration ->
            if (!seekingManually) {
                player_progress?.progress = progress
                runOnUiThread {
                    song_duration?.text = getStringTime(duration)
                    song_curr_time?.text = getStringTime(currentTime)
                }
            }
        }

        if(isStoragePermissionGranted())
            showMainFragment()
        else
            requestStoragePermission()

        move_songs.setOnClickListener {
            mAudiosFragment?.showSelectFolderDialog()
        }
        play_sel_songs.setOnClickListener { mAudiosFragment?.playSelectedSongs() }
    }


    //Returns time in format string given an amount of milliseconds
    fun getStringTime(millisecons: Int): String {
        //todo make the conversion properly, it's bad
        val seconds = millisecons/1000
        val minutes = millisecons/(1000*60)
        return minutes.toString()+":"+seconds.toString()
    }

    //Returns true if the next song is the song where the playlist started
    private fun isNextSongTheStart(): Boolean {
        if (currentPlaylist != null) {
            var curPos = currentPosition+1
            if(curPos > currentPlaylist?.size!! -1)
                curPos = 0

            return curPos == startPosition
        }
        else
            return false
    }

    private fun playNextSong() {
        if (currentPlaylist != null && currentPlaylist?.size!! > 0) {
            currentPosition++
            if(currentPosition > currentPlaylist?.size!! -1)
                currentPosition = 0

            onPlayNewAudio(currentPlaylist!![currentPosition])
        }
    }

    private fun onPlayNewAudio(song: Song) {

        if(!playerIsVisible())
            showPlayer()
        //Configure the animation title parameters
        player_compressed.setCarouselText(song.name)
        main_song_title?.setCarouselText(song.name)
        player.setAudio(song.path)

        player.play()
        player_compressed.startAnimation()
        main_song_title.startAnimation()
        if(player.isPlaying())
            play?.setImageResource(R.drawable.ic_pause_24dp)
    }

    private fun showMainFragment() {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
            .replace(R.id.fragment_container, mAudiosFragment!!, "mAudiosFragment")
            .commit()
    }

    //Check if the storage permission is granted
    fun isStoragePermissionGranted(): Boolean {
        val stoPermission = ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        return stoPermission != PackageManager.PERMISSION_DENIED
    }

    fun requestStoragePermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
            STORAGE_REQUEST_CODE
        )
    }

    //Called when user wants to see the available folders
    private fun setOnShowFoldersListener() {
        mAudiosFragment?.setOnShowFolders {
            //Show the fragment with the folders
            if(mFoldersFragment == null)
                mFoldersFragment = FoldersFragment()

            supportFragmentManager.beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.fragment_container, mFoldersFragment as FoldersFragment, "mFoldersFragment")
                .commit()

            setOnShowSongsInFolderListener()
        }
    }

    //Called when user wants to play the available folders
    private fun setOnPlaySelectedSongsListener() {
        mAudiosFragment?.setOnPlaySelectedSongsListener {
            startPlayList(it, 0)
        }
    }

    //Called when user wants to see the songs inside a folder
    private fun setOnShowSongsInFolderListener() {
        //Listen to show songs in folder
        mFoldersFragment?.setOnShowSongsInFolder {
            if(mSongsFragment == null)
                mSongsFragment = SongsFragment()
            mSongsFragment?.setFolder(it)
            mSongsFragment?.setOnSelectSongs {songs, pos ->
                startPlayList(songs, pos)
            }

            supportFragmentManager.beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.fragment_container, mSongsFragment as SongsFragment, "mSongsFragment")
                .commit()
        }
    }

    fun startPlayList(songs: List<Song>, pos: Int) {
        currentPlaylist = songs
        startPosition = pos
        currentPosition = pos
        val songToPlay = songs[pos]
        onPlayNewAudio(songToPlay)
    }

    //Called when user wants to add new songs to an specific folder
    private fun setOnAddSongsToFolder() {
    }

    //Show the player Layout
    private fun showPlayer() {
        //player_container
        val view = layoutInflater.inflate(R.layout.content_player, null)
        player_container.removeAllViews()
        player_container.addView(view)
        expandPlayer()

        play.setOnClickListener {
            if (player.isPlaying()) {
                player.pause()
                (it as ImageView).setImageResource(R.drawable.ic_play_24dp)

                player_compressed.stopAnimation()
                main_song_title.stopAnimation()
            } else {
                player.play()
                if (player.isPlaying()) {
                    (it as ImageView).setImageResource(R.drawable.ic_pause_24dp)
                    player_compressed.startAnimation()
                    main_song_title.startAnimation()
                }

            }
        }

        player_compressed.setOnClickListener {
            expandPlayer()
        }

        //Listen to seeks from ui
        player_progress.onSeekBarChangeListener {
            player.seekTo(it)
        }

        player_progress.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onStartTrackingTouch(p0: SeekBar) {
                seekingManually = true
                my_toast.visibility = View.VISIBLE
            }
            override fun onStopTrackingTouch(p0: SeekBar) {
                player.seekTo(p0.progress)
                seekingManually = false
                my_toast.visibility = View.GONE
            }
            override fun onProgressChanged(p0: SeekBar, p1: Int, p2: Boolean) {
                runOnUiThread {
                    my_toast.text = getStringTime((player.getDuration()
                            *p1/200f).toInt()) //todo arreglar bien este número si fusee necesario
                    //my_toast.text = getStringTime(currentTime) //todo arreglar bien este número si fusee necesario
                }
            }
        })
    }

    //Hide the player layout
    fun hidePlayer() {
        collapsePlayer()
    }

    //Tells if the player layout is visible
    public fun playerIsVisible(): Boolean {
        return player_container.visibility == View.VISIBLE || lay_player_compressed.visibility == View.VISIBLE
    }

    fun playerIsExpanded(): Boolean {
        return player_container.visibility == View.VISIBLE
    }

    //Shows additional options in the action bar (when some mAudios are selected)
    private fun showSelectedSongsOptions() {
        extendedOptions.visibility = View.VISIBLE
        toolbar.visibility = View.GONE
    }

    private fun hideSelectedSongsOptions() {
        extendedOptions.visibility = View.GONE
        toolbar.visibility = View.VISIBLE
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        searchMenuItem = menu.findItem(R.id.action_search)

        //Search filter
        val mSearchView = MenuItemCompat.getActionView(searchMenuItem) as SearchView
        mSearchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(p0: String?): Boolean {
                return true
            }
            override fun onQueryTextChange(p0: String): Boolean {
                mOnQueryText?.invoke(p0)
                /*if (mAudiosFragment != null)
                    mAudiosFragment?.filterByString(p0)*/
                return true
            }
        })

        //Listen when searchView is collapsed or expanded
        searchMenuItem?.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(p0: MenuItem?): Boolean {
                searchViewIsVisible = true
                return true
            }

            override fun onMenuItemActionCollapse(p0: MenuItem?): Boolean {
                searchViewIsVisible = false
                mAudiosFragment?.onSearchViewCollapsed()
                return true
            }
        })

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            R.id.action_search -> {
                /*val searchView = (searchMenu?.findItem(R.id.action_search)?.actionView as SearchView)
                searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {

                })*/
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {

        if (mAudiosFragment?.isVisible == true) {
            if (mAudiosFragment!!.thereAreSelectedSongs())
                showSelectedSongsOptions()
            else
                hideSelectedSongsOptions()
        }
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onDestroy() {
        super.onDestroy()
        if(!player.isPlaying())
            player.release()
    }

    override fun onBackPressed() {
        if (playerIsExpanded()) {
            hidePlayer()
        } else if (mAudiosFragment?.isVisible == true) {
            if (mAudiosFragment?.thereAreSelectedSongs() == true) {
                mAudiosFragment?.clearSelectedSongs()
            } else {
                super.onBackPressed()
            }
        } else if (mFoldersFragment?.isVisible == true) {
            if (mAudiosFragment == null) {
                mAudiosFragment = AudiosFragment()
            }

            supportFragmentManager.beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.fragment_container, mAudiosFragment as AudiosFragment, "mAudiosFragment")
                .commit()

            //Set title
            setFragmentTitle(getString(R.string.app_name))
        } else if (mSongsFragment?.isVisible == true) {
            if (mFoldersFragment == null) {
                mFoldersFragment = FoldersFragment()
            }

            supportFragmentManager.beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.fragment_container, mFoldersFragment as FoldersFragment, "mFoldersFragment")
                .commit()

        } else {
            super.onBackPressed()
        }

    }

    public fun setFragmentTitle(title: String) {
        val actionBar = (this as AppCompatActivity).supportActionBar
        actionBar?.title = title
        this.invalidateOptionsMenu()
    }

    //Animations
    private fun expandPlayer() {
        lay_player_compressed.visibility = View.GONE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val cx: Int = this.screenWidth() - 60.px
            val cy = 20.px
            val finalRadius: Double = Math.hypot(cx.toDouble(), cy.toDouble())
            val anim: Animator = ViewAnimationUtils.createCircularReveal(player_container, cx, cy, 0f, finalRadius.toFloat())
            anim.duration = 300
            player_container.visibility = View.VISIBLE
            anim.start()
        }
        else
            player_container.visibility = View.VISIBLE

        player_compressed.setSpeed(0.1f)

    }

    private fun collapsePlayer() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val cx: Int = this.screenWidth() - 60.px
            val cy = 20.px
            val finalRadius: Double = Math.hypot(cx.toDouble(), cy.toDouble())
            val anim: Animator =
                ViewAnimationUtils.createCircularReveal(player_container, cx, cy, finalRadius.toFloat(), 0f)
            anim.duration = 300
            anim.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    player_container.visibility = View.INVISIBLE
                    lay_player_compressed.visibility = View.VISIBLE
                }
            })
            anim.start()
        } else {
            player_container.visibility = View.INVISIBLE
            lay_player_compressed.visibility = View.VISIBLE
        }
        player_compressed.setSpeed(0.05f)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            STORAGE_REQUEST_CODE -> {
                val storePermission = grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED
                if (storePermission)
                    showMainFragment()
                else {
                    longToast(R.string.permission_warning)
                    requestStoragePermission()
                }
                return
            }
        }
    }

    //Interface to communicate te text query to the fragment who implement it
    public fun setOnQueryTextListener(onQueryText: (String) -> Unit) {
        mOnQueryText = onQueryText
    }
    //This is a testing comment created on github :)
    //TODO. Delete the comment above :)
    //todo. this comment must be deleted too. It was created only with testing purposes

    //todo delete this testing function
    fun sumValues() {
        //Only branch02 has this testing comment
        val i = 20
        val ii = 100
    }
}
