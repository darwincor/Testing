package com.inglesdivino.dialogs

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.loader.app.LoaderManager
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.inglesdivino.simplemusicplayer.*
import org.jetbrains.anko.toast
import java.lang.StringBuilder

class SelectAudioDialogFragment: DialogFragment(), LoaderManager.LoaderCallbacks<Cursor>{

    //Songs adapter
    var songsAdapter: SongsAdapter? = null
    var rootView: View? = null
    val STORAGE_REQUEST_CODE = 1
    val RC_FROM_SAF = 2

    //Player View Model
    var mPlayerViewModel: PlayerViewModel? = null

    var TAG = "Darwincio"

    //Id's of the loader
    private val INTERNAL_CURSOR_ID = 0
    private val EXTERNAL_CURSOR_ID = 1

    //Filter search
    private var filter = ""

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

    //Current audios
    private var currentSongs: List<Song>?= null //Songs already included in the folder

    //Variables to avoid nulling the long click
    private var lastLongClick: Long = 0

    private var selected_audios: ArrayList<Audio> = ArrayList()

    var dialogNoAudios: TextView? = null
    var addSongsTitle: TextView? = null
    var addSongs: Button? = null

    //Listener for selection of songs
    private var onSelectedSongs: ((ArrayList<Audio>) -> Unit)? =  null

    @SuppressLint("RestrictedApi")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val builder = AlertDialog.Builder(activity)
        rootView = activity?.layoutInflater?.inflate(R.layout.dialog_select_audio, null)

        dialogNoAudios = rootView?.findViewById(R.id.dialog_no_folders)
        val fabOpenSaf: FloatingActionButton? = rootView?.findViewById(R.id.fab_open_saf)
        val selectSongsList: RecyclerView? = rootView?.findViewById(R.id.selectSongsList)
        val searchQuery: EditText? = rootView?.findViewById(R.id.search_query)
        addSongs = rootView?.findViewById(R.id.play_songs)
        addSongsTitle = rootView?.findViewById(R.id.add_songs_title)

        addSongsTitle?.text = String.format(getString(R.string.add_songs), "")
        addSongs?.visibility = View.GONE


        addSongs?.setOnClickListener{
            onSelectedSongs?.invoke(selected_audios)
            dismiss()
        }

        searchQuery?.afterTextChanged {
            filterByString(it)
        }

