package com.inglesdivino.simplemusicplayer

import android.content.Context
import androidx.lifecycle.LiveData
import org.jetbrains.anko.doAsync

public class Repository(context: Context) {
    private val db: FolderRoomDatabase? = FolderRoomDatabase.getDatabase(context)
    private val mFolderDao: FolderDao? = db?.folderDao()
    private val mSongDao: SongDao? = db?.songDao()
    private val mAllFolders: LiveData<List<Folder>>? = mFolderDao?.getAll()
    //private val mAllSongs: LiveData<List<Song>>? = mSongDao?.getAll()
    fun getAllFolders(): LiveData<List<Folder>>? = mAllFolders
    fun getSongsInFolder(folder: Folder?): LiveData<List<Song>>? {
        return mSongDao?.getSongsInFolder(folder?.id)
    }
    fun insertFolder(folder: Folder) = doAsync {mFolderDao?.insertFolder(folder)}
    fun deleteFolder(folder: Folder?) = doAsync { mFolderDao?.deleteFolder(folder) }
    fun insertSong(song: Song) = doAsync { mSongDao?.insertSong(song) }
    fun deleteSong(song: Song?) = doAsync { mSongDao?.deleteSong(song) }
}