package com.ardrawing.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.ardrawing.app.camera.OverlayView
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Gestiona el guardado y carga de proyectos AR Drawing.
 *
 * Cada proyecto almacena:
 *  - Nombre
 *  - Fecha de creación / última edición
 *  - Una copia de la imagen (PNG) en almacenamiento interno
 *  - El estado completo del OverlayView (opacidad, escala, posición, mirror, grid, rotación)
 *
 * Los metadatos se guardan en SharedPreferences como JSON array.
 * Las imágenes se guardan en filesDir/projects/<id>/image.png
 */
class ProjectManager(private val context: Context) {

    companion object {
        private const val PREFS_NAME  = "ar_drawing_projects"
        private const val KEY_PROJECTS = "projects_list"
        private const val DATE_FORMAT  = "dd/MM/yyyy HH:mm"
    }

    // ── Modelo de datos ──────────────────────────────────────────────────────

    data class Project(
        val id: String,
        val name: String,
        val createdAt: String,
        val updatedAt: String,
        val imageUri: String,      // ruta interna o uri externa original
        val imagePath: String,     // ruta absoluta del PNG guardado internamente
        val opacity: Float,
        val scale: Float,
        val translateX: Float,
        val translateY: Float,
        val isMirror: Boolean,
        val isGrid: Boolean,
        val rotationDeg: Float,
        val referenceImagePaths: List<String> = emptyList()
    )

    // ── CRUD ─────────────────────────────────────────────────────────────────

    /** Devuelve todos los proyectos guardados, más reciente primero. */
    fun getAllProjects(): List<Project> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json  = prefs.getString(KEY_PROJECTS, "[]") ?: "[]"
        val array = JSONArray(json)
        val list  = mutableListOf<Project>()
        for (i in 0 until array.length()) {
            list.add(projectFromJson(array.getJSONObject(i)))
        }
        return list.sortedByDescending { it.updatedAt }
    }

    /**
     * Guarda un proyecto nuevo o actualiza uno existente.
     * Si [projectId] es null, crea uno nuevo.
     * Devuelve el proyecto guardado.
     */
    fun saveProject(
        projectId: String?,
        name: String,
        bitmap: Bitmap?,
        originalUri: String?,
        state: OverlayView.OverlayState,
        referenceBitmaps: List<Bitmap> = emptyList()
    ): Project {
        val id  = projectId ?: UUID.randomUUID().toString()
        val now = SimpleDateFormat(DATE_FORMAT, Locale.getDefault()).format(Date())

        // Guardar imagen internamente
        // Guardar imagen internamente
        val imagePath = saveImage(id, bitmap)
        val refPaths = referenceBitmaps.mapIndexed { i, bmp ->
            saveReferenceImage(id, i, bmp)
        }

        val projects    = getAllProjects().toMutableList()
        val existingIdx = projects.indexOfFirst { it.id == id }

        val createdAt = if (existingIdx >= 0) projects[existingIdx].createdAt else now

        val project = Project(
            id          = id,
            name        = name.ifBlank { "Proyecto ${projects.size + 1}" },
            createdAt   = createdAt,
            updatedAt   = now,
            imageUri    = originalUri ?: imagePath,
            imagePath   = imagePath,
            opacity     = state.opacity,
            scale       = state.scale,
            translateX  = state.translateX,
            translateY  = state.translateY,
            isMirror    = state.isMirror,
            isGrid      = state.isGrid,
            rotationDeg = state.rotationDeg,
            referenceImagePaths = refPaths
        )

        if (existingIdx >= 0) projects[existingIdx] = project
        else projects.add(0, project)

        persistProjects(projects)
        return project
    }

    /** Elimina un proyecto y su imagen. */
    fun deleteProject(id: String) {
        val projects = getAllProjects().toMutableList()
        projects.removeAll { it.id == id }
        // Borrar carpeta de imagen
        val dir = File(context.filesDir, "projects/$id")
        dir.deleteRecursively()
        persistProjects(projects)
    }

    /** Carga el Bitmap de un proyecto guardado. */
    fun loadBitmap(project: Project): Bitmap? {
        val file = File(project.imagePath)
        if (!file.exists()) return null
        return BitmapFactory.decodeFile(file.absolutePath)
    }

    /** Convierte un Project en OverlayState listo para restaurar. */
    fun toOverlayState(project: Project) = OverlayView.OverlayState(
        opacity     = project.opacity,
        scale       = project.scale,
        translateX  = project.translateX,
        translateY  = project.translateY,
        isMirror    = project.isMirror,
        isGrid      = project.isGrid,
        rotationDeg = project.rotationDeg
    )

    // ── Privado ──────────────────────────────────────────────────────────────

    private fun saveImage(id: String, bitmap: Bitmap?): String {
        val dir = File(context.filesDir, "projects/$id")
        dir.mkdirs()
        val file = File(dir, "image.png")
        if (bitmap != null) {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
            }
        }
        return file.absolutePath
    }

    private fun saveReferenceImage(id: String, index: Int, bitmap: Bitmap): String {
        val dir = File(context.filesDir, "projects/$id")
        dir.mkdirs()
        val file = File(dir, "ref_$index.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
        }
        return file.absolutePath
    }

    private fun persistProjects(projects: List<Project>) {
        val array = JSONArray()
        projects.forEach { array.put(projectToJson(it)) }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PROJECTS, array.toString())
            .apply()
    }

    private fun projectToJson(p: Project) = JSONObject().apply {
        put("id",          p.id)
        put("name",        p.name)
        put("createdAt",   p.createdAt)
        put("updatedAt",   p.updatedAt)
        put("imageUri",    p.imageUri)
        put("imagePath",   p.imagePath)
        put("opacity",     p.opacity)
        put("scale",       p.scale)
        put("translateX",  p.translateX)
        put("translateY",  p.translateY)
        put("isMirror",    p.isMirror)
        put("isGrid",      p.isGrid)
        put("rotationDeg", p.rotationDeg)
        val refArray = JSONArray()
        p.referenceImagePaths.forEach { refArray.put(it) }
        put("referencePaths", refArray)
    }

    private fun projectFromJson(j: JSONObject) = Project(
        id          = j.getString("id"),
        name        = j.getString("name"),
        createdAt   = j.getString("createdAt"),
        updatedAt   = j.getString("updatedAt"),
        imageUri    = j.getString("imageUri"),
        imagePath   = j.getString("imagePath"),
        opacity     = j.getDouble("opacity").toFloat(),
        scale       = j.getDouble("scale").toFloat(),
        translateX  = j.getDouble("translateX").toFloat(),
        translateY  = j.getDouble("translateY").toFloat(),
        isMirror    = j.getBoolean("isMirror"),
        isGrid      = j.getBoolean("isGrid"),
        rotationDeg = j.getDouble("rotationDeg").toFloat(),
        referenceImagePaths = run {
            val arr = j.optJSONArray("referencePaths") ?: JSONArray()
            (0 until arr.length()).map { arr.getString(it) }
        }
    )
}
