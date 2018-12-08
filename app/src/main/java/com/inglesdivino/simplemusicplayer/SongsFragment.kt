package com.inglesdivino.simplemusicplayer

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.view.menu.MenuPopupHelper
import androidx.appcompat.widget.PopupMenu
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.inglesdivino.dialogs.SelectAudioDialogFragment
import kotlinx.android.synthetic.main.fragment_songs.*
import org.jetbrains.anko.*
import java.lang.Exception
import java.util.*
import kotlin.concurrent.schedule

class SongsFragment : Fragment() {

    //Player View Model
    public var mPlayerViewModel: PlayerViewModel? = null

    //Listeners
    var mOnSelectSong: ((Song) -> Unit)? = null
    var mOnSelectSongs: ((List<Song>, Int) -> Unit)? = null

    //Song id
    var mFolder: Folder? = null

    var mMainSongs: List<Song>? = null

    //Variables to avoid nulling the long click
    private var lastLongClick: Long = 0

    var lastDeletedIndex: Int = -1  //Index of the last deleted song (if -1, no folder has been deleted)

    private var mSelectAudioDialogFragment: SelectAudioDialogFragment? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_songs, container, false)
    }

    @SuppressLint("RestrictedApi")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //Set title after 300 milliseconds
        Timer("songsFragmentTitle", false).schedule(200) {
            context?.runOnUiThread {(activity as MainActivity).setFragmentTitle(mFolder?.name!!)}
        }

        //Add the adapter
        val adapter = SongsListAdapter{view, bundle -> performOnActionSong(view, bundle)}

        songsList.adapter = adapter
        songsList.layoutManager = LinearLayoutManager(context)

        //Set the observer of live data
        mPlayerViewModel = ViewModelProviders.of(this).get(PlayerViewModel::class.java)
        mPlayerViewModel?.getSonsInFolder(mFolder)?.observe(this, Observer {
            mMainSongs = it
            adapter.setSongs(it)

            //Check if there are songs available
            if (it.isEmpty()) {
                no_songs.visibility = View.VISIBLE
                no_songs.setOnClickListener { showSelectAudioDialog() }
                play_folder_songs.visibility = View.GONE
            }
            else {
                no_songs.visibility = View.GONE
                play_folder_songs.visibility = View.VISIBLE
            }
        })
        //Add new songs
        //Todo add an option in the menuBar
        //no_songs.setOnClickListener{showSelectAudioDialog()}
        play_folder_songs.setOnClickListener { mOnSelectSongs?.invoke(mMainSongs!!, 0) }
    }

    //Performs the action when a song is clicked
    private fun performOnActionSong(view: View, bundle: Bundle) {
        val action = bundle.getInt("action")
        val position = bundle.getInt("position")
        if (action == 0) {    //Click on item

            //Ignore the click if the previous action was a long click 500 milliseconds ago.
            if ((System.currentTimeMillis() - lastLongClick) < 500) {
                return
            }

            //mOnSelectSong?.invoke(mMainSongs!![position])
            mOnSelectSongs?.invoke(mMainSongs!!, position)

            /*if (songIsSelected(song)) {
                selected_audios = ArrayList(selected_audios.filterNot { it.id == song.id })   //Remove the clicked song from the list of selected ones
                foldersAdapter!!.mAudios[position].selected = false
                foldersAdapter!!.notifyItemChanged(position)
                if(!thereAreSelectedSongs()){}    //If there are not more selected mAudios, update the action bar
                activity?.invalidateOptionsMenu()
            } else {
                if(thereAreSelectedSongs()){    //If there are selected mAudios, continue selecting
                    selected_audios.add(song)
                    activity?.invalidateOptionsMenu()

                    foldersAdapter!!.mAudios[position].selected = true
                    foldersAdapter!!.notifyItemChanged(position)
                }
                else{   //Play audio or pause
                    onPlayNewAudio?.invoke(song.path)
                }
            }*/
        }
        else if (action == 1) { //Click on the "more options" of the item view
            showSongMoreOptions(view, position)
        } else {           //Long click
            /*if (!songIsSelected(song)) {
                selected_audios.add(song)
                activity?.invalidateOptionsMenu()

                foldersAdapter!!.mAudios[position].selected = true
                foldersAdapter!!.notifyItemChanged(position)
            }*/
            lastLongClick = System.currentTimeMillis()
        }
        //Get the number of selected mAudios
        /*if (selected_audios.size > 0) {
            activity?.selected_counter?.visibility = View.VISIBLE
            activity?.selected_counter?.text = selected_audios.size.toString()
        } else {
            activity?.selected_counter?.visibility = View.GONE
        }*/
    }

    private fun showSelectAudioDialog() {
        mSelectAudioDialogFragment = SelectAudioDialogFragment()
        mSelectAudioDialogFragment?.setOnSelectedSongs {selected_audios ->
            selected_audios.forEach {
                val song = Song(0, mFolder?.id!!, it.id, it.title, it.path)
                mPlayerViewModel?.insertSong(song)
            }
        }
        mSelectAudioDialogFragment?.show(activity?.supportFragmentManager, "mSelectAudioDialogFragment")
    }

    //Set the id of the folder (containing all these songs in this fragment)
    fun setFolder(folder: Folder) {
        mFolder = folder
    }

    //Adapter Fol folders
    inner class SongsListAdapter(val onItemAction: (View, Bundle) -> Unit) : RecyclerView.Adapter<SongsListAdapter.ViewHolder>() {

        var mSongs: List<Song>? = null

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_song, parent, false)
            return ViewHolder(itemView)
        }

        override fun getItemCount() = mSongs?.size?:0

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            if (mSongs != null) {
                val current: Song? = mSongs?.get(position)
                holder.songName.text = current?.name
            }
            else{
                //Covers the case of data not being ready yet
                holder.songName.text = "No Song"
            }
            //On option clicked
            holder.songMore.setOnClickListener {
                val bundle = Bundle()
                bundle.putInt("action", 1)    //Action: long click
                bundle.putInt("position", holder.adapterPosition)
                onItemAction(holder.songMore, bundle)
            }

            //Show contextual menu on long press as well
            holder.itemView.setOnLongClickListener {
                val bundle = Bundle()
                bundle.putInt("action", 2)    //Action: long click
                bundle.putInt("position", holder.adapterPosition)
                onItemAction(it, bundle)
                false
            }
            holder.itemView.setOnClickListener {
                val bundle = Bundle()
                bundle.putInt("action", 0)    //Action: click
                bundle.putInt("position", holder.adapterPosition)
                onItemAction(holder.itemView, bundle)
            }
        }

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            var songName: TextView = view.findViewById(R.id.song_name)
            var songMore: ImageView = view.findViewById(R.id.song_more)
        }

        fun setSongs(songs: List<Song>) {
            mSongs = songs
            //todo when deleting a folder, delete all its contents (songs) first
            if (lastDeletedIndex >= 0) {
                try {
                    notifyItemRemoved(lastDeletedIndex)
                    lastDeletedIndex = -1
                }catch (ignored: Exception){notifyDataSetChanged()}
            }
            else
                notifyDataSetChanged()
        }
    }

    fun setOnSelectSong(onSelectSong: (Song) -> Unit) {
        this.mOnSelectSong = onSelectSong
    }

    //Show folder more options
    @SuppressLint("RestrictedApi")
    private fun showSongMoreOptions(view: View, pos: Int) {
        val menu = PopupMenu(context!!, view)
        menu.inflate(R.menu.popup_menu_song)
        @SuppressLint("RestrictedApi") val menuHelper = MenuPopupHelper(context!!, menu.menu as MenuBuilder, view)
        menuHelper.setForceShowIcon(true)
        menu.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.rename_song -> {
                    showRenameSongDialog(mMainSongs?.get(pos))
                }
                R.id.delete_song -> {
                    //todo confirm song deletion
                    mPlayerViewModel?.deleteSong(mMainSongs?.get(pos))
                    lastDeletedIndex = pos
                    //Reset the index after a certain time
                    Timer("resetSongIndex", false).schedule(300){lastDeletedIndex = -1}
                }
            }
            false
        }
        menuHelper.show()
    }

    //Show a dialog to rename a song
    private fun showRenameSongDialog(song: Song?) {
        activity?.alert{
            title=getString(R.string.rename_folder)
            var etRename: EditText? = null
            customView {
                verticalLayout {
                    etRename = editText{
                        singleLine = true
                        hintResource = R.string.song_name
                        setText(song?.name)
                        setSelectAllOnFocus(true)
                    }
                    padding = dip(16)
                }
            }
            positiveButton(R.string.ok) {
                if (etRename?.text.isNullOrBlank()) {
                    activity?.toast(R.string.invalid_name)
                } else {
                    val newName = etRename?.text.toString().trim()
                    song?.name = newName
                    mPlayerViewModel?.updateSong(song)
                }
            }
            negativeButton(R.string.cancel){}

        }?.show()
    }

    fun setOnSelectSongs(onSelectSongs: (List<Song>, Int) -> Unit) {
        this.mOnSelectSongs = onSelectSongs
    }
}