package com.simplemobiletools.gallery.asynctasks

import android.content.Context
import android.os.AsyncTask
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.simplemobiletools.commons.extensions.getFilenameFromPath
import com.simplemobiletools.commons.extensions.hasWriteStoragePermission
import com.simplemobiletools.commons.extensions.internalStoragePath
import com.simplemobiletools.commons.extensions.sdCardPath
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.extensions.config
import com.simplemobiletools.gallery.extensions.containsNoMedia
import com.simplemobiletools.gallery.extensions.getFilesFrom
import com.simplemobiletools.gallery.models.Directory
import com.simplemobiletools.gallery.models.Medium
import java.io.File
import java.util.*
import kotlin.collections.HashMap

class GetDirectoriesAsynctask(val context: Context, val isPickVideo: Boolean, val isPickImage: Boolean,
                              val callback: (dirs: ArrayList<Directory>) -> Unit) : AsyncTask<Void, Void, ArrayList<Directory>>() {
    var config = context.config
    var shouldStop = false
    val showHidden = config.shouldShowHidden
    val temporarilyShowHidden = config.temporarilyShowHidden
    val showHiddenMedia = config.showHiddenMedia
    val dataFolders = config.getDataFolder()

    override fun doInBackground(vararg params: Void): ArrayList<Directory> {
        if (!context.hasWriteStoragePermission())
            return ArrayList()

        val st = System.nanoTime()

        val media = context.getFilesFrom("", isPickImage, isPickVideo)
        val allDirs = groupDirectories(media)
        val filteredDirs = processDirs(allDirs)
        Directory.sorting = config.directorySorting
        filteredDirs.sort()
        val end = System.nanoTime()
        Log.d("Adapter", ((end-st)/1000000).toString());
        return movePinnedToFront(filteredDirs)
    }

    private fun processDirs(dirsAll: Map<String, Directory>): ArrayList<Directory> {
        val thumbnailHiddenFolders = config.thumbnailHiddenFolders
        val passwordsString = config.passwords
        val customNamesString = config.customNames

        val listType = object : TypeToken<HashMap<String, String>>() {}.type
        val pass =  Gson().fromJson<HashMap<String, String>>(passwordsString, listType) ?: HashMap(1)
        val passDirs = pass.keys
        val customNames =  Gson().fromJson<HashMap<String, String>>(customNamesString, listType) ?: HashMap(1)
        val customNameDirs = customNames.keys

        // Filter non-existing dirs
        var dirsExcluded = ArrayList(dirsAll.values.filter { File(it.path).exists() }).filter {
            shouldFolderBeVisible(it.path, config.excludedFolders) } as ArrayList<Directory>

        dirsExcluded.forEach {
            if(thumbnailHiddenFolders.contains(it.path)) { it.isThumbnailHidden = true }
            if(passDirs.contains(it.path)) { it.passcode = pass.get(it.path) }
            if(customNameDirs.contains(it.path)) { it.name = customNames.get(it.path)!! }
        }

        if(!temporarilyShowHidden && !showHiddenMedia && config.passProtectedAlbumsHidden) {
            dirsExcluded = dirsExcluded.filterNot { it.passcode != null } as ArrayList<Directory>
        }

        return dirsExcluded
    }

    private fun groupDirectories(media: ArrayList<Medium>): Map<String, Directory> {
        val albumCovers = config.parseAlbumCovers()
        val hidden = context.resources.getString(R.string.hidden)
        val directories = LinkedHashMap<String, Directory>()
        for ((name, path, isVideo, dateModified, dateTaken, size) in media) {
            if (shouldStop)
                cancel(true)

            val parentDir = File(path).parent ?: continue
            if (directories.containsKey(parentDir)) {
                val directory = directories[parentDir]!!
                val newImageCnt = directory.mediaCnt + 1
                directory.mediaCnt = newImageCnt
                directory.addSize(size)
            } else {
                var dirName = parentDir.getFilenameFromPath()
                if (parentDir == context.internalStoragePath) {
                    dirName = context.getString(R.string.internal)
                } else if (parentDir == context.sdCardPath) {
                    dirName = context.getString(R.string.sd_card)
                }

                if (File(parentDir).containsNoMedia()) {
                    dirName += " $hidden"

                    if (!showHidden)
                        continue
                }

                var thumbnail = path
                albumCovers.forEach {
                    if (it.path == parentDir && File(it.tmb).exists()) {
                        thumbnail = it.tmb
                    }
                }

                val directory = Directory(parentDir, thumbnail, dirName, 1, dateModified, dateTaken, size)
                directories.put(parentDir, directory)
            }
        }
        return directories
    }

    private fun shouldFolderBeVisible(path: String, excludedPaths: MutableSet<String>): Boolean {
        val file = File(path)
        return if (isThisExcluded(path, excludedPaths))
            false
        else if (!showHidden && file.isDirectory && file.canonicalFile == file.absoluteFile) {
            return !file.containsNoMedia() && !path.contains("/.")
        } else {
            true
        }
    }

    private fun checkHasNoMedia(file: File): Boolean {
        return file.containsNoMedia()
    }

    private fun isThisExcluded(path: String, excludedPaths: MutableSet<String>): Boolean {
        return if(excludedPaths.any { path == it }) true
        else {
            var isDataFolderExcluded = false
            dataFolders.forEach {
                if(excludedPaths.contains(it)) {
                    isDataFolderExcluded = true
                }
            }
            if(isDataFolderExcluded) {
                return dataFolders.any { path.startsWith(it) }
            }
            return false
        }
    }

    private fun movePinnedToFront(dirs: ArrayList<Directory>): ArrayList<Directory> {
        val foundFolders = ArrayList<Directory>()
        val pinnedFolders = config.pinnedFolders

        dirs.forEach { if (pinnedFolders.contains(it.path)) foundFolders.add(it) }
        dirs.removeAll(foundFolders)
        dirs.addAll(0, foundFolders)
        return dirs
    }

    override fun onPostExecute(dirs: ArrayList<Directory>) {
        super.onPostExecute(dirs)
        callback.invoke(dirs)
    }
}