        dialogNoAudios?.visibility = View.GONE

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            //Load from SAF
            fabOpenSaf?.setOnClickListener{
                if (!isStoragePermissionGranted())
                    requestStoragePermission()
                else
                    launchSAF()
            }
        } else {
            fabOpenSaf?.visibility = View.GONE
        }

        selectSongsList?.layoutManager = LinearLayoutManager(context)
        songsAdapter = SongsAdapter(){performOnActionSong(it)}
        selectSongsList?.adapter = songsAdapter
        //Add divider to recycler view
        val dividerItemDecoration = DividerItemDecoration(activity, LinearLayoutManager.VERTICAL)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            dividerItemDecoration.setDrawable(activity?.resources?.getDrawable(R.drawable.divider)!!)
        }
        selectSongsList?.addItemDecoration(dividerItemDecoration)

        //Load the mAudios
        loaderManager.initLoader(EXTERNAL_CURSOR_ID, null, this)
        loaderManager.initLoader(INTERNAL_CURSOR_ID, null, this)

        builder.setView(rootView)
        return builder.create()
    }

    private fun filterByString(filter: String) {
        this.filter = filter.trim()
        val args = Bundle()
        args.putString("filter", this.filter)
        synchronized(songs) {
            songs.clear()
        }
        activity?.supportLoaderManager?.restartLoader(EXTERNAL_CURSOR_ID, args, this)
        activity?.supportLoaderManager?.restartLoader(INTERNAL_CURSOR_ID, args, this)
    }

    //Performs the action when a song is clicked
    private fun performOnActionSong(bundle: Bundle) {
        val action = bundle.getInt("action")
        val audio = bundle.getSerializable("song") as Audio
        val position = bundle.getInt("position")
        if (action == 0) {    //Click on item
            //Ignore the click if the previous action was a long click 500 milliseconds ago.
            if ((System.currentTimeMillis() - lastLongClick) < 500) {
                return
            }

            //If the audio is already in the folder, show a message
            if (isAudioInFolder(audio)) {
                Toast.makeText(context, R.string.song_in_folder, Toast.LENGTH_SHORT).show()
                return
            }

            if (!songIsSelected(audio)) {
                selected_audios.add(audio)
                //songsAdapter!!.audios[position].selected = true
                songsAdapter!!.setSelectedItem(position)
                songsAdapter!!.notifyItemChanged(position)
            }
            else{
                selected_audios = ArrayList(selected_audios.filterNot { it.id == audio.id })   //Remove the clicked song from the list of selected ones
                //songsAdapter!!.audios[position].selected = false
                songsAdapter!!.setUnselectedItem(position)
                songsAdapter!!.notifyItemChanged(position)
                if(!thereAreSelectedSongs()){}    //If there are not more selected mAudios, update the action bar
                activity?.invalidateOptionsMenu()
            }
        }
        else if (action == 1) { //Click on the arrow of the item view
        } else {           //Long click
            lastLongClick = System.currentTimeMillis()
        }
        //Get the number of selected mAudi os
        if (selected_audios.size > 0) {
            addSongsTitle?.text = String.format(getString(R.string.add_songs), "("+selected_audios.size+")")
            addSongs?.visibility = View.VISIBLE
        } else {
            addSongsTitle?.text = String.format(getString(R.string.add_songs), "")
            addSongs?.visibility = View.GONE
        }
    }

    //Returns true if there are selected mAudios
    fun thereAreSelectedSongs(): Boolean {
        return selected_audios.size > 0
    }

    //Returns true if the given audio is the list of the selected ones
    fun songIsSelected(audio: Audio): Boolean {
        return selected_audios.any {it.id == audio.id}
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
            selectionArgList.add(filter)
            selectionArgList.add(filter)
            selectionArgList.add(filter)
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
            dialogNoAudios?.text = getString(R.string.no_matches)
            dialogNoAudios?.visibility = View.VISIBLE
        } else {
            dialogNoAudios?.visibility = View.GONE
        }

        //foldersAdapter!!.mAudios = mAudios
        //songsAdapter!!.audios = songs
        songsAdapter?.setAudios(songs)
        songsAdapter!!.notifyDataSetChanged()

        //Mark the selected mAudios
        markSelectedAudios(songs)
    }

    override fun onLoaderReset(loader: Loader<Cursor>) {
    }

    private fun markSelectedAudios(audios: ArrayList<Audio>) {
        audios.forEach { audio ->
            if(selected_audios.any { it.id == audio.id})
                audio.selected = true
        }
    }

    //Checks if a given audio is already in the current list of songs
    private fun isAudioInFolder(audio: Audio): Boolean {
        return if(currentSongs != null) {
            currentSongs!!.any { it.media_id == audio.id}
        } else false
    }

    //Class adapter
    class SongsAdapter(val onItemAction: (Bundle) -> Unit): RecyclerView.Adapter<SongsAdapter.ViewHolder>() {
        var mAudios: ArrayList<Audio> = ArrayList()
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_select_audio, parent, false)
            return ViewHolder(view)
        }

        override fun getItemCount(): Int {
            return mAudios.size
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            //Surround access to mAudios' elements with try catch, because loaders can modify 'mAudios' asynchronously
            try {
                val song = mAudios[position]
                holder.row_title.text = song.title  //Tile
                holder.row_artist.text = song.artist    //Artist
                holder.row_album.text = song.album  //Album

                if (song.selected) {   //If the song is selected
                    holder.itemView.setBackgroundColor(Color.parseColor("#33009999"))
                } else {
                    holder.itemView.setBackgroundColor(Color.parseColor("#00000000"))
                }

                //On option clicked
                holder.row_options_button.setOnClickListener {
                    val bundle = Bundle()
                    bundle.putInt("action", 1)    //Action: long click
                    bundle.putInt("position", holder.adapterPosition)
                    bundle.putSerializable("song", song)
                    onItemAction(bundle)
                }

                //Show contextual menu on long press as well
                holder.audio_row.setOnLongClickListener {
                    val bundle = Bundle()
                    bundle.putInt("action", 2)    //Action: long click
                    bundle.putInt("position", holder.adapterPosition)
                    bundle.putSerializable("song", song)
                    onItemAction(bundle)
                    false
                }
                holder.audio_row.setOnClickListener {
                    val bundle = Bundle()
                    bundle.putInt("action", 0)    //Action: click
                    bundle.putInt("position", holder.adapterPosition)
                    bundle.putSerializable("song", song)
                    onItemAction(bundle)
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val row_title = view.findViewById<TextView>(R.id.row_title)
            val row_artist = view.findViewById<TextView>(R.id.row_artist)
            val row_album = view.findViewById<TextView>(R.id.row_album)
            val row_icon = view.findViewById<ImageView>(R.id.row_icon)
            val row_options_button = view.findViewById<ImageView>(R.id.row_options_button)
            val audio_row = view.findViewById<LinearLayout>(R.id.audio_row)
        }

        fun setAudios(audios: ArrayList<Audio>): Unit {
            this.mAudios = audios
        }

        fun setSelectedItem(pos: Int) {
            this.mAudios[pos].selected = true
        }

        fun setUnselectedItem(pos: Int) {
            this.mAudios[pos].selected = false
        }
    }

    /**
     * From Android 4.4, when the user selects "RC_FROM_GALLERY", call this function instead.
     */
    fun launchSAF() {

        //As for Android 4.4, we can launch Storage Access Framework (SAF)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {

            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "audio/*"

            try {
                startActivityForResult(intent, RC_FROM_SAF)
            } catch (ex: Exception) {
                ex.printStackTrace()
                activity?.runOnUiThread{activity?.toast(getString(R.string.error))}
            }
        }
    }

    //Check if the storage permission is granted
    fun isStoragePermissionGranted(): Boolean {
        val stoPermission = ContextCompat.checkSelfPermission(context!!, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        return stoPermission != PackageManager.PERMISSION_DENIED
    }

    fun requestStoragePermission() {
        requestPermissions(arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), STORAGE_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            STORAGE_REQUEST_CODE -> {
                val storePermission = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
                if (storePermission) {
                    //Do pending task
                }
            }
        }
    }

    //Set the current songs of the current folder
    fun setCurrentSongs(curAudios: List<Song>?) {
        currentSongs = curAudios
    }

    fun setOnSelectedSongs(onSelectedSongs: (ArrayList<Audio>) -> Unit) {
        this.onSelectedSongs = onSelectedSongs
    }
}