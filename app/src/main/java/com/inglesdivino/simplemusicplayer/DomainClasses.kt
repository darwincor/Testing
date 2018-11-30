package com.inglesdivino.simplemusicplayer

import java.io.Serializable

data class Audio(val title: String, val artist: String, val album: String, val path: String,
                 val id: Long, val external: Boolean, val size: Long,
                 val mod_date: Long, val checked: Boolean = false, var selected: Boolean = false): Serializable