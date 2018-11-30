package com.inglesdivino.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.fragment.app.DialogFragment
import com.inglesdivino.simplemusicplayer.R

class OptionsDialogFragment : DialogFragment(){

    //internal var mListener: AudioOptionsListener? = null
    var mOnOptionSelected: ((Int) -> Unit)? = null

    var mTitle: String? = ""
    var category: String? = ""
    internal var layout: View? = null

    private var hideEditOption = false
    var rootView: View? = null

    @NonNull
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        if (savedInstanceState != null) {
            mTitle = savedInstanceState.getString("title")
            category = savedInstanceState.getString("category")
            hideEditOption = savedInstanceState.getBoolean("hideEditOption")
        }

        val builder = android.app.AlertDialog.Builder(activity)
        rootView = activity?.layoutInflater?.inflate(R.layout.dialog_options, null)

        rootView?.findViewById<TextView>(R.id.title)?.text = mTitle

        //Set listeners
        rootView?.findViewById<TextView>(R.id.txt_play)?.setOnClickListener {mOnOptionSelected?.invoke(OPT_PLAY); dismiss()}
        rootView?.findViewById<TextView>(R.id.txt_rename)?.setOnClickListener {mOnOptionSelected?.invoke(OPT_RENAME)}
        rootView?.findViewById<TextView>(R.id.txt_share)?.setOnClickListener {mOnOptionSelected?.invoke(OPT_SHARE)}
        rootView?.findViewById<TextView>(R.id.txt_properties)?.setOnClickListener {mOnOptionSelected?.invoke(OPT_PROPERTIES)}
        rootView?.findViewById<TextView>(R.id.txt_delete)?.setOnClickListener {mOnOptionSelected?.invoke(OPT_DELETE)}

        builder.setView(rootView)

        // Add action buttons
        builder.setPositiveButton(R.string.ok){ _, _ -> dismiss()}

        return builder.create()
    }

    fun setTitle(title: String) {
        this.mTitle = title
    }


    fun setOnOptionSelectedListener(onOptionSelected: (Int)->Unit){
        mOnOptionSelected = onOptionSelected
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("title", mTitle)
        outState.putString("category", category)
        outState.putBoolean("hideEditOption", hideEditOption)
    }

    companion object {
        const val OPT_PLAY = 0
        const val OPT_RENAME = 1
        const val OPT_SHARE = 2
        const val OPT_PROPERTIES = 3
        const val OPT_DELETE = 6
    }
}