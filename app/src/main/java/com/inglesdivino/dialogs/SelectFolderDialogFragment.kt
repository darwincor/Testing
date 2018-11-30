package com.inglesdivino.dialogs

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.inglesdivino.simplemusicplayer.*

class SelectFolderDialogFragment: DialogFragment(){

    //Player View Model

    //Songs adapter
    var foldersAdapter: FoldersListAdapter? = null
    var rootView: View? = null

    //Player View Model
    var mPlayerViewModel: PlayerViewModel? = null

    var TAG = "Darwincio"

    var dialogNoFolders: TextView? = null

    //Listener for selection of songs
    private var mOnSelectedFolder: ((Folder?) -> Unit)? =  null

    @SuppressLint("RestrictedApi")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val builder = AlertDialog.Builder(activity)
        rootView = activity?.layoutInflater?.inflate(R.layout.dialog_select_folder, null)

        dialogNoFolders = rootView?.findViewById(R.id.dialog_no_folders)
        val selectFolderList: RecyclerView? = rootView?.findViewById(R.id.selectFolderList)

        selectFolderList?.layoutManager = LinearLayoutManager(context)
        foldersAdapter = FoldersListAdapter()
        selectFolderList?.adapter = foldersAdapter
        //Add divider to recycler view
        val dividerItemDecoration = DividerItemDecoration(activity, LinearLayoutManager.VERTICAL)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            dividerItemDecoration.setDrawable(activity?.resources?.getDrawable(R.drawable.divider)!!)
        }
        selectFolderList?.addItemDecoration(dividerItemDecoration)


        //Set the observer of live data
        mPlayerViewModel = ViewModelProviders.of(this).get(PlayerViewModel::class.java)
        mPlayerViewModel?.getAllFolders()?.observe(this, Observer {

            foldersAdapter?.setFolders(it)

            if(it.isEmpty())
                dialogNoFolders?.visibility = View.VISIBLE
            else
                dialogNoFolders?.visibility = View.GONE
        })

        builder.setView(rootView)
        return builder.create()
    }


    //Class adapter
    //Adapter Fol folders
    inner class FoldersListAdapter : RecyclerView.Adapter<FoldersListAdapter.ViewHolder>() {

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
                holder.folderMore.visibility = View.GONE
                holder.folderName.setTextColor(ContextCompat.getColor(context!!, R.color.textColorSecondary))
                DrawableCompat.setTint(holder.folderLogo.drawable, ContextCompat.getColor(context!!, R.color.textColorSecondary))

                holder.itemView.setOnClickListener {
                    mOnSelectedFolder?.invoke(current)
                    dismiss()
                }
            }
            else{
                //Covers the case of data not being ready yet
                holder.folderName.text = "No word"
            }
        }
        //todo add an extra bottom paddin to the recyclers, since the player could be visible
        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            var folderLogo: ImageView = view.findViewById(R.id.folder_logo)
            var folderName: TextView = view.findViewById(R.id.folder_name)
            var folderMore: ImageView = view.findViewById(R.id.folder_more)
        }

        fun setFolders(folders: List<Folder>) {
            mFolders = folders
            notifyDataSetChanged()
        }
    }

    fun setOnSelectedFolderListener(onSelectedFolder: (Folder?) -> Unit) {
        this.mOnSelectedFolder = onSelectedFolder
    }
}