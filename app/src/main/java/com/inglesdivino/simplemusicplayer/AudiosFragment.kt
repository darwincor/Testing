package com.inglesdivino.simplemusicplayer

import android.database.Cursor
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import androidx.loader.app.LoaderManager
import androidx.loader.content.Loader
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProviders
import androidx.loader.content.CursorLoader
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.inglesdivino.dialogs.OptionsDialogFragment
import com.inglesdivino.dialogs.PropertiesDialogFragment
import com.inglesdivino.dialogs.SelectFolderDialogFragment
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_player.*
import kotlinx.android.synthetic.main.fragment_songs_viewer.*
import org.jetbrains.anko.toast
import java.lang.StringBuilder
import kotlin.collections.ArrayList

class AudiosFragment : Fragment(), LoaderManager.LoaderCallbacks<Cursor> {

    //Player View Model
    public var mPlayerViewModel: PlayerViewModel? = null

    //Songs adapter
    //var songsAdapter: SongsAdapter? = null
    companion object {
        var songsAdapter: SongsAdapter? = null
    }

    private var mSelectFolderDialogFragment: SelectFolderDialogFragment? = null

    //Id's of the loader
    private val INTERNAL_CURSOR_ID = 0
    private val EXTERNAL_CURSOR_ID = 1
    val SETTINGS_REQUEST_CODE = 103

    //Dialogs fragments
    var mPropertiesDialogFragment: PropertiesDialogFragment? = null
    var mOptionsDialogFragment: OptionsDialogFragment? = null

    //String to filter songs
    var searchQuery: String = ""
    var filteredSongs = ArrayList<Audio>()

    //Songs player
    private val player = Player()

