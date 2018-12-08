package com.inglesdivino.simplemusicplayer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData

class PlayerViewModel (application: Application): AndroidViewModel(application){
    private val mRepository: Repository = Repository(application)
    private val mAllFolders: LiveData<List<Folder>>? = mRepository.getAllFolders()

    fun getAllFolders(): LiveData<List<Folder>>? = mAllFolders
    fun getSonsInFolder(folder: Folder?): LiveData<List<Song>>? {
        return mRepository.getSongsInFolder(folder)
    }
    fun insertFolder(folder: Folder) = mRepository.insertFolder(folder)
    fun deleteFolder(folder: Folder?) = mRepository.deleteFolder(folder)
    fun updateFolder(folder: Folder?) = mRepository.updateFolder(folder)
    fun insertSong(song: Song) = mRepository.insertSong(song)
    fun deleteSong(song: Song?) = mRepository.deleteSong(song)
    fun updateSong(song: Song?) = mRepository.updateSong(song)
}