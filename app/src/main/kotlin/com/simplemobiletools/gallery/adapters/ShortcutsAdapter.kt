package com.simplemobiletools.gallery.adapters

import android.content.DialogInterface
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.*
import android.os.Build
import android.support.v7.app.AlertDialog
import android.support.v7.content.res.AppCompatResources
import android.support.v7.view.ActionMode
import android.support.v7.widget.RecyclerView
import android.util.SparseArray
import android.view.*
import android.widget.FrameLayout
import com.bignerdranch.android.multiselector.ModalMultiSelectorCallback
import com.bignerdranch.android.multiselector.MultiSelector
import com.bignerdranch.android.multiselector.SwappingHolder
import com.bumptech.glide.Glide
import com.simplemobiletools.commons.dialogs.ConfirmationDialog
import com.simplemobiletools.commons.dialogs.PropertiesDialog
import com.simplemobiletools.commons.extensions.adjustAlpha
import com.simplemobiletools.commons.extensions.isImageVideoGif
import com.simplemobiletools.commons.extensions.needsStupidWritePermissions
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.commons.views.MyTextView
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.activities.ShortcutsActivity
import com.simplemobiletools.gallery.activities.SimpleActivity
import com.simplemobiletools.gallery.dialogs.EditShortcutDialog
import com.simplemobiletools.gallery.dialogs.MyPropertiesDialog
import com.simplemobiletools.gallery.dialogs.PasswordDialog
import com.simplemobiletools.gallery.extensions.*
import com.simplemobiletools.gallery.helpers.dpToPx
import com.simplemobiletools.gallery.models.Directory
import kotlinx.android.synthetic.main.directory_item_centered.view.*
import kotlinx.android.synthetic.main.directory_tmb.view.*
import java.io.File
import java.util.*

