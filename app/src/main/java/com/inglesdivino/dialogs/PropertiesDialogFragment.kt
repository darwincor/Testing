package com.inglesdivino.dialogs

import android.app.Dialog
import android.os.Bundle
import android.text.format.DateFormat
import android.view.View
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.inglesdivino.simplemusicplayer.R


class PropertiesDialogFragment : DialogFragment() {

    private var dialog: AlertDialog? = null

    var name_str: String? = ""
    var location_str: String? = ""
    var file_size: Long = 0
    var modified_time: Long = 0
    var rootView: View? = null

    @NonNull
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        if (savedInstanceState != null) {
            name_str = savedInstanceState.getString("name_str")
            location_str = savedInstanceState.getString("location_str")
            file_size = savedInstanceState.getLong("file_size")
            modified_time = savedInstanceState.getLong("modified_time")
        }

        val builder = android.app.AlertDialog.Builder(activity)
        rootView = activity?.layoutInflater?.inflate(R.layout.dialog_properties, null)

        builder.setTitle(getString(R.string.song_properties))
        builder.setView(rootView)

        val name = rootView?.findViewById<TextView>(R.id.name)
        val location = rootView?.findViewById<TextView>(R.id.location)
        val fileSize = rootView?.findViewById<TextView>(R.id.file_size)
        val modifiedTime = rootView?.findViewById<TextView>(R.id.modified_time)


        //Set values to the fields
        name?.text = name_str
        location?.text = location_str
        fileSize?.text = getHumaReadableSize(file_size)
        modifiedTime?.text = getReadableDateFromSeconds(modified_time)

        builder.setPositiveButton(R.string.ok){ _, _ -> dismiss()}

        return builder.create()
    }

    //Get human readable size from bytes
    private fun getHumaReadableSize(bytes: Long): String {

        // Convert the bytes to Kilobytes (1 KB = 1024 Bytes)
        val fileSizeInKB = bytes / 1024

        if (fileSizeInKB < 1000) {
            return fileSizeInKB.toString() + " KB"
        } else {
            // Convert the KB to MegaBytes (1 MB = 1024 KBytes)
            val fileSizeInMB = fileSizeInKB / 1024
            if (fileSizeInMB < 1000) {
                return fileSizeInMB.toString() + " MB"
            } else {
                val fileSizeInGB = fileSizeInMB / 1024
                return fileSizeInGB.toString() + " GB"
            }
        }
    }

    //Get a readable date from seconds
    private fun getReadableDateFromSeconds(seconds: Long): String {
        val dateFormat = "dd/MM/yyyy hh:mm:ss"
        return DateFormat.format(dateFormat, seconds * 1000).toString()
    }


    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("name_str", name_str)
        outState.putString("location_str", location_str)
        outState.putLong("file_size", file_size)
        outState.putLong("modified_time_str", modified_time)
    }
}