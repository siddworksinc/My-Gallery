package com.simplemobiletools.gallery.asynctasks

import android.content.Context
import android.os.AsyncTask
import com.simplemobiletools.gallery.extensions.config
import com.simplemobiletools.gallery.extensions.getFilesFrom
import com.simplemobiletools.gallery.models.Directory
import com.simplemobiletools.gallery.models.Medium
import java.util.*

class GetMediaByDirsAsyncTask(val context: Context, val mPath: String, val isPickVideo: Boolean = false,
                              val isPickImage: Boolean = false, val showAll: Boolean, val dirs: ArrayList<Directory>,
                              val callback: (media: ArrayList<Medium>) -> Unit) :
        AsyncTask<Void, Void, ArrayList<Medium>>() {

    override fun doInBackground(vararg params: Void): ArrayList<Medium> {
        if(context.config.temporarilyShowHidden) {
            val path = if (showAll) "" else mPath
            return context.getFilesFrom(path, isPickImage, isPickVideo)
        } else {
            val allMedia = arrayListOf<Medium>()
            dirs.forEach {
                allMedia.addAll(context.getFilesFrom(it.path, isPickImage, isPickVideo))
            }
            Medium.sorting = context.config.getFileSorting(mPath)
            allMedia.sort()
            return allMedia
        }
    }

    override fun onPostExecute(media: ArrayList<Medium>) {
        super.onPostExecute(media)
        callback.invoke(media)
    }
}
