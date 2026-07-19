package com.ardrawing.app.data

import android.content.Context
import org.json.JSONObject

class CourseManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("course_prefs", Context.MODE_PRIVATE)

    data class Lesson(
        val id: String,
        val title: String,
        val description: String,
        val emoji: String,
        val xpReward: Int = 10,
        val imageResName: String? = null
    )

    data class Module(
        val id: String,
        val title: String,
        val emoji: String,
        val lessons: List<Lesson>
    )

    data class Level(
        val id: String,
        val number: Int,
        val title: String,
        val subtitle: String,
        val color: String,
        val modules: List<Module>
    )

    val levels = listOf(
        Level("lvl1", 1, "Fundamentos", "Para quien nunca dibujó", "#4A90D9",
            listOf(
                Module("m1_1", "Líneas y trazos", "✏️", listOf(
                    Lesson("l1", "Líneas rectas", "Trazá líneas rectas sin regla de arriba a abajo", "📏"),
                    Lesson("l2", "Líneas curvas", "Practicá curvas fluidas y continuas", "〰️"),
                    Lesson("l3", "Control de presión", "Variá la presión para cambiar el grosor", "🖊️"),
                    Lesson("l4", "Velocidad del trazo", "Rápido vs lento — cómo afecta al trazo", "⚡"),
                    Lesson("l5", "Líneas paralelas", "Trazá líneas paralelas equidistantes", "☰"),
                    Lesson("l6", "Trazos cortos y largos", "Combiná trazos de distinta longitud", "➖")
                )),
                Module("m1_2", "Formas básicas", "🔷", listOf(
                    Lesson("l7", "Círculo perfecto", "Dibujá círculos a mano alzada", "⭕"),
                    Lesson("l8", "Cuadrado y rectángulo", "Formas rectas sin regla", "🔲"),
                    Lesson("l9", "Triángulo", "Triángulos de distintos tipos", "🔺"),
                    Lesson("l10", "Óvalo y elipse", "La elipse como círculo en perspectiva", "🥚"),
                    Lesson("l11", "Combinar formas", "Objetos simples con formas básicas", "🎯"),
                    Lesson("l12", "Formas orgánicas", "Formas irregulares libres", "🌊")
                )),
                Module("m1_3", "Ver para dibujar", "👁️", listOf(
                    Lesson("l13", "Ver ángulos reales", "Entrenás el ojo para medir ángulos", "📐"),
                    Lesson("l14", "Medir con el lápiz", "Técnica clásica de proporción", "📏"),
                    Lesson("l15", "Negative space", "Dibujá el espacio vacío alrededor", "⬜"),
                    Lesson("l16", "Relaciones de tamaño", "Comparar elementos entre sí", "↔️"),
                    Lesson("l17", "Simplificar", "Ver la forma simple detrás de lo complejo", "🔍")
                )),
                Module("m1_4", "Proporción", "📐", listOf(
                    Lesson("l18", "Método de cuadrícula", "Copiar usando una grilla", "🔲"),
                    Lesson("l19", "Dividir espacios", "Hallar el punto medio a ojo", "✂️"),
                    Lesson("l20", "Proporciones 1:2:3", "Entender relaciones numéricas", "📊"),
                    Lesson("l21", "Copia exacta", "Reproducir una imagen con precisión", "🎯")
                ))
            )
        ),
        Level("lvl2", 2, "Construcción", "Entender cómo se arman los objetos", "#5BAD6F",
            listOf(
                Module("m2_1", "Volumen y 3D", "📦", listOf(
                    Lesson("l22", "De 2D a 3D", "Agregar profundidad a las formas planas", "📦"),
                    Lesson("l23", "Cubo en perspectiva", "El cubo como base de todo", "🎲"),
                    Lesson("l24", "Esfera y cilindro", "Sólidos curvos básicos", "🌐"),
                    Lesson("l25", "Cono y pirámide", "Sólidos con vértice", "🔺"),
                    Lesson("l26", "Combinar sólidos", "Objetos complejos = sólidos simples", "🏗️"),
                    Lesson("l27", "Objetos cotidianos", "Descomponer objetos en sólidos", "☕")
                )),
                Module("m2_2", "Perspectiva", "🏙️", listOf(
                    Lesson("l28", "Línea de horizonte", "La base de toda perspectiva", "🌅"),
                    Lesson("l29", "Un punto de fuga", "Perspectiva frontal", "🛤️"),
                    Lesson("l30", "Dos puntos de fuga", "Perspectiva de esquina", "🏢"),
                    Lesson("l31", "Perspectiva a ojo", "Sin reglas ni instrumentos", "👁️"),
                    Lesson("l32", "Errores comunes", "Identificar y corregir errores", "❌")
                )),
                Module("m2_3", "Luz y sombra", "💡", listOf(
                    Lesson("l33", "Fuente de luz", "Dirección e intensidad de la luz", "☀️"),
                    Lesson("l34", "Sombra propia y proyectada", "Los dos tipos de sombra", "🌑"),
                    Lesson("l35", "Gradiente de valor", "De blanco a negro suavemente", "🔲"),
                    Lesson("l36", "Hatching", "Sombreado con líneas paralelas", "☰"),
                    Lesson("l37", "Blending", "Difuminado suave", "🌫️")
                )),
                Module("m2_4", "Textura", "🪵", listOf(
                    Lesson("l38", "Textura de madera", "Vetas y nudos", "🪵"),
                    Lesson("l39", "Textura de tela", "Pliegues y arrugas", "👘"),
                    Lesson("l40", "Textura de piedra", "Rugosidad y irregularidad", "🪨"),
                    Lesson("l41", "Textura orgánica", "Hojas, agua, piel", "🍃")
                ))
            )
        ),
        Level("lvl3", 3, "Objetos y Naturaleza", "El mundo real en papel", "#E8A838",
            listOf(
                Module("m3_1", "Objetos cotidianos", "🏠", listOf(
                    Lesson("l42", "Taza y vajilla", "Objetos de cocina", "☕"),
                    Lesson("l43", "Silla y muebles", "Perspectiva aplicada", "🪑"),
                    Lesson("l44", "Vehículos simples", "Autos y bicicletas", "🚗"),
                    Lesson("l45", "Comida y bebida", "Naturaleza muerta simple", "🍎")
                )),
                Module("m3_2", "Naturaleza", "🌿", listOf(
                    Lesson("l46", "Hojas y ramas", "Formas orgánicas naturales", "🍃"),
                    Lesson("l47", "Flores detalladas", "Pétalos y estructuras", "🌸"),
                    Lesson("l48", "Árboles completos", "Estructura y follaje", "🌳"),
                    Lesson("l49", "Agua y nubes", "Elementos fluidos", "☁️")
                )),
                Module("m3_3", "Animales", "🐾", listOf(
                    Lesson("l50", "Estructura animal", "El cuerpo animal básico", "🐕"),
                    Lesson("l51", "Mascotas", "Perro y gato detallados", "🐱"),
                    Lesson("l52", "Aves y plumas", "Estructura y textura de aves", "🦅"),
                    Lesson("l53", "Animales salvajes", "León, elefante, oso", "🦁")
                )),
                Module("m3_4", "Arquitectura", "🏛️", listOf(
                    Lesson("l54", "Casas simples", "Arquitectura básica", "🏠"),
                    Lesson("l55", "Edificios urbanos", "Ciudad en perspectiva", "🏢"),
                    Lesson("l56", "Interiores", "Habitaciones y espacios", "🪟")
                ))
            )
        ),
        Level("lvl4", 4, "Figura Humana", "Anatomía y retrato", "#C0392B",
            listOf(
                Module("m4_1", "Cuerpo humano", "🧍", listOf(
                    Lesson("l57", "Proporciones del cuerpo", "8 cabezas de alto", "📏"),
                    Lesson("l58", "Figura de palito", "De esqueleto a cuerpo", "🦴"),
                    Lesson("l59", "Torso y pelvis", "El núcleo del cuerpo", "🫀"),
                    Lesson("l60", "Brazos y piernas", "Extremidades con volumen", "💪"),
                    Lesson("l61", "Manos detalladas", "La parte más difícil", "🤚"),
                    Lesson("l62", "Pies y calzado", "Estructura del pie", "🦶")
                )),
                Module("m4_2", "Retrato", "😊", listOf(
                    Lesson("l63", "Proporciones del rostro", "La cruz del rostro", "😐"),
                    Lesson("l64", "Ojos expresivos", "El elemento más importante", "👁️"),
                    Lesson("l65", "Nariz", "Desde distintos ángulos", "👃"),
                    Lesson("l66", "Boca y labios", "Expresión y detalle", "👄"),
                    Lesson("l67", "Orejas y cabello", "Completar el retrato", "👂"),
                    Lesson("l68", "Expresiones", "Las 6 emociones básicas", "😄")
                )),
                Module("m4_3", "Poses", "🏃", listOf(
                    Lesson("l69", "Pose estática", "Figura parada natural", "🧍"),
                    Lesson("l70", "Gesture drawing", "30 segundos por pose", "⚡"),
                    Lesson("l71", "Poses dinámicas", "Movimiento y acción", "🏃"),
                    Lesson("l72", "Ropa sobre el cuerpo", "Pliegues y caída", "👕")
                ))
            )
        ),
        Level("lvl5", 5, "Estilos y Técnicas", "Tu voz artística propia", "#8E44AD",
            listOf(
                Module("m5_1", "Estilos", "🎨", listOf(
                    Lesson("l73", "Realismo", "Copiar la realidad fielmente", "📸"),
                    Lesson("l74", "Cartoon", "Simplificar con gracia", "🎭"),
                    Lesson("l75", "Manga y anime", "El estilo japonés", "⛩️"),
                    Lesson("l76", "Minimalismo", "Menos es más", "◾"),
                    Lesson("l77", "Sketch urbano", "Dibujo rápido en la calle", "🏙️")
                )),
                Module("m5_2", "Color básico", "🎨", listOf(
                    Lesson("l78", "Teoría del color", "Rueda cromática y armonías", "🌈"),
                    Lesson("l79", "Temperatura de color", "Cálido vs frío", "🌡️"),
                    Lesson("l80", "Color y luz", "Cómo la luz cambia el color", "💡"),
                    Lesson("l81", "Acuarela básica", "Primera capa de agua", "💧")
                )),
                Module("m5_3", "Composición", "🖼️", listOf(
                    Lesson("l82", "Regla de tercios", "El secreto de toda foto/dibujo", "⊞"),
                    Lesson("l83", "Punto focal", "Dónde va la mirada", "🎯"),
                    Lesson("l84", "Composición narrativa", "Contar una historia en un cuadro", "📖")
                ))
            )
        ),
        Level("lvl6", 6, "Proyectos Completos", "Obras de principio a fin", "#FF6B35",
            listOf(
                Module("m6_1", "Retrato completo", "🖼️", listOf(
                    Lesson("l85", "Retrato realista", "Paso a paso completo", "👤"),
                    Lesson("l86", "Retrato estilizado", "Con tu propio estilo", "🎨"),
                    Lesson("l87", "Autorretrato", "El desafío personal", "🪞")
                )),
                Module("m6_2", "Paisaje completo", "🏔️", listOf(
                    Lesson("l88", "Paisaje natural", "Con todos los elementos", "🌄"),
                    Lesson("l89", "Paisaje urbano", "Ciudad detallada", "🌆"),
                    Lesson("l90", "Paisaje nocturno", "Luces y sombras nocturnas", "🌃")
                )),
                Module("m6_3", "Desafíos finales", "🏆", listOf(
                    Lesson("l91", "Copia de obra maestra", "Reproducir un clásico", "🖼️"),
                    Lesson("l92", "Dibujo cronometrado", "30 minutos, una obra", "⏱️"),
                    Lesson("l93", "Obra original libre", "Sin referencia, pura imaginación", "✨"),
                    Lesson("l94", "Portfolio final", "Tu colección de trabajos", "📁")
                ))
            )
        )
    )

    // ── Progreso ──────────────────────────────────────────────

    fun isLessonCompleted(lessonId: String): Boolean =
        prefs.getBoolean("lesson_$lessonId", false)

    fun completeLesson(lessonId: String) {
        if (!isLessonCompleted(lessonId)) {
            prefs.edit().putBoolean("lesson_$lessonId", true).apply()
            addXP(10)
            updateStreak()
        }
    }

    fun getTotalXP(): Int = prefs.getInt("total_xp", 0)

    fun getLevel(): Int {
        val xp = getTotalXP()
        return when {
            xp < 100  -> 1
            xp < 300  -> 2
            xp < 600  -> 3
            xp < 1000 -> 4
            xp < 1500 -> 5
            else      -> 6
        }
    }

    fun getXPProgress(): Float {
        val xp = getTotalXP()
        return when {
            xp < 100  -> xp / 100f
            xp < 300  -> (xp - 100) / 200f
            xp < 600  -> (xp - 300) / 300f
            xp < 1000 -> (xp - 600) / 400f
            xp < 1500 -> (xp - 1000) / 500f
            else      -> 1f
        }
    }

    fun getStreak(): Int = prefs.getInt("streak", 0)

    fun getAllLessons(): List<Lesson> = levels.flatMap { it.modules }.flatMap { it.lessons }

    fun isLessonUnlocked(lessonId: String): Boolean {
        val all = getAllLessons()
        val idx = all.indexOfFirst { it.id == lessonId }
        if (idx <= 0) return true
        return isLessonCompleted(all[idx - 1].id)
    }

    fun getCompletedCount(): Int =
        getAllLessons().count { isLessonCompleted(it.id) }

    private fun addXP(amount: Int) {
        val current = getTotalXP()
        prefs.edit().putInt("total_xp", current + amount).apply()
    }

    private fun updateStreak() {
        val today = java.text.SimpleDateFormat("yyyyMMdd",
            java.util.Locale.getDefault()).format(java.util.Date())
        val lastDay = prefs.getString("last_day", "")
        val streak  = prefs.getInt("streak", 0)
        if (lastDay == today) return
        val yesterday = java.util.Calendar.getInstance().apply {
            add(java.util.Calendar.DAY_OF_YEAR, -1)
        }.let {
            java.text.SimpleDateFormat("yyyyMMdd",
                java.util.Locale.getDefault()).format(it.time)
        }
        val newStreak = if (lastDay == yesterday) streak + 1 else 1
        prefs.edit()
            .putInt("streak", newStreak)
            .putString("last_day", today)
            .apply()
    }
}