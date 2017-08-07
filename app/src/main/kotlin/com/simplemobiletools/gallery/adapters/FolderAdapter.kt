package com.simplemobiletools.gallery.adapters

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.simplemobiletools.commons.extensions.baseConfig
import com.simplemobiletools.gallery.R
import com.simplemobiletools.gallery.extensions.config
import kotlinx.android.synthetic.main.item_folder.view.*
import java.io.File
import java.util.*

class FolderAdapter(val context: Context, private var dirs: ArrayList<String>, val itemClick: (String) -> Unit) :
        RecyclerView.Adapter<FolderAdapter.ViewHolder>() {

    companion object {
        var textColor = 0
        var backgroundColor = 0
    }

    init {
        textColor = context.baseConfig.textColor
        backgroundColor = context.baseConfig.backgroundColor
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent?.context).inflate(R.layout.item_folder, parent, false)
        return ViewHolder(context, view, itemClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindView(position, dirs)
    }

    override fun onViewRecycled(holder: ViewHolder?) {
        super.onViewRecycled(holder)
        holder?.stopLoad()
    }

    override fun getItemCount() = dirs.size

    fun updateDirs(newDirs: ArrayList<String>) {
        dirs = newDirs
        notifyDataSetChanged()
    }

    class ViewHolder(val context: Context, val view: View, val itemClick: (String) -> (Unit)) : RecyclerView.ViewHolder(view) {
        fun bindView(position: Int, dirs: ArrayList<String>) {
            itemView.apply {
                list_item_holder.background = ColorDrawable(backgroundColor)

                list_item_name.text = File(dirs[position]).name
                list_item_name.setTextColor(textColor)

                list_item_number.text = (position + 1).toString()
                list_item_number.setTextColor(textColor)

                list_item_details.text = dirs[position]
                list_item_details.setTextColor(textColor)

                list_item_icon.setColorFilter(textColor)
                list_item_icon.setOnClickListener { itemClick(dirs[position]) }
            }
        }

        fun stopLoad() {
            try {
                Glide.with(context).clear(view.list_item_number)
            } catch (ignored: Exception) {
            }
        }
    }

    fun onItemMove(before: Int, after: Int) {
        if (before < after) {
            for (i in before..after - 1) {
                Collections.swap(dirs, i, i + 1)
            }
        } else {
            for (i in before downTo after + 1) {
                Collections.swap(dirs, i, i - 1)
            }
        }
        notifyItemMoved(before, after)
        notifyItemChanged(before)
        notifyItemChanged(after)
        context.config.orderedAlbums = Gson().toJson(dirs)
    }
}
