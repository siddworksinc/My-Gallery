package com.simplemobiletools.gallery.helpers

import android.content.Context
import android.content.res.Configuration
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.simplemobiletools.commons.helpers.BaseConfig
import com.simplemobiletools.commons.helpers.SORT_BY_DATE_MODIFIED
import com.simplemobiletools.commons.helpers.SORT_DESCENDING
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.models.AlbumCover
import java.util.*
import kotlin.collections.ArrayList

class Config(context: Context) : BaseConfig(context) {
    companion object {
        fun newInstance(context: Context) = Config(context)
    }

    var fileSorting: Int
        get() = prefs.getInt(SORT_ORDER, SORT_BY_DATE_MODIFIED or SORT_DESCENDING)
        set(order) = prefs.edit().putInt(SORT_ORDER, order).apply()

    var directorySorting: Int
        get() = prefs.getInt(DIRECTORY_SORT_ORDER, SORT_BY_DATE_MODIFIED or SORT_DESCENDING)
        set(order) = prefs.edit().putInt(DIRECTORY_SORT_ORDER, order).apply()

    fun saveFileSorting(path: String, value: Int) {
        if (path.isEmpty()) {
            fileSorting = value
        } else {
            prefs.edit().putInt(SORT_FOLDER_PREFIX + path, value).apply()
        }
    }

    fun getFileSorting(path: String) = prefs.getInt(SORT_FOLDER_PREFIX + path, fileSorting)

    fun removeFileSorting(path: String) {
        prefs.edit().remove(SORT_FOLDER_PREFIX + path).apply()
    }

    fun hasCustomSorting(path: String) = prefs.contains(SORT_FOLDER_PREFIX + path)

    var wasHideFolderTooltipShown: Boolean
        get() = prefs.getBoolean(HIDE_FOLDER_TOOLTIP_SHOWN, false)
        set(wasShown) = prefs.edit().putBoolean(HIDE_FOLDER_TOOLTIP_SHOWN, wasShown).apply()

    var shouldShowHidden = showHiddenMedia || temporarilyShowHidden

    var showHiddenMedia: Boolean
        get() = prefs.getBoolean(SHOW_HIDDEN_MEDIA, false)
        set(showHiddenFolders) = prefs.edit().putBoolean(SHOW_HIDDEN_MEDIA, showHiddenFolders).apply()

    var temporarilyShowHidden: Boolean
        get() = prefs.getBoolean(TEMPORARILY_SHOW_HIDDEN, false)
        set(temporarilyShowHidden) = prefs.edit().putBoolean(TEMPORARILY_SHOW_HIDDEN, temporarilyShowHidden).apply()

    var showAll: Boolean
        get() = prefs.getBoolean(SHOW_ALL, false)
        set(showAll) = prefs.edit().putBoolean(SHOW_ALL, showAll).apply()

    fun addExcludedFolder(path: String) {
        addExcludedFolders(HashSet<String>(Arrays.asList(path)))
    }

    fun addExcludedFolders(paths: Set<String>) {
        val currExcludedFolders = HashSet<String>(excludedFolders)
        currExcludedFolders.addAll(paths)
        excludedFolders = currExcludedFolders
    }

    fun removeExcludedFolder(path: String) {
        val currExcludedFolders = HashSet<String>(excludedFolders)
        currExcludedFolders.remove(path)
        excludedFolders = currExcludedFolders
    }

    var excludedFolders: MutableSet<String>
        get() = prefs.getStringSet(EXCLUDED_FOLDERS, getDataFolder())
        set(excludedFolders) = prefs.edit().remove(EXCLUDED_FOLDERS).putStringSet(EXCLUDED_FOLDERS, excludedFolders).apply()

    var thumbnailHiddenFolders: MutableSet<String>
        get() = prefs.getStringSet(THUMBNAIL_HIDDEN_FOLDERS, getDataFolder())
        set(excludedFolders) = prefs.edit().remove(THUMBNAIL_HIDDEN_FOLDERS).putStringSet(THUMBNAIL_HIDDEN_FOLDERS, excludedFolders).apply()

