package com.ardrawing.app.gallery

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ardrawing.app.databinding.ItemGalleryBinding
import com.bumptech.glide.Glide

class GalleryAdapter(
    private val items: MutableList<DrawingItem>,
    private val onItemClick: (DrawingItem) -> Unit
) : RecyclerView.Adapter<GalleryAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemGalleryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: DrawingItem) {
            binding.tvTitle.text = item.title
            binding.tvCategory.text = item.category

            Glide.with(binding.root.context)
                .load(item.resourceId)
                .centerCrop()
                .placeholder(android.R.drawable.ic_menu_gallery)
                .into(binding.ivThumbnail)

            binding.root.setOnClickListener { onItemClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemGalleryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(items[position])

    override fun getItemCount() = items.size

    fun updateItems(newItems: List<DrawingItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}
