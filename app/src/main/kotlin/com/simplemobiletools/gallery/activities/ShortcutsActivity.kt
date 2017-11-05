package com.simplemobiletools.gallery.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.support.design.widget.AppBarLayout
import android.support.design.widget.CoordinatorLayout
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.graphics.ColorUtils
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.Toolbar
import android.text.Html
import android.view.*
import com.afollestad.materialdialogs.MaterialDialog
import com.google.gson.Gson
import com.mikepenz.materialdrawer.AccountHeaderBuilder
import com.mikepenz.materialdrawer.Drawer
import com.mikepenz.materialdrawer.DrawerBuilder
import com.mikepenz.materialdrawer.model.DividerDrawerItem
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem
import com.mikepenz.materialdrawer.model.SectionDrawerItem
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem
import com.siddworks.android.mygallery.AboutActivity
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.views.MyScalableRecyclerView
import com.simplemobiletools.gallery.BuildConfig
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.adapters.MediaAdapter
import com.simplemobiletools.gallery.adapters.ShortcutsAdapter
import com.simplemobiletools.gallery.asynctasks.GetDirectoriesAsynctask
import com.simplemobiletools.gallery.asynctasks.GetRecentMediaAsyncTask
import com.simplemobiletools.gallery.dialogs.AlbumSortingDialog
import com.simplemobiletools.gallery.dialogs.PasswordDialog
import com.simplemobiletools.gallery.extensions.*
import com.simplemobiletools.gallery.helpers.*
import com.simplemobiletools.gallery.models.Directory
import com.simplemobiletools.gallery.models.Medium
import kotlinx.android.synthetic.main.activity_shortcuts.*
import kotlinx.android.synthetic.main.drawer_header.view.*
import java.io.File
import java.util.*
import kotlin.collections.ArrayList

class ShortcutsActivity : SimpleActivity(), ShortcutsAdapter.DirOperationsListener, MediaAdapter.MediaOperationsListener {
    override fun deleteFiles(files: ArrayList<File>) {

    }

    private val STORAGE_PERMISSION = 1
    private val PICK_MEDIA = 2
    private val LAST_MEDIA_CHECK_PERIOD = 3000L

    lateinit var mDirs: ArrayList<Directory>
    private var mIsGettingDirs = false
    private var mLoadedInitialPhotos = false
    private var mStoredAnimateGifs = true
    private var mStoredCropThumbnails = true
    private var mStoredScrollHorizontally = true
    private var mLastMediaModified = 0
    private var mLastMediaHandler = Handler()
    private lateinit var recentMedia: ArrayList<Medium>

