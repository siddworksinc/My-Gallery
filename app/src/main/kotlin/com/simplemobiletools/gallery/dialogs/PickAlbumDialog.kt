package com.simplemobiletools.gallery.dialogs

import android.os.Environment
import android.support.v7.app.AlertDialog
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import com.google.gson.Gson
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.commons.extensions.toast
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.activities.SimpleActivity
import com.simplemobiletools.gallery.adapters.DirectoryAdapter
import com.simplemobiletools.gallery.asynctasks.GetDirectoriesAsynctask
import com.simplemobiletools.gallery.extensions.config
import com.simplemobiletools.gallery.extensions.getCachedDirectories
import com.simplemobiletools.gallery.extensions.getFilesFrom
import com.simplemobiletools.gallery.models.Directory
import kotlinx.android.synthetic.main.dialog_directory_picker.view.*
import java.io.File

class PickAlbumDialog(val activity: SimpleActivity, val sourcePath: String?,
                      var filterDirs: ArrayList<String>?, val callback: (dir: Directory) -> Unit) {
    var dialog: AlertDialog
    var directoriesGrid: RecyclerView
    var shownDirectories: ArrayList<Directory> = ArrayList()
    val view = LayoutInflater.from(activity).inflate(R.layout.dialog_directory_picker, null)

    init {
        if(filterDirs == null) {filterDirs = ArrayList<String>()}
        directoriesGrid = view.directories_grid
        view.directory_message.visibility = View.VISIBLE
        view.directory_message.text = "Loading Albums..."

        dialog = AlertDialog.Builder(activity)
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancel, null)
                .setNeutralButton(R.string.other_folder, { dialogInterface, i -> showOtherFolder() })
                .create().apply {
            activity.setupDialogStuff(view, this, R.string.select_album)

            val dirs = activity.getCachedDirectories()
            if (dirs.isNotEmpty()) {
                gotDirectories(dirs, view, true)
            }

            GetDirectoriesAsynctask(activity, false, false) {
                gotDirectories(it.dirs, view, false)
            }.execute()
        }
    }

    fun showOtherFolder() {
        val showHidden = activity.config.shouldShowHidden
        FilePickerDialog(activity, Environment.getExternalStorageDirectory().getPath(), false, showHidden, true) {
            val file = File(it)
            var path : String = ""
            val allMedia = activity.getFilesFrom(file.path, false , false)
            if(!allMedia.isEmpty()) {
                val first = allMedia.first()
                path = first.path
            }
            val dir = Directory(it, path, file.name, 1, file.lastModified(), 0, 0)
            callback(dir)
        }
    }

    private fun gotDirectories(directories: ArrayList<Directory>, view: View, isFromCache: Boolean) {
        if (directories.hashCode() == shownDirectories.hashCode())
            return

        val filteredAllDirs = directories.filter { !filterDirs!!.contains(it.path) } as ArrayList<Directory>
        if(!filteredAllDirs.isEmpty()) {
            view.directory_message.visibility = View.GONE
            view.directory_message.text = ""
        } else if(filteredAllDirs.isEmpty() && !isFromCache) {
            view.directory_message.visibility = View.VISIBLE
            view.directory_message.text = "No More Albums To Show!\n(To browse and select album,\nuse OTHER FOLDER option)"
        }

        shownDirectories = filteredAllDirs
        directoriesGrid.adapter = DirectoryAdapter(activity, filteredAllDirs, null, true) {
            if (sourcePath != null && it.path.trimEnd('/') == sourcePath) {
                activity.toast(R.string.source_and_destination_same)
                return@DirectoryAdapter
            } else {
                callback(it)
                dialog.dismiss()
            }
        }

        if(!isFromCache) {
            val dirsJson = Gson().toJson(directories)
            activity.config.directories = dirsJson
        }
    }
}
