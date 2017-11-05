package com.simplemobiletools.gallery.asynctasks

import android.os.AsyncTask
import com.simplemobiletools.gallery.models.Directory
import com.simplemobiletools.gallery.models.Medium
import java.io.File
import java.util.*

class GetRecentMediaAsyncTask(var media: ArrayList<Medium>, var dirs: ArrayList<Directory>,
                              val callback: (media: ArrayList<Medium>?) -> Unit) :
        AsyncTask<Void, Void, ArrayList<Medium>?>() {

    override fun doInBackground(vararg params: Void): ArrayList<Medium>? {
        val recentMedia = ArrayList<Medium>()
        for(medium in media) {
            val parentDir = File(medium.path).parent ?: continue
            if(dirs.any { it.path == parentDir && it.passcode == null }) {
                recentMedia.add(medium)
            }
        }
        return recentMedia
    }

    override fun onPostExecute(media: ArrayList<Medium>?) {
        super.onPostExecute(media)
        callback.invoke(media)
    }
}
