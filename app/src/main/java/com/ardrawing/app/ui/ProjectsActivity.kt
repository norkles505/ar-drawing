package com.ardrawing.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ardrawing.app.R
import com.ardrawing.app.data.ProjectManager
import com.bumptech.glide.Glide



/**
 * Pantalla que muestra todos los proyectos guardados.
 * Desde aquí se puede:
 *  - Continuar un proyecto (abre DrawingActivity con EXTRA_PROJECT_ID)
 *  - Eliminar un proyecto
 */
class ProjectsActivity : AppCompatActivity() {

    private lateinit var projectManager: ProjectManager
    private lateinit var recyclerView: RecyclerView

    private lateinit var adapter: ProjectsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_projects)

        projectManager = ProjectManager(this)
        recyclerView   = findViewById(R.id.rvProjects)
     

        recyclerView.layoutManager = LinearLayoutManager(this)

        findViewById<ImageButton>(R.id.btnBackProjects).setOnClickListener { finish() }


   

        loadProjects()

        
    }

    override fun onResume() {
        super.onResume()
        loadProjects()
    }

    private fun loadProjects() {
        val projects = projectManager.getAllProjects()

        findViewById<TextView>(R.id.tvProjectCount).text =
            if (projects.isEmpty()) "" else "${projects.size} guardados"
        findViewById<View>(R.id.layoutEmpty).visibility =
            if (projects.isEmpty()) View.VISIBLE else View.GONE


       
        recyclerView.visibility = if (projects.isEmpty()) View.GONE else View.VISIBLE

        adapter = ProjectsAdapter(
            projects = projects.toMutableList(),
            onOpen = { project ->
                val intent = Intent(this, DrawingActivity::class.java)
                intent.putExtra(DrawingActivity.EXTRA_PROJECT_ID, project.id)
                startActivity(intent)
            },
            onDelete = { project ->
                AlertDialog.Builder(this)
                    .setTitle("Eliminar proyecto")
                    .setMessage("¿Eliminar «${project.name}»? Esta acción no se puede deshacer.")
                    .setPositiveButton("Eliminar") { _, _ ->
                        projectManager.deleteProject(project.id)
                        loadProjects()
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
        )
        recyclerView.adapter = adapter
    }

    // ── Adapter ───────────────────────────────────────────────────────────────

    private class ProjectsAdapter(
        private val projects: MutableList<ProjectManager.Project>,
        private val onOpen: (ProjectManager.Project) -> Unit,
        private val onDelete: (ProjectManager.Project) -> Unit
    ) : RecyclerView.Adapter<ProjectsAdapter.VH>() {

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
            val project = projects[position]
            holder.name.text = project.name
            holder.date.text = "Editado: ${project.updatedAt}"

            Glide.with(holder.itemView)
                .load(project.imagePath)
                .centerCrop()
                .placeholder(R.drawable.ic_pencil)
                .into(holder.thumbnail)
            holder.btnDelete.setOnClickListener { onDelete(project) }
            holder.itemView.setOnClickListener  { onOpen(project) }

            // Badge HOY
            val today = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(java.util.Date())
            val isToday = project.updatedAt.startsWith(today)
            holder.itemView.findViewById<TextView>(R.id.tvTodayBadge).visibility =
                if (isToday) View.VISIBLE else View.GONE

            // Barra acento: naranja si es el más reciente, gris el resto
            holder.itemView.findViewById<View>(R.id.viewAccentBar)
                .setBackgroundColor(if (position == 0) 0xFFFF6B35.toInt() else 0xFF232323.toInt())


        }

        override fun getItemCount() = projects.size
    }
}