    fun getDataFolder(): Set<String> {
        val folders = HashSet<String>()
        val dataFolder = context.externalCacheDir?.parentFile?.parent?.trimEnd('/') ?: ""
        if (dataFolder.endsWith("data"))
            folders.add(dataFolder)
        return folders
    }

    fun addIncludedFolder(path: String) {
        val currIncludedFolders = HashSet<String>(includedFolders)
        currIncludedFolders.add(path)
        includedFolders = currIncludedFolders
    }

    fun removeIncludedFolder(path: String) {
        val currIncludedFolders = HashSet<String>(includedFolders)
        currIncludedFolders.remove(path)
        includedFolders = currIncludedFolders
    }

    var includedFolders: MutableSet<String>
        get() = prefs.getStringSet(INCLUDED_FOLDERS, HashSet<String>())
        set(includedFolders) = prefs.edit().remove(INCLUDED_FOLDERS).putStringSet(INCLUDED_FOLDERS, includedFolders).apply()

    fun saveFolderMedia(path: String, json: String) {
        prefs.edit().putString(SAVE_FOLDER_PREFIX + path, json).apply()
    }

    fun loadFolderMedia(path: String) = prefs.getString(SAVE_FOLDER_PREFIX + path, "")

    var autoplayVideos: Boolean
        get() = prefs.getBoolean(AUTOPLAY_VIDEOS, false)
        set(autoplay) = prefs.edit().putBoolean(AUTOPLAY_VIDEOS, autoplay).apply()

    var animateGifs: Boolean
        get() = prefs.getBoolean(ANIMATE_GIFS, false)
        set(animateGifs) = prefs.edit().putBoolean(ANIMATE_GIFS, animateGifs).apply()

    var maxBrightness: Boolean
        get() = prefs.getBoolean(MAX_BRIGHTNESS, false)
        set(maxBrightness) = prefs.edit().putBoolean(MAX_BRIGHTNESS, maxBrightness).apply()

    var cropThumbnails: Boolean
        get() = prefs.getBoolean(CROP_THUMBNAILS, true)
        set(cropThumbnails) = prefs.edit().putBoolean(CROP_THUMBNAILS, cropThumbnails).apply()

    var screenRotation: Int
        get() = prefs.getInt(SCREEN_ROTATION, ROTATE_BY_SYSTEM_SETTING)
        set(screenRotation) = prefs.edit().putInt(SCREEN_ROTATION, screenRotation).apply()

    var loopVideos: Boolean
        get() = prefs.getBoolean(LOOP_VIDEOS, false)
        set(loop) = prefs.edit().putBoolean(LOOP_VIDEOS, loop).apply()

    var displayFileNames: Boolean
        get() = prefs.getBoolean(DISPLAY_FILE_NAMES, false)
        set(display) = prefs.edit().putBoolean(DISPLAY_FILE_NAMES, display).apply()

    var darkBackground: Boolean
        get() = prefs.getBoolean(DARK_BACKGROUND, false)
        set(darkBackground) = prefs.edit().putBoolean(DARK_BACKGROUND, darkBackground).apply()

    var showMedia: Int
        get() = prefs.getInt(SHOW_MEDIA, IMAGES_AND_VIDEOS)
        set(showMedia) = prefs.edit().putInt(SHOW_MEDIA, showMedia).apply()

    var dirColumnCnt: Int
        get() = prefs.getInt(getDirectoryColumnsField(), getDefaultDirectoryColumnCount())
        set(dirColumnCnt) = prefs.edit().putInt(getDirectoryColumnsField(), dirColumnCnt).apply()

    private fun getDirectoryColumnsField(): String {
        val isPortrait = context.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
        return if (isPortrait) {
            if (scrollHorizontally) {
                DIR_HORIZONTAL_COLUMN_CNT
            } else {
                DIR_COLUMN_CNT
            }
        } else {
            if (scrollHorizontally) {
                DIR_LANDSCAPE_HORIZONTAL_COLUMN_CNT
            } else {
                DIR_LANDSCAPE_COLUMN_CNT
            }
        }
    }

