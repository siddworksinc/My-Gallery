package com.simplemobiletools.gallery.dialogs

import android.app.Activity
import android.content.res.Resources
import android.support.v7.app.AlertDialog
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.simplemobiletools.commons.extensions.getFilenameFromPath
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.commons.extensions.updateTextColors
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.activities.SimpleActivity
import com.simplemobiletools.gallery.extensions.config
import com.simplemobiletools.gallery.models.AlbumCover
import com.simplemobiletools.gallery.models.Directory
import kotlinx.android.synthetic.main.dialog_shortcut.view.*
import java.util.*

class EditShortcutDialog() {
    lateinit var mInflater: LayoutInflater
    lateinit var mResources: Resources

    /**
     * A File Properties dialog constructor with an optional parameter, usable at 1 file selected
     *
     * @param activity request activity to avoid some Theme.AppCompat issues
     * @param path the file path
     * @param countHiddenItems toggle determining if we will count hidden files themselves and their sizes (reasonable only at directory properties)
     */
    constructor(activity: SimpleActivity, dir: Directory, callback: () -> Unit) : this() {
        mInflater = LayoutInflater.from(activity)
        mResources = activity.resources
        val view = mInflater.inflate(R.layout.dialog_shortcut, null)

        val name = dir.name
        val path = dir.path

        view.property_label_name.text = mResources.getString(R.string.name)
        view.property_value_name_value.setText(name)
        view.property_value_name_value.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
                val newName = view.property_value_name_value.text?.toString()
                if(newName != dir.name && newName != null) {
                    var customNamesString = activity.config.customNames
                    val listType = object : TypeToken<HashMap<String, String>>() {}.type
                    val customNames =  Gson().fromJson<HashMap<String, String>>(customNamesString, listType) ?: HashMap(1)
                    customNames.put(dir.path, newName)
                    customNamesString = Gson().toJson(customNames)
                    activity.config.customNames = customNamesString
                }
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }
        })
        view.name_reset.setColorFilter(activity.config.primaryColor)
        view.name_reset.setImageResource(R.drawable.ic_restore)
        view.name_reset.setOnClickListener {
            var customNamesString = activity.config.customNames
            val listType = object : TypeToken<HashMap<String, String>>() {}.type
            val customNames =  Gson().fromJson<HashMap<String, String>>(customNamesString, listType) ?: HashMap(1)
            customNames.remove(dir.path)
            customNamesString = Gson().toJson(customNames)
            activity.config.customNames = customNamesString
            view.property_value_name_value.setText(dir.path.getFilenameFromPath())
        }

        view.property_label_path.text = mResources.getString(R.string.path)
        view.property_value_path_value.text = path

        view.hide_thumbnail.isChecked = dir.isThumbnailHidden
        view.hide_thumbnail_holder.setOnClickListener {
            view.hide_thumbnail.toggle()

            val thumbnailHiddenFolders = activity.config.thumbnailHiddenFolders
            if(thumbnailHiddenFolders.contains(dir.path)) {
                thumbnailHiddenFolders.remove(dir.path)
            } else {
                thumbnailHiddenFolders.add(dir.path)
            }
            activity.config.thumbnailHiddenFolders = thumbnailHiddenFolders
        }

        var albumCovers = activity.config.parseAlbumCovers()
        view.cover_image.isChecked = !albumCovers.filter { it.path == dir.path }.isEmpty()
        view.cover_image_holder.setOnClickListener {
            view.cover_image.toggle()
            if(view.cover_image.isChecked) {
                PickMediumDialog(activity, dir.path) {
                    albumCovers = albumCovers.filterNot { it.path == path } as ArrayList
                    albumCovers.add(AlbumCover(path, it))
                    activity.config.albumCovers = Gson().toJson(albumCovers)
                    view.cover_image.isChecked = true
                }
                view.cover_image.isChecked = false
            } else {
                albumCovers = albumCovers.filterNot { it.path == path } as ArrayList
                activity.config.albumCovers = Gson().toJson(albumCovers)
            }
        }

        view.password_protection.isChecked = dir.passcode!=null
        view.password_protection_holder.setOnClickListener {
            view.password_protection.toggle()
            val password = null;

            if(view.password_protection.isChecked) {
                view.password_pass.visibility = View.VISIBLE
                view.passcode_pass_confirm.visibility = View.VISIBLE
                view.password_warning.visibility = View.VISIBLE
                view.password_warning.text = "Enter a Password to activate protection"

                view.passcode_pass_confirm.addTextChangedListener(object : TextWatcher {
                    override fun afterTextChanged(p0: Editable?) {
                        if(view.password_pass.text?.toString() == view.passcode_pass_confirm.text?.toString()) {
                            if(view.password_pass.text?.toString() != null &&
                                    view.password_pass.text.toString() != "") {
                                savePassword(activity, dir, view.password_pass.text?.toString())
                                view.password_warning.text = "Password saved"
                            } else {
                                view.password_warning.text = "Enter a Password to activate protection"
                            }
                        } else {
                            view.password_warning.text = "Passwords do not match. Protection will not be activated"
                        }
                    }

                    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                    }

                    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                    }
                })

                view.password_pass.addTextChangedListener(object : TextWatcher {
                    override fun afterTextChanged(p0: Editable?) {
                        if(view.password_pass.text?.toString() == view.passcode_pass_confirm.text?.toString()) {
                            if(view.password_pass.text?.toString() != null &&
                                    view.password_pass.text.toString() != "") {
                                savePassword(activity, dir, view.password_pass.text?.toString())
                                view.password_warning.text = "Password saved"
                            } else {
                                view.password_warning.text = "Enter a Password to activate protection"
                            }
                        } else {
                            view.password_warning.text = "Passwords do not match. Protection will not be activated"
                        }
                    }

                    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                    }

                    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                    }
                })
            }
            else {
                view.password_pass.visibility = View.GONE
                view.passcode_pass_confirm.visibility = View.GONE
                view.password_warning.visibility = View.VISIBLE
                view.password_pass.setText("")
                view.passcode_pass_confirm.setText("")
                view.password_warning.text = "Password Protection Disabled"

                savePassword(activity, dir, password)
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

    private fun savePassword(activity: Activity, dir: Directory, password: String?) {
        val passwordsString = activity.config.passwords
        val listType = object : TypeToken<HashMap<String, String>>() {}.type
        val pass =  Gson().fromJson<HashMap<String, String>>(passwordsString, listType) ?: HashMap(1)

        if(pass.keys.contains(dir.path) || password == null) {
            pass.remove(dir.path)
        } else {
            pass.put(dir.path, password)
        }

        val newPasswords = Gson().toJson(pass)
        activity.config.passwords = newPasswords
    }
}
