package com.ardrawing.app.gallery

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ardrawing.app.R
import com.ardrawing.app.data.ProjectManager
import com.bumptech.glide.Glide

class ProjectsAdapter(
    private val onOpen: (ProjectManager.Project) -> Unit,
    private val onDelete: (ProjectManager.Project) -> Unit
) : RecyclerView.Adapter<ProjectsAdapter.VH>() {

    private val items = mutableListOf<ProjectManager.Project>()

    fun setProjects(projects: List<ProjectManager.Project>) {
        items.clear()
        items.addAll(projects)
        notifyDataSetChanged()
    }

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val thumbnail: ImageView   = view.findViewById(R.id.ivProjectThumb)
        val name: TextView         = view.findViewById(R.id.tvProjectName)
        val date: TextView         = view.findViewById(R.id.tvProjectDate)
      
        val btnDelete: ImageButton = view.findViewById(R.id.btnDeleteProject)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_project, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val project = items[position]

        holder.name.text = project.name
        holder.date.text = "Editado: ${project.updatedAt}"

        Glide.with(holder.itemView.context)
            .load(project.imagePath)
            .centerCrop()
            .placeholder(R.drawable.ic_pencil)
            .into(holder.thumbnail)

        holder.itemView.setOnClickListener { onOpen(project) }
     
        holder.btnDelete.setOnClickListener { onDelete(project) }
    }

    override fun getItemCount() = items.size
}
