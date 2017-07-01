package com.simplemobiletools.gallery.dialogs

import android.content.Context
import android.content.DialogInterface
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.inputmethod.InputMethodManager
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.commons.extensions.toast
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.models.Directory
import kotlinx.android.synthetic.main.dialog_password.view.*


class PasswordDialog(val activity: AppCompatActivity, val name: Int, val shortcut: Directory, val message: String = activity.getString(R.string.album_password_protected), val callback: (dir: Directory) -> Unit) {

    init {
        val view = LayoutInflater.from(activity).inflate(R.layout.dialog_password, null)
        view.password_message.text = message
        AlertDialog.Builder(activity)
            .setPositiveButton(R.string.ok, DialogInterface.OnClickListener { dialog, which ->
                run {
                    if (view.password_value.text != null &&
                            view.password_value.text.toString() == shortcut.passcode) {
                        callback(shortcut)
                    } else {
                        view.password_value.setText("")
                        activity.toast("Incorrect Password")
                    }
                }
            })
            .create().apply {
                activity.setupDialogStuff(view, this, name)
            }
        view.password_value.postDelayed(
            Runnable {
                view.password_value.requestFocus()
                val inputMethodManager = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.showSoftInput(view.password_value, InputMethodManager.SHOW_IMPLICIT)
            }, 100)
    }

}
