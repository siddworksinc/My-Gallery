package com.siddworks.android.mygallery

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.AsyncTask
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mikepenz.materialdrawer.Drawer
import com.mikepenz.materialdrawer.DrawerBuilder
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem
import com.mikepenz.materialdrawer.model.SectionDrawerItem
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem
import com.simplemobiletools.commons.extensions.hasWriteStoragePermission
import com.simplemobiletools.commons.extensions.toast
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.activities.MediaActivity
import com.simplemobiletools.gallery.activities.SimpleActivity
import com.simplemobiletools.gallery.adapters.ShortcutsAdapter
import com.simplemobiletools.gallery.asynctasks.GetDirectoriesAsynctask
import com.simplemobiletools.gallery.asynctasks.RefreshShortcutsAsynctask
import com.simplemobiletools.gallery.dialogs.ChangeSortingDialog
import com.simplemobiletools.gallery.dialogs.PasswordDialog
import com.simplemobiletools.gallery.dialogs.PickAlbumDialog
import com.simplemobiletools.gallery.extensions.config
import com.simplemobiletools.gallery.extensions.init
import com.simplemobiletools.gallery.extensions.launchAbout
import com.simplemobiletools.gallery.extensions.launchSettings
import com.simplemobiletools.gallery.helpers.*
import com.simplemobiletools.gallery.models.Shortcut
import com.simplemobiletools.gallery.views.MyScalableRecyclerView
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.util.*





class ShortcutsActivity : SimpleActivity(), ShortcutsAdapter.DirOperationsListener {

    lateinit var mShortcuts: ArrayList<Shortcut>
    private val STORAGE_PERMISSION = 1
    private var mIsGettingDirs = false
    private var mLoadedInitialPhotos = false
    private val PICK_MEDIA = 2

    private var mCurrAsyncTask: RefreshShortcutsAsynctask? = null

