package com.inglesdivino.simplemusicplayer

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.room.*
import org.jetbrains.annotations.NotNull

//ENTITIES
@Entity
data class Folder(
    @PrimaryKey(autoGenerate = true) var id: Int,
    var name: String,
    var date_mod: Long,
    var date_creation: Long
)

@Entity
data class Song(
    @PrimaryKey(autoGenerate = true) var id: Int,
    @NotNull var id_folder: Int,
    var media_id: Long,
    var name: String,
    var path: String
)

@Dao
interface FolderDao{

    @Insert
    fun insertFolder(folder: Folder)

    @Delete
    fun deleteFolder(folder: Folder?)

    @Update
    fun updateFolder(folder: Folder?)

    @Query("SELECT * FROM Folder ORDER BY name COLLATE NOCASE ASC")
    fun getAll(): LiveData<List<Folder>>

    @Query("SELECT * FROM Folder where id IN (:folderIds)")
    fun loadAllByIds(folderIds: IntArray): List<Folder>

    @Query("SELECT * FROM Folder where id = :id")
    fun getById(id: Int): Folder
}

@Dao
interface SongDao {
    @Insert
    fun insertSong(song: Song)

    @Delete
    fun deleteSong(song: Song?)

    @Update
    fun updateSong(song: Song?)

    @Query("SELECT * FROM Song")
    fun getAll(): LiveData<List<Song>>

    @Query("SELECT * FROM Song where id IN (:songIds)")
    fun loadAllByIds(songIds: IntArray): List<Song>

    @Query("SELECT * FROM Song where id = :id")
    fun getById(id: Int): Song

    @Query("SELECT * FROM Song where id_folder = :folder_id")
    fun getSongsInFolder(folder_id: Int?): LiveData<List<Song>>
}

@Database(entities = [Folder::class, Song::class], version = 1)
abstract class FolderRoomDatabase : RoomDatabase() {
    abstract fun folderDao(): FolderDao
    abstract fun songDao(): SongDao

    companion object {
        var INSTANCE: FolderRoomDatabase? = null

        fun getDatabase(context: Context): FolderRoomDatabase? {
            if (INSTANCE == null) {
                synchronized(FolderRoomDatabase::class.java) {
                    if (INSTANCE == null) {
                        INSTANCE = Room.databaseBuilder(
                            context,
                            FolderRoomDatabase::class.java, "simple_music_player_database")
                            .build()
                    }
                }
            }
            return INSTANCE
        }
    }
}