    private fun getDefaultDirectoryColumnCount() = context.resources.getInteger(if (scrollHorizontally) R.integer.directory_columns_horizontal_scroll
        else R.integer.directory_columns_vertical_scroll)

    var mediaColumnCnt: Int
        get() = prefs.getInt(getMediaColumnsField(), getDefaultMediaColumnCount())
        set(mediaColumnCnt) = prefs.edit().putInt(getMediaColumnsField(), mediaColumnCnt).apply()

    private fun getMediaColumnsField(): String {
        val isPortrait = context.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
        return if (isPortrait) {
            if (scrollHorizontally) {
                MEDIA_HORIZONTAL_COLUMN_CNT
            } else {
                MEDIA_COLUMN_CNT
            }
        } else {
            if (scrollHorizontally) {
                MEDIA_LANDSCAPE_HORIZONTAL_COLUMN_CNT
            } else {
                MEDIA_LANDSCAPE_COLUMN_CNT
            }
        }
    }

    private fun getDefaultMediaColumnCount() = context.resources.getInteger(if (scrollHorizontally) R.integer.media_columns_horizontal_scroll
        else R.integer.media_columns_vertical_scroll)

    var directories: String
        get() = prefs.getString(DIRECTORIES, "")
        set(directories) = prefs.edit().putString(DIRECTORIES, directories).apply()

    var albumCovers: String
        get() = prefs.getString(ALBUM_COVERS, "")
        set(albumCovers) = prefs.edit().putString(ALBUM_COVERS, albumCovers).apply()

    fun parseAlbumCovers(): ArrayList<AlbumCover> {
        val listType = object : TypeToken<List<AlbumCover>>() {}.type
        return Gson().fromJson<ArrayList<AlbumCover>>(albumCovers, listType) ?: ArrayList(1)
    }

    var shortcuts: String
        get() = prefs.getString(SHORTCUTS, "")
        set(shortcuts) = prefs.edit().putString(SHORTCUTS, shortcuts).apply()

    var passwords: String
        get() = prefs.getString(PASSWORDS, "")
        set(pass) = prefs.edit().putString(PASSWORDS, pass).apply()

    var masterPass: String?
        get() = prefs.getString(MASTER_PASSWORD, null)
        set(pass) = prefs.edit().putString(MASTER_PASSWORD, pass).apply()

    var appLocked: String?
        get() = prefs.getString(APP_LOCKED, null)
        set(pass) = prefs.edit().putString(APP_LOCKED, pass).apply()

    var tutorialCompleted: Boolean
        get() = prefs.getBoolean(TUTORIAL_COMPLETED, false)
        set(completed) = prefs.edit().putBoolean(TUTORIAL_COMPLETED, completed).apply()

    var passProtectedAlbumsHidden: Boolean
        get() = prefs.getBoolean(PASS_PRO_ALBUMS_HIDDEN, false)
        set(hidden) = prefs.edit().putBoolean(PASS_PRO_ALBUMS_HIDDEN, hidden).apply()

    var customNames: String
        get() = prefs.getString(CUSTOM_NAMES, "")
        set(names) = prefs.edit().putString(CUSTOM_NAMES, names).apply()

    var showAllDirectories: String
        get() = prefs.getString(SHOW_ALL_DIRECTORIES, "")
        set(directories) = prefs.edit().putString(SHOW_ALL_DIRECTORIES, directories).apply()

    var scrollHorizontally: Boolean
        get() = prefs.getBoolean(SCROLL_HORIZONTALLY, false)
        set(scrollHorizontally) = prefs.edit().putBoolean(SCROLL_HORIZONTALLY, scrollHorizontally).apply()

    var hideSystemUI: Boolean
        get() = prefs.getBoolean(HIDE_SYSTEM_UI, false)
        set(hideSystemUI) = prefs.edit().putBoolean(HIDE_SYSTEM_UI, hideSystemUI).apply()