    private var toolbar: Toolbar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shortcuts)
        logEvent("ActivityShortcuts")

        toolbar = findViewById(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)

        directories_refresh_layout.setOnRefreshListener({ getShortcuts() })
        mShortcuts = ArrayList<Shortcut>()
        init()
        setupDrawer()
    }

    override fun onResume() {
        super.onResume()
        tryLoadGallery()
    }

    override fun onPause() {
        super.onPause()
        storeShortcuts(mShortcuts)
        directories_refresh_layout.isRefreshing = false
        MyScalableRecyclerView.mListener = null
    }

    private fun tryLoadGallery() {
        if (hasWriteStoragePermission()) {
            getShortcuts()
            handleZooming()
            checkIfColorChanged()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), STORAGE_PERMISSION)
        }
    }

    private fun getShortcuts() {
        if (mIsGettingDirs)
            return

        mIsGettingDirs = true
        mLoadedInitialPhotos = true

        val token = object : TypeToken<List<Shortcut>>() {}.type
        val shortcuts =  Gson().fromJson<ArrayList<Shortcut>>(config.shortcuts, token) ?: ArrayList<Shortcut>(1)
        Shortcut.sorting = config.directorySorting
        shortcuts.sort()
        movePinnedToFront(shortcuts)
        gotShortcuts(shortcuts)

        mCurrAsyncTask = RefreshShortcutsAsynctask(applicationContext, shortcuts) {
            if(it != null) {
                gotShortcuts(it)
            }
        }
        mCurrAsyncTask!!.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)

        if(config.directories == "") {
            val task = GetDirectoriesAsynctask(applicationContext, false, false) {
                val directories = Gson().toJson(it)
                config.directories = directories
            }
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        }
    }

    private fun gotShortcuts(dirs: ArrayList<Shortcut>) {
        directories_refresh_layout.isRefreshing = false
        mIsGettingDirs = false

        mShortcuts = dirs

        setupAdapter()
        storeShortcuts(mShortcuts)
    }

    private fun storeShortcuts(shortcuts: ArrayList<Shortcut>) {
        val directories = Gson().toJson(shortcuts)
        config.shortcuts = directories
    }

    private fun movePinnedToFront(dirs: ArrayList<Shortcut>): ArrayList<Shortcut> {
        val foundFolders = ArrayList<Shortcut>()
        val pinnedFolders = config.pinnedFolders

        dirs.forEach { if (pinnedFolders.contains(it.path)) foundFolders.add(it) }
        dirs.removeAll(foundFolders)
        dirs.addAll(0, foundFolders)
        return dirs
    }

    private fun checkIfColorChanged() {
        if (directories_grid.adapter != null && getRecyclerAdapter().foregroundColor != config.primaryColor) {
            getRecyclerAdapter().updatePrimaryColor(config.primaryColor)
            directories_fastscroller.updateHandleColor()
        }
    }

    private fun getRecyclerAdapter() = (directories_grid.adapter as ShortcutsAdapter)

    private fun handleZooming() {
        val layoutManager = directories_grid.layoutManager as GridLayoutManager
        layoutManager.spanCount = config.dirColumnCnt
        MyScalableRecyclerView.mListener = object : MyScalableRecyclerView.MyScalableRecyclerViewListener {
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

    private fun itemClicked(shortcut: Shortcut) {
        if(shortcut.passcode != null) {
            PasswordDialog(this, R.string.open, shortcut) {
                Intent(this, MediaActivity::class.java).apply {
                    putExtra(DIRECTORY, shortcut.path)
                    putExtra(GET_IMAGE_INTENT, false)
                    putExtra(GET_VIDEO_INTENT, false)
                    putExtra(GET_ANY_INTENT, false)
                    startActivityForResult(this, PICK_MEDIA)
                }
            }
        } else {
            Intent(this, MediaActivity::class.java).apply {
                putExtra(DIRECTORY, shortcut.path)
                putExtra(GET_IMAGE_INTENT, false)
                putExtra(GET_VIDEO_INTENT, false)
                putExtra(GET_ANY_INTENT, false)
                startActivityForResult(this, PICK_MEDIA)
            }
        }
    }

    private fun setupAdapter() {
        // Reset Selections
        if(directories_grid.adapter != null) {
            val recyclerAdapter = getRecyclerAdapter()
            recyclerAdapter.actMode?.finish()
        }
        val adapter = ShortcutsAdapter(this, mShortcuts, this) {
            itemClicked(it)
        }

        directories_grid.adapter = adapter
        directories_fastscroller.setViews(directories_grid, directories_refresh_layout)
    }

    override fun refreshItems() {
        getShortcuts()
    }

    override fun itemLongClicked(position: Int) {
        directories_grid.setDragSelectActive(position)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == STORAGE_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getShortcuts()
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
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.add_shortcut -> addShortcut()
            R.id.sort -> showSortingDialog()
            R.id.increase_column_count -> increaseColumnCount()
            R.id.reduce_column_count -> reduceColumnCount()
            R.id.settings -> launchSettings()
            R.id.about -> launchAbout()
            R.id.send_feedback -> showContactDeveloper(this@ShortcutsActivity)
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun increaseColumnCount() {
        config.dirColumnCnt = ++(directories_grid.layoutManager as GridLayoutManager).spanCount
        invalidateOptionsMenu()
    }

    private fun reduceColumnCount() {
        config.dirColumnCnt = --(directories_grid.layoutManager as GridLayoutManager).spanCount
        invalidateOptionsMenu()
    }

    private fun addShortcut() {
        val map = mShortcuts.map { it.path }
        PickAlbumDialog(this, map) {
            if(it == null) {
                toast("No More Albums to Add")
            } else {
                val selectedDir = it
                if(!mShortcuts.filter { selectedDir.path == it.path }.isEmpty()) {
                    toast("Cannot add duplicate albums")
                } else {
                    val shortcut = Shortcut(selectedDir.path, selectedDir.tmb, selectedDir.name,
                            selectedDir.mediaCnt, selectedDir.modified, selectedDir.taken, selectedDir.size)
                    mShortcuts.add(shortcut)
                    gotShortcuts(mShortcuts)
                }
            }
        }
    }

    private fun showSortingDialog() {
        ChangeSortingDialog(this, true, false) {
            getShortcuts()
        }
    }

    override fun tryDeleteFolders(folders: ArrayList<File>) {
    }

    private fun setupDrawer() {
        //if you want to update the items at a later time it is recommended to keep it in a variable
        val gallery = PrimaryDrawerItem().withIdentifier(1).withName(R.string.gallery)
                .withIcon(R.drawable.ic_image_black_24dp).withIconTintingEnabled(true)
        val settings = PrimaryDrawerItem().withIdentifier(2).withName(R.string.settings)
                .withIcon(R.drawable.ic_settings_black_24dp).withIconTintingEnabled(true)
        val about = PrimaryDrawerItem().withIdentifier(3).withName(R.string.about)
                .withIcon(R.drawable.ic_info_black_24dp).withIconTintingEnabled(true)
        val amazingUser = SectionDrawerItem().withName("Be An Amazing User :)").withDivider(true)
        val share = PrimaryDrawerItem().withIdentifier(4).withName(R.string.share)
                .withIcon(R.drawable.ic_share_black_24dp).withIconTintingEnabled(true)
        val sendFeedback = PrimaryDrawerItem().withIdentifier(5).withName(R.string.send_feedback)
                .withIcon(R.drawable.ic_send_white_24dp).withIconTintingEnabled(true)
        val rateUs = PrimaryDrawerItem().withIdentifier(5).withName(R.string.rate_us)
                .withIcon(R.drawable.ic_thumb_up_white_24dp).withIconTintingEnabled(true)
        // Remaining
        // Donate/Contribute
        // tips & tutorials

        val result = DrawerBuilder()
                .withActivity(this)
                .withToolbar(toolbar as Toolbar)
                .addDrawerItems(
                        gallery,
                        settings,
                        about,
                        amazingUser,
                        share,
                        sendFeedback,
                        rateUs
                )
                .withOnDrawerItemClickListener(object : Drawer.OnDrawerItemClickListener {
                    override fun onItemClick(view: View, position: Int, drawerItem: IDrawerItem<*, *>): Boolean {
                        // do something with the clicked item :D
                        return false
                    }
                })
                .build()
    }
}
