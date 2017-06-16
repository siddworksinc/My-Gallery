package com.simplemobiletools.gallery.dialogs

import android.app.Activity
import android.content.res.Resources
import android.support.v7.app.AlertDialog
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.simplemobiletools.commons.extensions.formatLastModified
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.commons.extensions.updateTextColors
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.extensions.config
import com.simplemobiletools.gallery.models.Shortcut
import kotlinx.android.synthetic.main.dialog_shortcut.view.*
import java.util.*

class EditShortcutDialog() {
    lateinit var mInflater: LayoutInflater
    lateinit var mPropertyView: ViewGroup
    lateinit var mResources: Resources

    /**
     * A File Properties dialog constructor with an optional parameter, usable at 1 file selected
     *
     * @param activity request activity to avoid some Theme.AppCompat issues
     * @param path the file path
     * @param countHiddenItems toggle determining if we will count hidden files themselves and their sizes (reasonable only at directory properties)
     */
    constructor(activity: Activity, medium: Shortcut, callback: () -> Unit) : this() {
        mInflater = LayoutInflater.from(activity)
        mResources = activity.resources
        val view = mInflater.inflate(R.layout.dialog_shortcut, null)

        var name = ""
        var path = ""
        var modified = ""
        if(medium != null) {
            name = medium.name
            path = medium.path
            modified = medium.modified.formatLastModified()
        }

        view.property_label_name.text = mResources.getString(R.string.name)
        view.property_value_name_value.setText(name)

        view.property_label_path.text = mResources.getString(R.string.path)
        view.property_value_path_value.text = path

        view.hide_thumbnail.isChecked = medium.isThumbnailHidden
        view.hide_thumbnail_holder.setOnClickListener {
            view.hide_thumbnail.toggle()
            val token = object : TypeToken<List<Shortcut>>() {}.type
            val shortcuts =  Gson().fromJson<ArrayList<Shortcut>>(activity.config.shortcuts, token) ?: ArrayList<Shortcut>(1)
            shortcuts.forEach {
                if(it.path == medium.path) {
                    it.isThumbnailHidden = !medium.isThumbnailHidden
                }
            }
            val directories = Gson().toJson(shortcuts)
            activity.config.shortcuts = directories
        }

        view.passcode_protection.isChecked = medium.passcode!=null
        view.passcode_protection_holder.setOnClickListener {
            view.passcode_protection.toggle()
            var passcode = null;

            if(view.passcode_protection.isChecked) {
                view.passcode_pass.visibility = View.VISIBLE
                view.passcode_pass_confirm.visibility = View.VISIBLE
                view.passcode_warning.visibility = View.VISIBLE
                view.passcode_warning.text = "Enter a passcode to activate protection"

                view.passcode_pass_confirm.addTextChangedListener(object : TextWatcher {
                    override fun afterTextChanged(p0: Editable?) {
                        if(view.passcode_pass.text?.toString() == view.passcode_pass_confirm.text?.toString()) {
                            if(view.passcode_pass.text?.toString() != null &&
                                    view.passcode_pass.text.toString() != "") {
                                savePasscode(activity, medium, view.passcode_pass.text?.toString())
                                view.passcode_warning.text = "Passcode saved"
                            } else {
                                view.passcode_warning.text = "Enter a passcode to activate protection"
                            }
                        } else {
                            view.passcode_warning.text = "Passcodes do not match. Protection will not be activated"
                        }
                    }

                    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                    }

                    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                    }
                })

                view.passcode_pass.addTextChangedListener(object : TextWatcher {
                    override fun afterTextChanged(p0: Editable?) {
                        if(view.passcode_pass.text?.toString() == view.passcode_pass_confirm.text?.toString()) {
                            if(view.passcode_pass.text?.toString() != null &&
                                    view.passcode_pass.text.toString() != "") {
                                savePasscode(activity, medium, view.passcode_pass.text?.toString())
                                view.passcode_warning.text = "Passcode saved"
                            } else {
                                view.passcode_warning.text = "Enter a passcode to activate protection"
                            }
                        } else {
                            view.passcode_warning.text = "Passcodes do not match. Protection will not be activated"
                        }
                    }

                    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                    }

                    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                    }
                })

            }
            else {
                view.passcode_pass.visibility = View.GONE
                view.passcode_pass_confirm.visibility = View.GONE
                view.passcode_warning.visibility = View.GONE
                view.passcode_warning.text = "Enter a passcode to activate protection"
                view.passcode_pass.setText("")
                view.passcode_pass_confirm.setText("")

                savePasscode(activity, medium, passcode)
            }
        }

        activity.updateTextColors(view.properties_scrollview)

        AlertDialog.Builder(activity)
                .setPositiveButton(R.string.ok, null)
                .setOnDismissListener { callback() }
                .create().apply {
            activity.setupDialogStuff(view, this, R.string.edit)
        }
    }

    private fun savePasscode(activity: Activity, medium: Shortcut, passcode: String?) {
        val token = object : TypeToken<List<Shortcut>>() {}.type
        val shortcuts =  Gson().fromJson<ArrayList<Shortcut>>(activity.config.shortcuts, token) ?: ArrayList<Shortcut>(1)

        shortcuts.forEach {
            if(it.path == medium.path) {
                it.passcode = passcode
            }
        }

        val directories = Gson().toJson(shortcuts)
        activity.config.shortcuts = directories
    }
}
