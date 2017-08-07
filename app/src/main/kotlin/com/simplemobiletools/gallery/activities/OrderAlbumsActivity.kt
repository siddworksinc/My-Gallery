package com.simplemobiletools.gallery.activities

import android.os.Bundle
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.view.Menu
import android.view.MenuItem
import com.google.gson.Gson
import com.simplemobiletools.commons.extensions.beVisibleIf
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.adapters.FolderAdapter
import com.simplemobiletools.gallery.dialogs.PickAlbumDialog
import com.simplemobiletools.gallery.extensions.config
import com.simplemobiletools.gallery.helpers.logEvent
import kotlinx.android.synthetic.main.activity_order_albums.*


class OrderAlbumsActivity : SimpleActivity() {
    private lateinit var folders: ArrayList<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order_albums)
        logEvent("ActivityOrderAlbums")
        updateSortedFolders()
    }

    private fun updateSortedFolders() {
        folders = config.parseOrderedAlbums()

        order_albums_placeholder.beVisibleIf(folders.isEmpty())
        order_albums_placeholder.setTextColor(config.textColor)
        order_albums_rv.beVisibleIf(!folders.isEmpty())

        if(!folders.isEmpty()) {
            val currAdapter = order_albums_rv.adapter
            if (currAdapter == null) {
                val adapter = FolderAdapter(this, folders) {
                    config.removeOrderedAlbum(it)
                    updateSortedFolders()
                }
                order_albums_rv.adapter = adapter
                val ith = ItemTouchHelper(SimpleItemTouchHelperCallback(adapter))
                ith.attachToRecyclerView(order_albums_rv)

                DividerItemDecoration(this, DividerItemDecoration.VERTICAL).apply {
                    setDrawable(resources.getDrawable(R.drawable.divider))
                    order_albums_rv.addItemDecoration(this)
                }
            } else {
                (currAdapter as FolderAdapter).updateDirs(folders)
            }
        }
//        excluded_folders_placeholder.beVisibleIf(folders.isEmpty())
//        excluded_folders_placeholder.setTextColor(config.textColor)


//        val layoutManager = sort_albums_rv.layoutManager as LinearLayoutManager


    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_excluded_folders, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.add_folder -> addSortedFolder()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun addSortedFolder() {
        PickAlbumDialog(this, null, folders) {
            folders.add(it.path)
            config.orderedAlbums = Gson().toJson(folders)
            updateSortedFolders()
        }
    }
}

internal class SimpleItemTouchHelperCallback(private val mAdapter: FolderAdapter) : ItemTouchHelper.Callback() {
    override fun getMovementFlags(recyclerView: RecyclerView?, viewHolder: RecyclerView.ViewHolder?): Int {
        val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
        val swipeFlags = ItemTouchHelper.START or ItemTouchHelper.END
        return makeMovementFlags(dragFlags, swipeFlags)
    }

    override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
        mAdapter.onItemMove(viewHolder.adapterPosition, target.adapterPosition)
        return true
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder?, direction: Int) {
    }

    override fun isLongPressDragEnabled(): Boolean {
        return true
    }

    override fun isItemViewSwipeEnabled(): Boolean {
        return false
    }
}
