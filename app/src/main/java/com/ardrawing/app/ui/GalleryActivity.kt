package com.ardrawing.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.ardrawing.app.data.ProjectManager
import com.ardrawing.app.databinding.ActivityGalleryBinding
import com.ardrawing.app.gallery.ProjectsAdapter

class GalleryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGalleryBinding
    private lateinit var projectManager: ProjectManager
    private lateinit var adapter: ProjectsAdapter

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { openDrawingWithUri(it) }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        if (perms.values.all { it }) pickImageLauncher.launch("image/*")
        else Toast.makeText(this, "Se necesita permiso para importar fotos", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGalleryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        projectManager = ProjectManager(this)

        setupRecyclerView()
        setupFab()

       
    }

    override fun onResume() {
        super.onResume()
        // Recargar proyectos cada vez que se vuelve a esta pantalla
        loadProjects()
    }

    // ── Lista de proyectos ────────────────────────────────────────────────────

    private fun setupRecyclerView() {
        adapter = ProjectsAdapter(
            onOpen   = { project -> openProject(project) },
            onDelete = { project -> confirmDelete(project) }
        )
        binding.rvGallery.layoutManager = LinearLayoutManager(this)
        binding.rvGallery.adapter = adapter
    }

    private fun loadProjects() {
        val projects = projectManager.getAllProjects()
        adapter.setProjects(projects)

        // Mostrar estado vacío si no hay proyectos
        binding.tvEmpty.visibility    = if (projects.isEmpty()) View.VISIBLE else View.GONE
        binding.rvGallery.visibility  = if (projects.isEmpty()) View.GONE   else View.VISIBLE
    }

    private fun openProject(project: ProjectManager.Project) {
        val intent = Intent(this, DrawingActivity::class.java).apply {
            putExtra(DrawingActivity.EXTRA_PROJECT_ID, project.id)
        }
        startActivity(intent)
    }

    private fun confirmDelete(project: ProjectManager.Project) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar proyecto")
            .setMessage("¿Eliminar «${project.name}»? No se puede deshacer.")
            .setPositiveButton("Eliminar") { _, _ ->
                projectManager.deleteProject(project.id)
                loadProjects()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // ── Importar foto nueva ───────────────────────────────────────────────────

    private fun setupFab() {
        binding.fabImport.setOnClickListener {
            checkPermissionsAndPickImage()
        }
    }



    private fun checkPermissionsAndPickImage() {
        val needed = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                != PackageManager.PERMISSION_GRANTED)
                needed.add(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED)
                needed.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        if (needed.isEmpty()) pickImageLauncher.launch("image/*")
        else permissionLauncher.launch(needed.toTypedArray())
    }

    private fun openDrawingWithUri(uri: Uri) {
        val intent = Intent(this, DrawingActivity::class.java).apply {
            putExtra(DrawingActivity.EXTRA_IMAGE_URI, uri.toString())
        }
        startActivity(intent)
    }
}