    private var mCurrAsyncTask: GetDirectoriesAsynctask? = null
    private var drawer: Drawer? = null
    private var toolbar: Toolbar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shortcuts)
        logEvent("ActivityShortcuts")
        config.temporarilyShowHidden = false
        config.showAll = false

        toolbar = findViewById(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)
        setupDrawer()

        mDirs = ArrayList<Directory>()
        mStoredAnimateGifs = config.animateGifs
        mStoredCropThumbnails = config.cropThumbnails
        mStoredScrollHorizontally = config.scrollHorizontally

        initLocal()
        initRecentMedia()
    }

    override fun onResume() {
        super.onResume()
        if (mStoredAnimateGifs != config.animateGifs) {
            directories_grid.adapter?.notifyDataSetChanged()
        }

        if (mStoredCropThumbnails != config.cropThumbnails) {
            directories_grid.adapter?.notifyDataSetChanged()
        }

        if (mStoredScrollHorizontally != config.scrollHorizontally) {
            directories_grid.adapter?.let {
                (it as ShortcutsAdapter).scrollVertically = !config.scrollHorizontally
                it.notifyDataSetChanged()
            }
            setupScrollDirection()
        }

        tryLoadGallery()
        invalidateOptionsMenu()

        // set bar color if selected
        val shortcutsAdapter = directories_grid.adapter as ShortcutsAdapter?
        val selected = shortcutsAdapter?.actMode
        if(selected != null) {
            updateStatusBarColor(R.color.black)
        }

        recent_fab.beVisibleIf(config.recentMediaEnabled)
    }

    override fun onPause() {
        super.onPause()
        mCurrAsyncTask?.shouldStop = true
        storeDirectories(mDirs)
        directories_refresh_layout.isRefreshing = false
        mIsGettingDirs = false
        mStoredAnimateGifs = config.animateGifs
        mStoredCropThumbnails = config.cropThumbnails
        mStoredScrollHorizontally = config.scrollHorizontally
        directories_grid.listener = null
        mLastMediaHandler.removeCallbacksAndMessages(null)
    }

    override fun onDestroy() {
        super.onDestroy()
        config.temporarilyShowHidden = false
    }

    private fun tryLoadGallery() {
        if (hasWriteStoragePermission()) {
            getDirectories()
            setupLayoutManager()
            checkIfColorChanged()
        } else {
            MaterialDialog.Builder(this@ShortcutsActivity)
                    .title("Permission Required")
                    .contentColor(R.color.black)
                    .titleColor(R.color.black)
                    .content("My Gallery is a Gallery app. To show Photos & Videos, " +
                            "it needs access to your device's storage. " +
                            "(${getString(R.string.no_storage_permissions)})")
                    .positiveText("Grant")
                    .onPositive { _, _ ->
                        run {
                            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), STORAGE_PERMISSION)
                        }
                    }
                    .negativeText("Close")
                    .onNegative { _, _ -> finish() }
                    .positiveColor(ContextCompat.getColor(this@ShortcutsActivity, R.color.colorPrimary))
                    .show()
        }
    }

    private fun getDirectories() {
        if (mIsGettingDirs)
            return

        mIsGettingDirs = true
        val dirs = getCachedDirectories()
        if (dirs.isNotEmpty() && !mLoadedInitialPhotos) {
            gotDirectories(dirs)
        }

        if (!mLoadedInitialPhotos) {
            directories_refresh_layout.isRefreshing = true
            directories_refresh_layout.isEnabled = true
        }

        mLoadedInitialPhotos = true
        mCurrAsyncTask = GetDirectoriesAsynctask(applicationContext, false, false) {
            gotDirectories(it.dirs)
            GetRecentMediaAsyncTask(it.media, it.dirs) {
                recentMedia = it ?: arrayListOf()
                updateRecentMediaIfVisible()
            }.execute()
        }
        mCurrAsyncTask!!.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    private fun gotDirectories(dirs: ArrayList<Directory>) {
        supportActionBar?.subtitle = Html.fromHtml("<small>${dirs.size} Albums</small>")
        mLastMediaModified = getLastMediaModified()
        directories_refresh_layout.isRefreshing = false
        directories_refresh_layout.isEnabled = false
        mIsGettingDirs = false

        checkLastMediaChanged()
        if (dirs.hashCode() == mDirs.hashCode())
            return

        mDirs = dirs
        runOnUiThread {
            setupAdapter()
        }
        storeDirectories(mDirs)
    }

    private fun checkLastMediaChanged() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && isDestroyed)
            return

        mLastMediaHandler.removeCallbacksAndMessages(null)
        mLastMediaHandler.postDelayed({
            Thread({
                val lastModified = getLastMediaModified()
                if (mLastMediaModified != lastModified) {
                    mLastMediaModified = lastModified
                    runOnUiThread {
                        getDirectories()
                    }
                } else {
                    checkLastMediaChanged()
                }
            }).start()
        }, LAST_MEDIA_CHECK_PERIOD)
    }

    private fun storeDirectories(mDirs: ArrayList<Directory>) {
        if (!config.temporarilyShowHidden) {
            val directories = Gson().toJson(mDirs)
            config.directories = directories
        }
    }

    private fun checkIfColorChanged() {
        if (directories_grid.adapter != null && getRecyclerAdapter().foregroundColor != config.primaryColor) {
            getRecyclerAdapter().updatePrimaryColor(config.primaryColor)
            directories_vertical_fastscroller.updateHandleColor()
            directories_horizontal_fastscroller.updateHandleColor()
            setupDrawer()
            updateBackgroundColor()
            updateActionbarColor()
            updateRecentsColor()
        }
    }

    private fun getRecyclerAdapter() = (directories_grid.adapter as ShortcutsAdapter)

    private fun setupLayoutManager() {
        val layoutManager = directories_grid.layoutManager as GridLayoutManager
        if (config.scrollHorizontally) {
            layoutManager.orientation = GridLayoutManager.HORIZONTAL
            val layoutParams = CoordinatorLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT)
            layoutParams.behavior = AppBarLayout.ScrollingViewBehavior()
            directories_refresh_layout.layoutParams = layoutParams
        } else {
            layoutManager.orientation = GridLayoutManager.VERTICAL
            val layoutParams = CoordinatorLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            layoutParams.behavior = AppBarLayout.ScrollingViewBehavior()
            directories_refresh_layout.layoutParams = layoutParams
        }

        directories_grid.isDragSelectionEnabled = true
        directories_grid.isZoomingEnabled = true
        layoutManager.spanCount = config.dirColumnCnt
        directories_grid.listener = object : MyScalableRecyclerView.MyScalableRecyclerViewListener {
            override fun zoomIn() {
                if (layoutManager.spanCount > 1) {
                    reduceColumnCount()
                    getRecyclerAdapter().actMode?.finish()
                }
            }

            override fun zoomOut() {
                if (layoutManager.spanCount < 10) {
                    increaseColumnCount()
                    getRecyclerAdapter().actMode?.finish()
                }
            }

            override fun selectItem(position: Int) {
                getRecyclerAdapter().selectItem(position)
            }

            override fun selectRange(initialSelection: Int, lastDraggedIndex: Int, minReached: Int, maxReached: Int) {
                getRecyclerAdapter().selectRange(initialSelection, lastDraggedIndex, minReached, maxReached)
            }
        }
    }

    private fun itemClicked(directory: Directory) {
        if(directory.passcode != null) {
            PasswordDialog(this, R.string.open, directory) {
                Intent(this, MediaActivity::class.java).apply {
                    putExtra(DIRECTORY, directory.path)
                    putExtra(GET_IMAGE_INTENT, false)
                    putExtra(GET_VIDEO_INTENT, false)
                    putExtra(GET_ANY_INTENT, false)
                    startActivityForResult(this, PICK_MEDIA)
                }
            }
        } else {
            Intent(this, MediaActivity::class.java).apply {
                putExtra(DIRECTORY, directory.path)
                putExtra(GET_IMAGE_INTENT, false)
                putExtra(GET_VIDEO_INTENT, false)
                putExtra(GET_ANY_INTENT, false)
                startActivityForResult(this, PICK_MEDIA)
            }
        }
    }

    private fun setupAdapter() {
        var currAdapter = directories_grid.adapter
        if (currAdapter != null) {
            (currAdapter as ShortcutsAdapter).updateDirs(mDirs)
        } else {
            directories_grid.adapter = ShortcutsAdapter(this, mDirs, this) {
                itemClicked(it)
            }
        }
        setupScrollDirection()
    }

    private fun setupScrollDirection() {
        directories_vertical_fastscroller.isHorizontal = false
        directories_vertical_fastscroller.beGoneIf(config.scrollHorizontally)

        directories_horizontal_fastscroller.isHorizontal = true
        directories_horizontal_fastscroller.beVisibleIf(config.scrollHorizontally)

        if (config.scrollHorizontally) {
            directories_horizontal_fastscroller.setViews(directories_grid, directories_refresh_layout)
        } else {
            directories_vertical_fastscroller.setViews(directories_grid, directories_refresh_layout)
        }
    }

    override fun refreshItems() {
        getDirectories()
    }

    override fun itemLongClicked(position: Int) {
        directories_grid.setDragSelectActive(position)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == STORAGE_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getDirectories()
            } else {
                toast(R.string.no_storage_permissions)
                finish()
            }
        }
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_shortcuts, menu)
        menu.findItem(R.id.increase_column_count).isVisible = config.dirColumnCnt < 10
        menu.findItem(R.id.reduce_column_count).isVisible = config.dirColumnCnt > 1
        menu.findItem(R.id.temporarily_show_hidden).isVisible = !config.showHiddenMedia
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        super.onPrepareOptionsMenu(menu)
        menu?.findItem(R.id.temporarily_show_hidden)?.isVisible = !config.temporarilyShowHidden && !config.showHiddenMedia
        menu?.findItem(R.id.hide_hidden)?.isVisible = config.temporarilyShowHidden && !config.showHiddenMedia
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.sort -> showSortingDialog()
            R.id.open_camera -> launchCamera()
            R.id.temporarily_show_hidden -> temporarilyShowHidden()
            R.id.hide_hidden -> hideHidden()
            R.id.increase_column_count -> increaseColumnCount()
            R.id.reduce_column_count -> reduceColumnCount()
            R.id.show_all -> showAllMedia()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun showAllMedia() {
        val directories = Gson().toJson(mDirs)
        config.showAllDirectories = directories
        Intent(this, ShowAllMediaActivity::class.java).apply {
            putExtra(DIRECTORY, "/")
            startActivity(this)
        }
    }

    private fun hideHidden() {
        config.temporarilyShowHidden = false
        getDirectories()
        invalidateOptionsMenu()
    }

    private fun temporarilyShowHidden() {
        if(config.masterPass != null) {
            PasswordDialog(this, R.string.show_hidden, Directory("", "", "", 0, 0, 0, 0, false, config.masterPass), "Enter master password to continue") {
                config.temporarilyShowHidden = true
                getDirectories()
            }
        } else {
            config.temporarilyShowHidden = true
            getDirectories()
        }
        invalidateOptionsMenu()
    }

    private fun increaseColumnCount() {
        config.dirColumnCnt = ++(directories_grid.layoutManager as GridLayoutManager).spanCount
        invalidateOptionsMenu()
        directories_grid.adapter?.notifyDataSetChanged()
    }

    private fun reduceColumnCount() {
        config.dirColumnCnt = --(directories_grid.layoutManager as GridLayoutManager).spanCount
        invalidateOptionsMenu()
        directories_grid.adapter?.notifyDataSetChanged()
    }

    private fun showSortingDialog() {
        AlbumSortingDialog(this, true, false) {
            getDirectories()
        }
    }

    override fun tryDeleteFolders(folders: ArrayList<File>) {
        for (file in folders) {
            deleteFolders(folders) {
                runOnUiThread {
                    refreshItems()
                }
            }
        }
    }

    private fun setupDrawer() {
        var whatsNew : PrimaryDrawerItem? = null
        var whatsNewHeader : DividerDrawerItem? = null
        val whatsNewContent = checkWhatsNew(this@ShortcutsActivity)

        val gallery = PrimaryDrawerItem().withIdentifier(1).withName(R.string.gallery)
                .withIcon(R.drawable.ic_image_black_24dp).withIconTintingEnabled(true)
        val videos = PrimaryDrawerItem().withIdentifier(8).withName(R.string.videos)
                .withIcon(R.drawable.ic_theaters_white_24dp).withIconTintingEnabled(true)
        val settings = PrimaryDrawerItem().withIdentifier(2).withName(R.string.settings)
                .withIcon(R.drawable.ic_settings_black_24dp).withIconTintingEnabled(true)
        val about = PrimaryDrawerItem().withIdentifier(3).withName(R.string.about)
                .withIcon(R.drawable.ic_info_black_24dp).withIconTintingEnabled(true)

        if(whatsNewContent != null) {
            whatsNewHeader = DividerDrawerItem()

            whatsNew = PrimaryDrawerItem().withIdentifier(7).withName(R.string.whats_new)
                    .withIcon(R.drawable.ic_whatshot_white_24dp).withIconTintingEnabled(true)
        }
        val amazingUser = SectionDrawerItem().withName("Be An Amazing User :)").withDivider(true)

        val share = PrimaryDrawerItem().withIdentifier(4).withName(R.string.share)
                .withIcon(R.drawable.ic_share_black_24dp).withIconTintingEnabled(true)
        val sendFeedback = PrimaryDrawerItem().withIdentifier(5).withName(R.string.send_feedback)
                .withIcon(R.drawable.ic_send_white_24dp).withIconTintingEnabled(true)
        val rateUs = PrimaryDrawerItem().withIdentifier(6).withName(R.string.rate_us)
                .withIcon(R.drawable.ic_thumb_up_white_24dp).withIconTintingEnabled(true)

        // Remaining
        // Donate/Contribute
        // tips & tutorials
        val idToMap = HashMap<Int, String>()
        idToMap.put(1, "Gallery")
        idToMap.put(2, "Settings")
        idToMap.put(3, "About")
        idToMap.put(4, "Share")
        idToMap.put(5, "Send Feedback")
        idToMap.put(6, "Rate Us")
        idToMap.put(7, "Whats New")
        idToMap.put(8, "Videos")

        val customPrimaryColor = baseConfig.customPrimaryColor
        val view = LayoutInflater.from(this).inflate(R.layout.drawer_header, null)
        when (baseConfig.appRunCount % 3) {
            0 -> { view.material_drawer_account_header_background.setImageDrawable(
                    ContextCompat.getDrawable(this, R.drawable.header))
                    val primaryColorAlpha = ColorUtils.setAlphaComponent(customPrimaryColor, 140)
                    view.material_drawer_account_header_background.setColorFilter(primaryColorAlpha); }
            1 -> { view.material_drawer_account_header_background.setImageDrawable(
                    ContextCompat.getDrawable(this, R.drawable.header2))
                    val primaryColorAlpha = ColorUtils.setAlphaComponent(customPrimaryColor, 100)
                    view.material_drawer_account_header_background.setColorFilter(primaryColorAlpha); }
            2 -> { view.material_drawer_account_header_background.setImageDrawable(
                    ContextCompat.getDrawable(this, R.drawable.header3))
                    val primaryColorAlpha = ColorUtils.setAlphaComponent(customPrimaryColor, 160)
                    view.material_drawer_account_header_background.setColorFilter(primaryColorAlpha); }
        }

        view.app_version.text = "v${BuildConfig.VERSION_NAME}"
        var defaultSelection = 1L
        if (config.showMedia == VIDEOS) {defaultSelection = 8L}

        val header = AccountHeaderBuilder()
                .withActivity(this)
                .withAccountHeader(view)
                .build()

        val drawerBuilder = DrawerBuilder()
                .withActivity(this)
                .withToolbar(toolbar as Toolbar)
                .withAccountHeader(header)
                .withScrollToTopAfterClick(true)
                .withSelectedItem(defaultSelection)
                .withOnDrawerItemClickListener(object : Drawer.OnDrawerItemClickListener {
                    override fun onItemClick(view1: View, position: Int, drawerItem: IDrawerItem<*, *>): Boolean {
                        logEvent("Drawer" + idToMap.getValue(drawerItem.identifier.toInt()))
                        when (drawerItem.identifier) {
                            1L -> {
                                    config.showMedia = IMAGES_AND_VIDEOS
                                    tryLoadGallery()
                            }
                            2L -> {
                                    launchSettings()
                                    resetSelection()
                            }
                            3L -> {
                                    launchAbout()
                                    resetSelection()
                            }
                            4L -> {
                                    shareApp(this@ShortcutsActivity)
                                    resetSelection()
                            }
                            5L -> {
                                    showContactDeveloper(this@ShortcutsActivity)
                                    resetSelection()
                            }
                            6L -> {
                                    openUrl(this@ShortcutsActivity, "https://play.google.com/store/apps/details?id=com.siddworks.mygallery")
                                    resetSelection()
                            }
                            7L -> {
                                    if (whatsNewContent != null) {
                                        showWhatsNewDialog(this@ShortcutsActivity, whatsNewContent)
                                    }
                                    resetSelection()
                            }
                            8L -> {
                                    config.showMedia = VIDEOS
                                    tryLoadGallery()
                            }
                        }
                        return false
                    }
                })
        if(whatsNew != null) {
            drawerBuilder.addDrawerItems(
                    gallery,
                    videos,
                    settings,
                    about,
                    whatsNewHeader,
                    whatsNew,
                    amazingUser,
                    share,
                    sendFeedback,
                    rateUs
            )
        } else {
            drawerBuilder.addDrawerItems(
                    gallery,
                    videos,
                    settings,
                    about,
                    amazingUser,
                    share,
                    sendFeedback,
                    rateUs
            )
        }
        drawer = drawerBuilder.build()

        if(baseConfig.appRunCount == 1) {
            Handler().postDelayed({
                drawer?.openDrawer()
            }, 500)
            Handler().postDelayed({
                drawer?.closeDrawer()
            }, 1500)
        }
    }

    private fun launchAbout() {
        startActivity(Intent(this, AboutActivity::class.java))
    }

    private fun resetSelection() {
        if(config.showMedia == IMAGES_AND_VIDEOS) {
            drawer?.setSelection(1L, false)
        } else {
            drawer?.setSelection(8L, false)
        }
    }

    fun initLocal() {
        init()
        // can be init later
        updateRecentsColor()
    }

    private fun initRecentMedia() {
        recent_fab.beVisibleIf(config.recentMediaEnabled)
        if(config.recentMediaEnabled) {
            recent_fab.setOnClickListener(View.OnClickListener {
                if(bottomsheet_parent.visibility == View.GONE) {
                    val layoutParamsFab = recent_fab.layoutParams
                    (layoutParamsFab as CoordinatorLayout.LayoutParams).bottomMargin = dpToPx(142)
                    updateRecentMedia(recentMedia)
                    recent_fab.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_close_white_18dp));
                } else {
                    val layoutParamsFab = recent_fab.layoutParams
                    updateRecentMedia(null)
                    (layoutParamsFab as CoordinatorLayout.LayoutParams).bottomMargin = dpToPx(16)
                    recent_fab.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_schedule_white_18dp));
                }
            })
        }
    }

    private fun updateRecentMedia(recentMedia: ArrayList<Medium>?) {
        if(recentMedia != null) {
            bottomsheet_parent.beVisible()
            val currAdapter = recent_grid.adapter
            if (currAdapter == null) {
                val mediaAdapter = MediaAdapter(this, recentMedia, this, true) {
                    val file = File(it.path)
                    val isVideo = file.isVideoFast()
                    if (isVideo) {
                        openWith(file, false)
                    } else {
                        Intent(this, ViewPagerActivity::class.java).apply {
                            putExtra(MEDIUM, it.path)
                            putExtra(SHOW_ALL, config.showAll)
                            startActivity(this)
                        }
                    }
                }
                mediaAdapter.scrollVertically = false
                mediaAdapter.notifyDataSetChanged()
                recent_grid.adapter = mediaAdapter

                val layoutManager = recent_grid.layoutManager as GridLayoutManager
                layoutManager.orientation = GridLayoutManager.HORIZONTAL
            } else {
                (currAdapter as MediaAdapter).updateMedia(recentMedia)
            }
        } else {
            bottomsheet_parent.beGone()
        }
    }

    private fun updateRecentMediaIfVisible() {
        if(bottomsheet_parent.visibility == View.VISIBLE) {
            updateRecentMedia(recentMedia)
        }
    }
}
