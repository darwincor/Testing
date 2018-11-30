package com.inglesdivino.simplemusicplayer

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
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
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.inglesdivino.dialogs.SelectAudioDialogFragment
import kotlinx.android.synthetic.main.fragment_folders.*
import org.jetbrains.anko.*
import java.lang.Exception
import java.util.*
import kotlin.concurrent.schedule

class FoldersFragment : Fragment() {

    //Player View Model
    var mPlayerViewModel: PlayerViewModel? = null
    var mOnShowSongsInFolder: ((Folder)-> Unit)? = null

    var mainFoldersList: List<Folder>? = null

    var lastDeletedIndex: Int = -1  //Index of the last deleted folder (if -1, no folder has been deleted)

    //Variables to avoid nulling the long click
    private var lastLongClick: Long = 0

    private var mSelectAudioDialogFragment: SelectAudioDialogFragment? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_folders, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //Set title
        Timer("foldersFragmentTitle", false).schedule(200) {
            context?.runOnUiThread { (activity as MainActivity).setFragmentTitle(getString(R.string.folders)) }
        }

        //Add the adapter
        val adapter = FoldersListAdapter{v, folder, bundle -> performOnActionSong(v, folder, bundle) }

        foldersList.adapter = adapter
        foldersList.layoutManager = LinearLayoutManager(context)

        //Set the observer of live data
        mPlayerViewModel = ViewModelProviders.of(this).get(PlayerViewModel::class.java)
        mPlayerViewModel?.getAllFolders()?.observe(this, Observer {
            mainFoldersList = it
            adapter.setFolders(it)

            if(it.isEmpty())
                no_folders.visibility = View.VISIBLE
            else
                no_folders.visibility = View.GONE
        })

        //Create a new folder
        new_folder.setOnClickListener{
            activity?.alert{
                title=getString(R.string.create_folder)
                var fn: EditText? = null
                customView {
                    verticalLayout {
                        fn = editText{
                            singleLine = true
                            hintResource = R.string.folder_name
                        }
                        padding = dip(16)
                    }
                }
                positiveButton(R.string.create) {
                    if (fn?.text.isNullOrBlank()) {
                        activity?.toast(R.string.invalid_name)
                    } else {
                        val folder = Folder(0, fn?.text.toString().trim(), System.currentTimeMillis(), System.currentTimeMillis())
                        mPlayerViewModel?.insertFolder(folder)
                    }
                }
                negativeButton(R.string.cancel){}

            }?.show()
        }

        //Adjust the margins of the floating button
        adjustFloatingButton(new_folder)
    }

    private fun performOnActionSong(view: View, folder: Folder?, bundle: Bundle) {
        val action = bundle.getInt("action")
        val position = bundle.getInt("position")
        if (action == 0) {    //Click on item
            //Ignore the click if the previous action was a long click 500 milliseconds ago.
            if ((System.currentTimeMillis() - lastLongClick) < 500) {
                return
            }

            mOnShowSongsInFolder?.invoke(folder!!)

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
            showFolderMoreOptions(view, position)
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

    //Adapter Fol folders
    inner class FoldersListAdapter(val onItemAction: (View, Folder?, Bundle) -> Unit) : RecyclerView.Adapter<FoldersListAdapter.ViewHolder>() {

        var mFolders: List<Folder>? = null

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_folder, parent, false)
            return ViewHolder(itemView)
        }

        override fun getItemCount() = mFolders?.size?:0

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            if (mFolders != null) {
                val current: Folder? = mFolders?.get(position)
                holder.folderName.text = current?.name
                DrawableCompat.setTint(holder.folderLogo.drawable, ContextCompat.getColor(context!!, R.color.pink))
                //On option clicked
                holder.folderMore.setOnClickListener {
                    val bundle = Bundle()
                    bundle.putInt("action", 1)    //Action: long click
                    bundle.putInt("position", holder.adapterPosition)
                    onItemAction(it, current, bundle)
                }

                //Show contextual menu on long press as well
                holder.itemView.setOnLongClickListener {
                    val bundle = Bundle()
                    bundle.putInt("action", 2)    //Action: long click
                    bundle.putInt("position", holder.adapterPosition)
                    onItemAction(it, current, bundle)
                    false
                }
                holder.itemView.setOnClickListener {
                    val bundle = Bundle()
                    bundle.putInt("action", 0)    //Action: click
                    bundle.putInt("position", holder.adapterPosition)
                    onItemAction(it, current, bundle)
                }
            }
            else{
                //Covers the case of data not being ready yet
                holder.folderName.text = "No word"
            }
        }

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            var folderLogo: ImageView = view.findViewById(R.id.folder_logo)
            var folderName: TextView = view.findViewById(R.id.folder_name)
            var folderMore: ImageView = view.findViewById(R.id.folder_more)
        }

        fun setFolders(folders: List<Folder>) {
            mFolders = folders

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

    //Shows a dialog to add songs to the selected folder
    private fun showSelectAudioDialog(folder: Folder?) {
        mSelectAudioDialogFragment = SelectAudioDialogFragment()
        mSelectAudioDialogFragment?.setOnSelectedSongs {selected_audios ->
            selected_audios.forEach {
                val song = Song(0, folder?.id!!, it.id, it.title, it.path)
                mPlayerViewModel?.insertSong(song)
            }
            context?.toast(R.string.songs_added_successfully)
        }
        mSelectAudioDialogFragment?.show(activity?.supportFragmentManager, "mSelectAudioDialogFragment")
    }

    //Show folder more options
    @SuppressLint("RestrictedApi")
    private fun showFolderMoreOptions(view: View, pos: Int) {
        val menu = PopupMenu(context!!, view)
        menu.inflate(R.menu.popup_menu_folder)
        @SuppressLint("RestrictedApi") val menuHelper = MenuPopupHelper(context!!, menu.menu as MenuBuilder, view)
        menuHelper.setForceShowIcon(true)
        menu.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.add_songs_to_folder -> {
                    showSelectAudioDialog(mainFoldersList?.get(pos))
                }
                R.id.rename_folder -> {
                    //todo rename folder
                }
                R.id.delete_folder -> {
                    //todo ask for confirmation
                    mPlayerViewModel?.deleteFolder(mainFoldersList?.get(pos))
                    lastDeletedIndex = pos
                    //Reset the index after a certain time
                    Timer("resetFolderIndex", false).schedule(300){lastDeletedIndex = -1}
                }
            }
            false
        }
        menuHelper.show()
    }

    fun setOnShowSongsInFolder(onShowSongsInFolder: ((Folder) -> Unit)) {
        this.mOnShowSongsInFolder = onShowSongsInFolder
    }
}