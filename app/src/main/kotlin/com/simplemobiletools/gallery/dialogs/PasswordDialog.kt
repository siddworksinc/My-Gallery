package com.simplemobiletools.gallery.dialogs

import android.content.DialogInterface
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.commons.extensions.toast
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.activities.SimpleActivity
import com.simplemobiletools.gallery.models.Shortcut
import kotlinx.android.synthetic.main.dialog_password.view.*

class PasswordDialog(val activity: SimpleActivity, val name: Int, val shortcut: Shortcut, val callback: (dir: Shortcut) -> Unit) {

    init {
        val view = LayoutInflater.from(activity).inflate(R.layout.dialog_password, null)
        AlertDialog.Builder(activity)
                .setPositiveButton(R.string.ok, DialogInterface.OnClickListener { dialog, which ->
                    run {
                        if (view.password_value.text != null &&
                                view.password_value.text.toString() == shortcut.passcode) {
                            callback(shortcut)
                        } else {
                            activity.toast("Incorrect Password")
                        }
                    }
                })
                .create().apply {
            activity.setupDialogStuff(view, this, name)
        }
    }

}