    var orderedAlbums: String?
        get() = prefs.getString(ORDERED_ALBUMS, null)
        set(sortedFolders) = prefs.edit().remove(ORDERED_ALBUMS).putString(ORDERED_ALBUMS, sortedFolders).apply()

    fun removeOrderedAlbum(path: String) {
        val currSortedFolders = parseOrderedAlbums()
        currSortedFolders.remove(path)
        orderedAlbums = Gson().toJson(currSortedFolders)
    }

    fun parseOrderedAlbums(): ArrayList<String> {
        val listType = object : TypeToken<ArrayList<String>>() {}.type
        return Gson().fromJson<ArrayList<String>>(orderedAlbums, listType) ?: ArrayList(1)
    }

    var mediaColumnCounts: String?
        get() = prefs.getString(getMediaColumnsField(), null)
        set(mediaColumnCount) = prefs.edit().remove(getMediaColumnsField()).putString(getMediaColumnsField(), mediaColumnCount).apply()

    fun getColumnCount(mPath: String): Int {
        val listType = object : TypeToken<HashMap<String, Int>>() {}.type
        val hashMap = Gson().fromJson<HashMap<String, Int>>(mediaColumnCounts, listType) ?: HashMap<String, Int>(1)
        if(hashMap.containsKey(mPath)) {
            return hashMap[mPath]!!
        } else {
            return getDefaultMediaColumnCount()
        }
    }

    fun setColumnCount(mPath: String, count: Int) {
        val listType = object : TypeToken<HashMap<String, Int>>() {}.type
        val hashMap = Gson().fromJson<HashMap<String, Int>>(mediaColumnCounts, listType) ?: HashMap<String, Int>(1)
        hashMap.put(mPath, count)
        mediaColumnCounts = Gson().toJson(hashMap)
    }

    var slideshowInterval: Int
        get() = prefs.getInt(SLIDESHOW_INTERVAL, SLIDESHOW_DEFAULT_INTERVAL)
        set(slideshowInterval) = prefs.edit().putInt(SLIDESHOW_INTERVAL, slideshowInterval).apply()

    var slideshowIncludePhotos: Boolean
        get() = prefs.getBoolean(SLIDESHOW_INCLUDE_PHOTOS, true)
        set(slideshowIncludePhotos) = prefs.edit().putBoolean(SLIDESHOW_INCLUDE_PHOTOS, slideshowIncludePhotos).apply()

    var slideshowIncludeVideos: Boolean
        get() = prefs.getBoolean(SLIDESHOW_INCLUDE_VIDEOS, false)
        set(slideshowIncludeVideos) = prefs.edit().putBoolean(SLIDESHOW_INCLUDE_VIDEOS, slideshowIncludeVideos).apply()

    var slideshowRandomOrder: Boolean
        get() = prefs.getBoolean(SLIDESHOW_RANDOM_ORDER, false)
        set(slideshowRandomOrder) = prefs.edit().putBoolean(SLIDESHOW_RANDOM_ORDER, slideshowRandomOrder).apply()

    var slideshowUseFade: Boolean
        get() = prefs.getBoolean(SLIDESHOW_USE_FADE, false)
        set(slideshowUseFade) = prefs.edit().putBoolean(SLIDESHOW_USE_FADE, slideshowUseFade).apply()

    var slideshowMoveBackwards: Boolean
        get() = prefs.getBoolean(SLIDESHOW_MOVE_BACKWARDS, false)
        set(slideshowMoveBackwards) = prefs.edit().putBoolean(SLIDESHOW_MOVE_BACKWARDS, slideshowMoveBackwards).apply()

    var lastMedia: String?
        get() = prefs.getString(LAST_MEDIA, null)
        set(lastMedia) = prefs.edit().remove(LAST_MEDIA).putString(LAST_MEDIA, lastMedia).apply()

    var recentMediaEnabled: Boolean
        get() = prefs.getBoolean(RECENT_MEDIA_ENABLED, false)
        set(recentMediaEnabled) = prefs.edit().putBoolean(RECENT_MEDIA_ENABLED, recentMediaEnabled).apply()
}