    private val INTERNAL_COLUMNS = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.DATA,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.ALBUM,
        MediaStore.Audio.Media.TRACK,
        "\"" + MediaStore.Audio.Media.INTERNAL_CONTENT_URI + "\"",
        MediaStore.Audio.Media.SIZE,
        MediaStore.Audio.Media.DATE_MODIFIED
    )

    private val EXTERNAL_COLUMNS = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.DATA,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.ALBUM,
        MediaStore.Audio.Media.TRACK,
        MediaStore.Audio.Media.IS_MUSIC,
        "\"" + MediaStore.Audio.Media.EXTERNAL_CONTENT_URI + "\"",
        MediaStore.Audio.Media.SIZE,
        MediaStore.Audio.Media.DATE_MODIFIED
    )

    //Keep a reference to internal and external mAudios
    private val externalSongs = ArrayList<Audio>()
    private val internalSongs = ArrayList<Audio>()

    //List of general mAudios (trimmed and not trimmed)
    internal var songs = ArrayList<Audio>()

    //Current selected song
    private var selected_audio: Audio? = null
    public var selected_index = -1

    //Properties of the current selected song
    private var cur_name: String? = ""
    private var cur_location: String? = ""
    private var cur_category: String? = ""
    private var cur_file_size: String? = ""
    private var cur_modified_time: String? = ""

    private var selected_audios: ArrayList<Audio> = ArrayList()

    private var onPlayNewAudio: ((Song) -> Unit)? = null
    private var onShowFolders: (()->Unit)? = null
    private var onPlaySelectedSongs: ((List<Song>) -> Unit)? = null

    //Filter search
    private var filter = ""
    private var searchMenu: Menu? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_songs_viewer, container, false)
    }

    //Variables to avoid nulling the long click
    private var lastLongClick: Long = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        audiosList.layoutManager = LinearLayoutManager(context)
        songsAdapter = SongsAdapter()
        audiosList.adapter = songsAdapter

        //Load the mAudios
        loaderManager.initLoader(EXTERNAL_CURSOR_ID, null, this)
        loaderManager.initLoader(INTERNAL_CURSOR_ID, null, this)

        //Show folders listener
        folders.setOnClickListener{ onShowFolders?.invoke()}

        //Adjust the margins of the fab button
        adjustFloatingButton(folders)

        mPlayerViewModel = ViewModelProviders.of(this).get(PlayerViewModel::class.java)

        //Listen to queries from SearchView
        (activity as MainActivity).setOnQueryTextListener { searchQuery = it; filterByString(it) }
    }

    fun adjustFloatingButton(fb: FloatingActionButton) {
        val r = resources
        val marginBottom: Int
        val marginRight: Int
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            marginBottom = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, (-2 + 0).toFloat(), r.displayMetrics).toInt()
            marginRight = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4.toFloat(), r.displayMetrics).toInt()
        } else {
            marginBottom = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, (16 + 0).toFloat(), r.displayMetrics).toInt()
            marginRight = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16.toFloat(), r.displayMetrics).toInt()
        }
        val params = fb.layoutParams as ConstraintLayout.LayoutParams
        params.bottomMargin = marginBottom
        params.rightMargin = marginRight
        fb.layoutParams = params
    }

    fun showSelectFolderDialog() {
        mSelectFolderDialogFragment = SelectFolderDialogFragment()
        mSelectFolderDialogFragment?.setOnSelectedFolderListener { folder ->
            selected_audios.forEach {audio ->
                val song = Song(0, folder?.id?:0, audio.id, audio.title, audio.path)
                mPlayerViewModel?.insertSong(song)
            }
            activity?.toast(R.string.songs_added_successfully)
        }
        mSelectFolderDialogFragment?.show(activity?.supportFragmentManager, "mSelectFolderDialogFragment")
    }

    private fun showPropertiesDialogFragment(audio: Audio) {
        mPropertiesDialogFragment = PropertiesDialogFragment()
        mPropertiesDialogFragment = PropertiesDialogFragment()
        mPropertiesDialogFragment?.name_str = audio.title
        mPropertiesDialogFragment?.location_str = audio.path
        mPropertiesDialogFragment?.file_size = audio.size
        mPropertiesDialogFragment?.modified_time = audio.mod_date
        mPropertiesDialogFragment?.show(activity?.supportFragmentManager,"mPropertiesDialogFragment")

    }

    fun showOptionsDialogFragment(audio: Audio) {
        mOptionsDialogFragment = OptionsDialogFragment()
        mOptionsDialogFragment?.mTitle = audio.title
        mOptionsDialogFragment?.setOnOptionSelectedListener {
            performOnOptionClicked(it, audio)
        }
        mOptionsDialogFragment?.show(activity?.supportFragmentManager, "mOptionsDialogFragment")
    }

    private fun performOnOptionClicked(option: Int, audio: Audio) {
        when (option) {
            OptionsDialogFragment.OPT_PLAY -> playOneSong(audio)

            OptionsDialogFragment.OPT_RENAME
            -> { //TODO implement the rename option
            }

            OptionsDialogFragment.OPT_SHARE     //Share
            -> {// todo implement the share option
            }

            OptionsDialogFragment.OPT_PROPERTIES -> showPropertiesDialogFragment(audio)

            OptionsDialogFragment.OPT_DELETE     //Delete
            -> {
                //todo delete this options
            }
        }
    }

    fun stopCarouselAnimation() {
        main_song_title?.stopAnimation()
        player_compressed?.stopAnimation()
    }

    fun playSelectedSongs() {
        val playlist = ArrayList<Song>()
        selected_audios.forEach {
            val song = Song(-1, -1, it.id, it.title, it.path)
            playlist.add(song)
        }
        onPlaySelectedSongs?.invoke(playlist)
    }

    fun playOneSong(audio: Audio) {
        val playlist = ArrayList<Song>()
        val song = Song(-1, -1, audio.id, audio.title, audio.path)
        playlist.add(song)
        onPlaySelectedSongs?.invoke(playlist)
    }

    //Clear the selected mAudios
    fun clearSelectedSongs() {
        selected_audios.clear()
        songsAdapter?.mAudios?.forEachIndexed { index, audio ->
            if(audio.selected)
                songsAdapter?.setAudioUnselected(index)
        }


        //songsAdapter!!.notifyDataSetChanged() todo uncoment thsi
        activity?.invalidateOptionsMenu()
    }

    //Returns true if there are selected mAudios
    fun thereAreSelectedSongs(): Boolean {
        return selected_audios.size > 0
    }

    //Returns true if the given audio is the list of the selected ones
    fun songIsSelected(audio: Audio): Boolean {
        return selected_audios.any {it.id == audio.id}
    }

    fun filterByString(filter: String) {
        this.filter = filter.trim()
        val args = Bundle()
        args.putString("filter", this.filter)
        synchronized(songs) {
            songs.clear()
        }

        activity?.supportLoaderManager?.restartLoader(EXTERNAL_CURSOR_ID, args, this)
        activity?.supportLoaderManager?.restartLoader(INTERNAL_CURSOR_ID, args, this)
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
        val selectionArgList: ArrayList<String> = ArrayList()
        selectionArgList.add("%espeak-data/scratch%")
        var selection = StringBuilder("((_DATA NOT LIKE ?) AND "+"("+MediaStore.Audio.Media.IS_MUSIC+"=1))")
        var baseUri = MediaStore.Audio.Media.INTERNAL_CONTENT_URI //By Default internal
        var projection = INTERNAL_COLUMNS //By default

        //Check if there is some filter
        var filter:String? = args?.getString("filter")

        if (filter != null && filter.isNotEmpty())
        {
            filter = "%$filter%"
            selection = StringBuilder(
                "(" + selection + " AND " +
                        "((TITLE LIKE ?) OR (ARTIST LIKE ?) OR (ALBUM LIKE ?)))"
            )
            selectionArgList?.add(filter)
            selectionArgList?.add(filter)
            selectionArgList?.add(filter)
        }


        when (id) {
            EXTERNAL_CURSOR_ID -> {
                baseUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                projection = EXTERNAL_COLUMNS
            }
        }

        val selectionArgArray = arrayOfNulls<String>(selectionArgList.size)
        selectionArgList.toArray(selectionArgArray)

        return CursorLoader(
            context!!,
            baseUri,
            projection,
            selection.toString(),
            selectionArgArray,
            MediaStore.Audio.Media.DEFAULT_SORT_ORDER
        )
    }

    override fun onLoaderReset(loader: Loader<Cursor>) {
    }

    override fun onLoadFinished(loader: Loader<Cursor>, data: Cursor?) {
        if (data == null)
            return

        val loaderId = loader.id

        //Save the mAudios in the corresponding list list
        var goOn = data.moveToFirst()
        val external = loader.id == EXTERNAL_CURSOR_ID

        when (loaderId) {
            EXTERNAL_CURSOR_ID -> externalSongs.clear()
            INTERNAL_CURSOR_ID -> internalSongs.clear()
        }

        while (goOn) {

            var index: Int = data.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)

            //Title
            val title = data.getString(index)

            //Artist
            index = data.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val artist = data.getString(index)

            //Album
            index = data.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val album = data.getString(index)

            //Id
            index = data.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val id = data.getLong(index)

            //Data
            index = data.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val path = data.getString(index)

            //Size
            index = data.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)    //In bytes
            val size = data.getLong(index)

            //Date of modification
            index = data.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)   //In seconds since 1970
            val date_mod = data.getLong(index)


            //Create the song item
            val song = Audio(title, artist, album, path, id, external, size, date_mod)

            //Insert new song
            when (loaderId) {
                EXTERNAL_CURSOR_ID -> externalSongs.add(song)
                INTERNAL_CURSOR_ID -> internalSongs.add(song)
            }
            goOn = data.moveToNext()
        }

        //Empty the mixed (internal and external) array of mAudios
        synchronized(songs) {
            songs.clear()
            songs.addAll(internalSongs)
            songs.addAll(externalSongs)

            //Sort mAudios by title
            //mAudios = mAudios.sortedWith(compareBy {it.title}) as ArrayList<Audio>
            songs = ArrayList(songs.sortedWith(compareBy {it.title}))
        }

        //Set titles in function of mAudios

        if (songs.size == 0) {    //If no result
            no_audios.text = getString(R.string.no_songs)
            no_audios.visibility = View.VISIBLE
        } else {
            no_audios.visibility = View.GONE
        }

        //If audios are being filtered
        songsAdapter?.setAudios(songs)

        //Mark the selected mAudios
        markSelectedAudios(songs)
    }

    //Class adapter
    inner class SongsAdapter: RecyclerView.Adapter<SongsAdapter.ViewHolder>() {

        var mAudios: ArrayList<Audio> = ArrayList()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_audio, parent, false)
            return ViewHolder(view)
        }

        override fun getItemCount(): Int {
            return mAudios.size
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            //Surround access to mAudios' elements with try catch, because loaders can modify 'mAudios' asynchronously
            try {
                val audio = mAudios[position]
                holder.row_title.text = audio.title  //Tile
                holder.row_artist.text = audio.artist    //Artist
                holder.row_album.text = audio.album  //Album

                if(audio.selected)   //If the song is selected
                    holder.itemView.setBackgroundColor(ContextCompat.getColor(context!!, R.color.aqua_dark_trans2))
                else
                    holder.itemView.setBackgroundColor(Color.parseColor("#000000"))

                //On option clicked
                holder.row_options_button.setOnClickListener {
                    showOptionsDialogFragment(audio)
                }

                //Show contextual menu on long press as well
                holder.itemView.setOnLongClickListener {v ->
                    if (!songIsSelected(audio)) {
                        selected_audios.add(audio)
                        if(!MainActivity.searchViewIsVisible)
                            activity?.invalidateOptionsMenu()
                        mAudios[holder.adapterPosition].selected = true
                    }

                    notifyItemChanged(holder.adapterPosition)
                    lastLongClick = System.currentTimeMillis()

                    false
                }
                holder.itemView.setOnClickListener {v ->
                    //Ignore the click if the previous action was a long click 500 milliseconds ago.
                    if ((System.currentTimeMillis() - lastLongClick) > 500) {
                        if (songIsSelected(audio)) {
                            selected_audios = ArrayList(selected_audios.filterNot { it.id == audio.id })   //Remove the clicked song from the list of selected ones
                            mAudios[holder.adapterPosition].selected = false
                            notifyItemChanged(holder.adapterPosition)
                            if(!thereAreSelectedSongs()){    //If there are not more selected mAudios, update the action bar
                                if(!MainActivity.searchViewIsVisible)
                                    activity?.invalidateOptionsMenu()
                            }

                        } else {
                            if(thereAreSelectedSongs()){    //If there are selected mAudios, continue selecting
                                selected_audios.add(audio)
                                if(!MainActivity.searchViewIsVisible)
                                    activity?.invalidateOptionsMenu()
                                mAudios[holder.adapterPosition].selected = true
                                notifyDataSetChanged()
                            }
                            else{   //Play audio or pause
                                val song = Song(-1, -1, audio.id, audio.title, audio.path)
                                onPlayNewAudio?.invoke(song)
                            }
                        }
                    }
                }

                //Get the number of selected mAudios
                if (selected_audios.size > 0) {
                    activity?.selected_counter?.visibility = View.VISIBLE
                    activity?.selected_counter?.text = selected_audios.size.toString()
                } else {
                    activity?.selected_counter?.visibility = View.GONE
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val row_title = view.findViewById<TextView>(R.id.row_title)
            val row_artist = view.findViewById<TextView>(R.id.row_artist)
            val row_album = view.findViewById<TextView>(R.id.row_album)
            val row_icon = view.findViewById<ImageView>(R.id.row_icon)
            val row_options_button = view.findViewById<ImageView>(R.id.row_options_button)
            val audio_row = view.findViewById<LinearLayout>(R.id.audio_row)
        }

        fun setAudios(audios: ArrayList<Audio>){
            mAudios.clear()
            mAudios.addAll(audios)
            //mAudios = audios
            notifyDataSetChanged()
        }

        fun setAudioSelected(position: Int) {
            mAudios[position].selected = true
            notifyItemChanged(position)
        }

        fun setAudioUnselected(position: Int) {
            mAudios[position].selected = false
            notifyItemChanged(position)
        }

        /*fun notifyItemHasChanged(pos: Int) {
            Log.i("Darwincio_del", "setItemSelected(): position = "+pos)
            Log.i("Darwincio_del", "setItemSelected(): mAudios.length = "+mAudios.size)
            notifyItemChanged(pos)
        }

        fun setItemSelected(position: Int) {
            mAudios[position].selected = true
        }*/
    }

    fun onSearchViewCollapsed() {
        if (thereAreSelectedSongs()) {
            activity?.invalidateOptionsMenu()
        }
    }

    private fun markSelectedAudios(audios: ArrayList<Audio>) {
        audios.forEach { audio ->
            if(selected_audios.any { it.id == audio.id})
                audio.selected = true
        }
    }

    //Listeners
    fun setOnNewAudio(onPlayNewAudio: (Song) -> Unit) {
        this.onPlayNewAudio = onPlayNewAudio
    }

    fun setOnShowFolders(onShowFolders: () -> Unit) {
        this.onShowFolders = onShowFolders
    }

    fun setOnPlaySelectedSongsListener(onPlaySelectedSongs: (List<Song>) -> Unit) {
        this.onPlaySelectedSongs = onPlaySelectedSongs
    }
}