package com.simplemobiletools.gallery.dialogs

import android.app.Activity
import android.content.res.Resources
import android.support.v7.app.AlertDialog
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.commons.extensions.updateTextColors
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.activities.SimpleActivity
import com.simplemobiletools.gallery.extensions.config
import kotlinx.android.synthetic.main.dialog_shortcut.view.*

class MasterPassDialog() {
    lateinit var mInflater: LayoutInflater
    lateinit var mResources: Resources

    /**
     * A File Properties dialog constructor with an optional parameter, usable at 1 file selected
     *
     * @param activity request activity to avoid some Theme.AppCompat issues
     * @param path the file path
     * @param countHiddenItems toggle determining if we will count hidden files themselves and their sizes (reasonable only at directory properties)
     */
    constructor(activity: SimpleActivity, callback: () -> Unit) : this() {
        mInflater = LayoutInflater.from(activity)
        mResources = activity.resources
        val view = mInflater.inflate(R.layout.dialog_shortcut, null)

        view.property_label_name.visibility = View.GONE
        view.property_value_name_value.visibility = View.GONE
        view.property_label_path.visibility = View.GONE
        view.property_value_path_value.visibility = View.GONE

        view.hide_thumbnail_holder.visibility = View.GONE
        view.cover_image_holder.visibility = View.GONE

        view.password_protection.text = "Enable Master Password"
        view.password_protection.visibility = View.VISIBLE
        view.password_protection.isChecked = activity.config.masterPass != null
        view.passcode_protection_holder.setOnClickListener {
            view.password_protection.toggle()
            val password = null;

            if(view.password_protection.isChecked) {
                view.password_pass.visibility = View.VISIBLE
                view.passcode_pass_confirm.visibility = View.VISIBLE
                view.password_warning.visibility = View.VISIBLE
                view.password_warning.text = "Enter a Password to Enable Master Password"

                view.passcode_pass_confirm.addTextChangedListener(object : TextWatcher {
                    override fun afterTextChanged(p0: Editable?) {
                        if(view.password_pass.text?.toString() == view.passcode_pass_confirm.text?.toString()) {
                            if(view.password_pass.text?.toString() != null &&
                                    view.password_pass.text.toString() != "") {
                                savePassword(activity, view.password_pass.text?.toString())
                                view.password_warning.text = "Password saved"
                            } else {
                                view.password_warning.text = "Enter a Password to Enable Master Password"
                            }
                        } else {
                            view.password_warning.text = "Passwords do not match. Master Password will not be Enabled"
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
                                savePassword(activity, view.password_pass.text?.toString())
                                view.password_warning.text = "Password saved"
                            } else {
                                view.password_warning.text = "Enter a Password to Enable Master Password"
                            }
                        } else {
                            view.password_warning.text = "Passwords do not match. Master Password will not be Enabled"
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
                view.password_warning.text = "Master Password Disabled"

                savePassword(activity, password)
            }
        }

        activity.updateTextColors(view.properties_scrollview)

        AlertDialog.Builder(activity)
                .setPositiveButton(R.string.ok, null)
                .setOnDismissListener { callback() }
                .create().apply {
            activity.setupDialogStuff(view, this, R.string.master_password)
        }
    }

    private fun savePassword(activity: Activity, password: String?) {
        activity.config.masterPass = password
    }
}