class ShortcutsAdapter(val activity: ShortcutsActivity, var dirs: MutableList<Directory>,
                       val listener: ShortcutsAdapter.DirOperationsListener?, val itemClick: (Directory) -> Unit) :
        RecyclerView.Adapter<ShortcutsAdapter.ViewHolder>() {

    val multiSelector = MultiSelector()
    val config = activity.config

    var actMode: ActionMode? = null
    var itemViews = SparseArray<View>()
    val selectedPositions = HashSet<Int>()
    var foregroundColor = config.primaryColor
    var scrollVertically = !config.scrollHorizontally
    private var selectorOverlayDrawable: LayerDrawable? = null


    fun toggleItemSelection(select: Boolean, pos: Int) {
        if (itemViews[pos] != null)
            getProperView(itemViews[pos]!!).isSelected = select

        if (select)
            selectedPositions.add(pos)
        else
            selectedPositions.remove(pos)

        if (selectedPositions.isEmpty()) {
            actMode?.finish()
            return
        }

        updateTitle(selectedPositions.size)
    }

    fun getProperView(itemView: View): View {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            itemView.dir_frame
        else
            itemView.dir_thumbnail
    }

    fun updateTitle(cnt: Int) {
        actMode?.title = "$cnt / ${dirs.size}"
        actMode?.invalidate()
    }

    fun updatePrimaryColor(color: Int) {
        foregroundColor = color
        (0..itemViews.size() - 1).mapNotNull { itemViews[it] }
                .forEach { setupItemViewForeground(it) }
    }

    private fun setupItemViewForeground(itemView: View) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            (getProperView(itemView) as FrameLayout).foreground = getSelector()
        else
            getProperView(itemView).foreground = getSelector()
    }

    private fun getSelector(): StateListDrawable {
        if(selectorOverlayDrawable == null) {
            val layers = arrayOfNulls<Drawable>(2)
            layers[0] = foregroundColor.createSelector()
            layers[1] = AppCompatResources.getDrawable(activity, R.drawable.ic_check_black_overlay)
            selectorOverlayDrawable = LayerDrawable(layers)
        }

        val selectorOverlay = StateListDrawable()
        selectorOverlay.addState(intArrayOf(android.R.attr.state_selected), selectorOverlayDrawable)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            val pressedDrawable = ColorDrawable(foregroundColor.adjustAlpha(0.2f))
            selectorOverlay.addState(intArrayOf(android.R.attr.state_pressed), pressedDrawable)
        } else {
            val pressedDrawable = RippleDrawable(ColorStateList.valueOf(foregroundColor.adjustAlpha(0.2f)), null, ColorDrawable(Color.WHITE))
            selectorOverlay.addState(intArrayOf(), pressedDrawable)
        }
        return selectorOverlay
    }

    val adapterListener = object : MyAdapterListener {
        override fun toggleItemSelectionAdapter(select: Boolean, position: Int) {
            toggleItemSelection(select, position)
        }

        override fun setupItemForeground(itemView: View) {
            setupItemViewForeground(itemView)
        }

        override fun getSelectedPositions(): HashSet<Int> = selectedPositions
    }

    val multiSelectorMode = object : ModalMultiSelectorCallback(multiSelector) {
        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            when (item.itemId) {
                R.id.cab_edit -> editDirectory()
                R.id.cab_hide -> toggleFoldersVisibility(true)
                R.id.cab_unhide -> toggleFoldersVisibility(false)
                R.id.cab_select_all -> selectAll()
                R.id.cab_select_inverse -> selectInverse()
                R.id.cab_delete -> askConfirmDelete()
                R.id.cab_exclude -> tryExcludeFolder()
                R.id.cab_copy_to -> copyMoveTo(true)
                R.id.cab_move_to -> copyMoveTo(false)
                R.id.cab_properties -> showProperties()
                else -> return false
            }
            return true
        }

        private fun copyMoveTo(isCopyOperation: Boolean) {
            if (selectedPositions.isEmpty())
                return

            val files = ArrayList<File>()
            selectedPositions.forEach {
                val dir = File(dirs[it].path)
                files.addAll(dir.listFiles().filter { it.isFile && it.isImageVideoGif() })
            }

            activity.tryCopyMoveFilesTo(files, isCopyOperation) {
                listener?.refreshItems()
                actMode?.finish()
            }
        }

        private fun tryExcludeFolder() {
            val excludeText = activity.getString(R.string.exclude_folder_description) + "\n\n" + getSelectedPaths().toList().reduce { acc, s -> acc + "\n" + s }
            val view = MyTextView(activity)
            view.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))
            view.text = excludeText
            AlertDialog.Builder(activity)
                    .setPositiveButton("OK", DialogInterface.OnClickListener { dialog, which ->
                        run {
                            activity.config.addExcludedFolders(getSelectedPaths())
                            listener?.refreshItems()
                            actMode?.finish()
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .create().apply {
                activity.setupDialogStuff(view, this, R.string.additional_info)
            }
        }

        override fun onCreateActionMode(actionMode: ActionMode?, menu: Menu?): Boolean {
            super.onCreateActionMode(actionMode, menu)
            actMode = actionMode
            activity.menuInflater.inflate(R.menu.cab_shortcuts, menu)
            activity.updateStatusBarColor(R.color.black)
            return true
        }

        override fun onPrepareActionMode(actionMode: ActionMode?, menu: Menu): Boolean {
            menu.findItem(R.id.cab_edit).isVisible = selectedPositions.size <= 1
            checkHideBtnVisibility(menu)
            return true
        }

        override fun onDestroyActionMode(actionMode: ActionMode?) {
            super.onDestroyActionMode(actionMode)
            selectedPositions.forEach {
                if (itemViews[it] != null)
                    getProperView(itemViews[it]!!).isSelected = false
            }
            selectedPositions.clear()
            actMode = null
            activity.updateActionbarColor(config.primaryColor)
        }

        fun checkHideBtnVisibility(menu: Menu) {
            var hiddenCnt = 0
            var unhiddenCnt = 0
            selectedPositions.map { dirs.getOrNull(it)?.path }.filterNotNull().forEach {
                if (File(it).containsNoMedia())
                    hiddenCnt++
                else
                    unhiddenCnt++
            }

            menu.findItem(R.id.cab_hide).isVisible = unhiddenCnt > 0
            menu.findItem(R.id.cab_unhide).isVisible = hiddenCnt > 0
        }
    }

    private fun toggleFoldersVisibility(hide: Boolean) {
        getSelectedPaths().forEach {
            if (hide) {
                if (config.wasHideFolderTooltipShown) {
                    hideFolder(it)
                } else {
                    config.wasHideFolderTooltipShown = true
                    ConfirmationDialog(activity, activity.getString(R.string.hide_folder_description)) {
                        hideFolder(it)
                    }
                }
            } else {
                activity.removeNoMedia(it) {
                    noMediaHandled()
                }
            }
        }
    }

    private fun hideFolder(path: String) {
        activity.addNoMedia(path) {
            noMediaHandled()
        }
    }

    private fun noMediaHandled() {
        activity.runOnUiThread {
            listener?.refreshItems()
            actMode?.finish()
        }
    }

    private fun editDirectory() {
        val shortcut = dirs[selectedPositions.first()]
        if (shortcut.passcode != null) {
            PasswordDialog(activity, R.string.edit, shortcut) {
                EditShortcutDialog(activity, shortcut) {
                    listener?.refreshItems()
                    notifyDataSetChanged()
                    actMode?.finish()
                }
            }
        } else {
            EditShortcutDialog(activity, shortcut) {
                listener?.refreshItems()
                notifyDataSetChanged()
                actMode?.finish()
            }
        }
    }

    private fun showProperties() {
        if (selectedPositions.size <= 1) {
            if (dirs[selectedPositions.first()].passcode != null) {
                PasswordDialog(activity, R.string.properties, dirs[selectedPositions.first()]) {
                    PropertiesDialog(activity, dirs[selectedPositions.first()].path, config.shouldShowHidden)
                }
            } else {
                PropertiesDialog(activity, dirs[selectedPositions.first()].path, config.shouldShowHidden)
            }
        } else {
            val paths = ArrayList<String>()
            selectedPositions.forEach { paths.add(dirs[it].path) }
            MyPropertiesDialog(activity, paths, config.shouldShowHidden)
        }
    }

    fun selectAll() {
        val cnt = dirs.size
        for (i in 0..cnt - 1) {
            selectedPositions.add(i)
            notifyItemChanged(i)
        }
        updateTitle(cnt)
    }

    private fun selectInverse() {
        val cnt = dirs.size
        for (i in 0..cnt - 1) {
            if(selectedPositions.contains(i)) {
                selectedPositions.remove(i)
            } else  {
                selectedPositions.add(i)
            }
            notifyItemChanged(i)
        }
        updateTitle(selectedPositions.size)
    }

    private fun askConfirmDelete() {
        ConfirmationDialog(activity) {
            deleteFiles()
            actMode?.finish()
        }
    }

    private fun deleteFiles() {
        val folders = ArrayList<File>(selectedPositions.size)
        val removeFolders = ArrayList<Directory>(selectedPositions.size)

        var needPermissionForPath = ""
        selectedPositions.forEach {
            if (dirs.size > it) {
                val path = dirs[it].path
                if (activity.needsStupidWritePermissions(path) && config.treeUri.isEmpty()) {
                    needPermissionForPath = path
                }
            }
        }

        activity.handleSAFDialog(File(needPermissionForPath)) {
            selectedPositions.sortedDescending().forEach {
                if (dirs.size > it) {
                    val directory = dirs[it]
                    folders.add(File(directory.path))
                    removeFolders.add(directory)
                    notifyItemRemoved(it)
                    itemViews.put(it, null)
                }
            }

            dirs.removeAll(removeFolders)
            selectedPositions.clear()
            listener?.tryDeleteFolders(folders)

            val newItems = SparseArray<View>()
            var curIndex = 0
            for (i in 0..itemViews.size() - 1) {
                if (itemViews[i] != null) {
                    newItems.put(curIndex, itemViews[i])
                    curIndex++
                }
            }

            itemViews = newItems
        }
    }

    private fun getSelectedPaths(): HashSet<String> {
        val paths = HashSet<String>(selectedPositions.size)
        selectedPositions.forEach { paths.add(dirs[it].path) }
        return paths
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent?.context).inflate(R.layout.directory_item_centered, parent, false)
        return ViewHolder(view, adapterListener, activity, multiSelectorMode, multiSelector, listener, itemClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val dir = dirs[position]
        itemViews.put(position, holder.bindView(dir, scrollVertically))
        toggleItemSelection(selectedPositions.contains(position), position)
        holder.itemView.tag = holder
    }

    override fun onViewRecycled(holder: ViewHolder?) {
        super.onViewRecycled(holder)
        holder?.stopLoad()
    }

    override fun getItemCount() = dirs.size

    fun selectItem(pos: Int) {
        toggleItemSelection(true, pos)
    }

    fun selectRange(from: Int, to: Int, min: Int, max: Int) {
        if (from == to) {
            (min..max).filter { it != from }
                    .forEach { toggleItemSelection(false, it) }
            return
        }

        if (to < from) {
            for (i in to..from)
                toggleItemSelection(true, i)

            if (min > -1 && min < to) {
                (min..to - 1).filter { it != from }
                        .forEach { toggleItemSelection(false, it) }
            }
            if (max > -1) {
                for (i in from + 1..max)
                    toggleItemSelection(false, i)
            }
        } else {
            for (i in from..to)
                toggleItemSelection(true, i)

            if (max > -1 && max > to) {
                (to + 1..max).filter { it != from }
                        .forEach { toggleItemSelection(false, it) }
            }

            if (min > -1) {
                for (i in min..from - 1)
                    toggleItemSelection(false, i)
            }
        }
    }

    class ViewHolder(val view: View, val adapterListener: MyAdapterListener,
                     val activity: SimpleActivity, val multiSelectorCallback: ModalMultiSelectorCallback,
                     val multiSelector: MultiSelector, val listener: DirOperationsListener?,
                     val itemClick: (Directory) -> (Unit)) :

            SwappingHolder(view, MultiSelector()) {

        fun bindView(shortcut: Directory, scrollVertically: Boolean): View {
            itemView.apply {
                dir_name.text = shortcut.name + " (" + shortcut.mediaCnt + ")"
                activity.loadImageForShortcut(shortcut, dir_thumbnail, scrollVertically)

                setOnClickListener { viewClicked(shortcut) }
                setOnLongClickListener { viewLongClicked(); true }

                adapterListener.setupItemForeground(this)
            }
            return itemView
        }

        fun viewClicked(directory: Directory) {
            if (multiSelector.isSelectable) {
                val isSelected = adapterListener.getSelectedPositions().contains(layoutPosition)
                adapterListener.toggleItemSelectionAdapter(!isSelected, layoutPosition)
            } else {
                itemClick(directory)
            }
        }

        fun viewLongClicked() {
            if (listener != null) {
                if (!multiSelector.isSelectable) {
                    activity.startSupportActionMode(multiSelectorCallback)
                    adapterListener.toggleItemSelectionAdapter(true, layoutPosition)
                }

                listener.itemLongClicked(layoutPosition)
            }
        }

        fun stopLoad() {
            if (!activity.isDestroyed)
                Glide.with(activity).clear(view.dir_thumbnail)
        }
    }

    interface MyAdapterListener {
        fun toggleItemSelectionAdapter(select: Boolean, position: Int)

        fun setupItemForeground(itemView: View)

        fun getSelectedPositions(): HashSet<Int>
    }

    interface DirOperationsListener {
        fun refreshItems()

        fun tryDeleteFolders(folders: ArrayList<File>)

        fun itemLongClicked(position: Int)
    }

    fun updateDirs(newDirs: ArrayList<Directory>) {
        dirs = newDirs
        notifyDataSetChanged()
    }
}
