package com.simplemobiletools.gallery.asynctasks

import android.content.Context
import android.os.AsyncTask
import com.simplemobiletools.commons.extensions.hasWriteStoragePermission
import com.simplemobiletools.gallery.extensions.config
import com.simplemobiletools.gallery.extensions.getFilesFrom
import com.simplemobiletools.gallery.models.Shortcut

class RefreshShortcutsAsynctask(val context: Context, val shortcuts: ArrayList<Shortcut>,
                                val callback: (dirs: ArrayList<Shortcut>?) -> Unit) : AsyncTask<Void, Void, ArrayList<Shortcut>>() {
    var config = context.config
    var shouldStop = false
    val showHidden = config.shouldShowHidden

    override fun doInBackground(vararg params: Void): ArrayList<Shortcut>? {
        if (!context.hasWriteStoragePermission())
            return ArrayList()
        var isChanged = false;
        val newShortcuts = ArrayList<Shortcut>()

        shortcuts.forEach {
            if(!it.isThumbnailHidden && it.coverImage == null) {
                val allMedia = context.getFilesFrom(it.path, false, false)
                if(!allMedia.isEmpty()) {
                    val first = allMedia.first()
                    if(it.tmb != first.path) {
                        val shortcut = Shortcut(it.path, first.path, it.name, 1, it.modified, 0, 0)
                        newShortcuts.add(shortcut)
                        isChanged = true;
                    } else {
                        newShortcuts.add(it)
                    }
                }
            }
        }
        if(isChanged) { return newShortcuts }
        else { return null }
    }

    override fun onPostExecute(dirs: ArrayList<Shortcut>?) {
        super.onPostExecute(dirs)
        callback.invoke(dirs)
    }
}
