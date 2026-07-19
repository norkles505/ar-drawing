content = """<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="#000000">

    <androidx.camera.view.PreviewView
        android:id="@+id/previewView"
        android:layout_width="0dp"
        android:layout_height="0dp"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent" />

    <com.ardrawing.app.camera.OverlayView
        android:id="@+id/overlayView"
        android:layout_width="0dp"
        android:layout_height="0dp"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent" />

    <com.ardrawing.app.camera.RulerView
        android:id="@+id/rulerView"
        android:layout_width="0dp"
        android:layout_height="0dp"
        android:visibility="gone"
        android:elevation="4dp"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent" />

    <LinearLayout
        android:id="@+id/topBar"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:padding="8dp"
        android:background="#66000000"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent">

        <ImageButton
            android:id="@+id/btnBack"
            android:layout_width="44dp"
            android:layout_height="44dp"
            android:src="@drawable/ic_arrow_back"
            android:background="?attr/selectableItemBackgroundBorderless"
            android:contentDescription="Atras"
            android:tint="#FFFFFF" />

        <ImageButton
            android:id="@+id/btnFlash"
            android:layout_width="44dp"
            android:layout_height="44dp"
            android:src="@drawable/ic_flash"
            android:background="?attr/selectableItemBackgroundBorderless"
            android:contentDescription="Flash"
            android:tint="#FFFFFF"
            android:alpha="0.5" />

        <Space
            android:layout_width="0dp"
            android:layout_height="0dp"
            android:layout_weight="1" />

        <ImageButton
            android:id="@+id/btnSaveProject"
            android:layout_width="44dp"
            android:layout_height="44dp"
            android:src="@drawable/ic_save"
            android:background="?attr/selectableItemBackgroundBorderless"
            android:contentDescription="Guardar proyecto"
            android:tint="#FFD700" />

    </LinearLayout>

    <LinearLayout
        android:id="@+id/controlsPanel"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="12dp"
        android:background="#CC000000"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:gravity="center_vertical"
            android:layout_marginBottom="8dp">

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="O"
                android:textColor="#FFFFFF"
                android:textSize="18sp"
                android:layout_marginEnd="8dp" />

            <SeekBar
                android:id="@+id/seekbarOpacity"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:max="100"
                android:progress="50"
                android:progressTint="#FFFFFF"
                android:thumbTint="#FFFFFF" />

        </LinearLayout>

        <LinearLayout
            android:id="@+id/brightnessPanel"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:visibility="gone"
            android:layout_marginBottom="8dp">

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="horizontal"
                android:gravity="center_vertical"
                android:layout_marginBottom="4dp">

                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="Brillo"
                    android:textSize="12sp"
                    android:textColor="#FFFFFF"
                    android:layout_marginEnd="8dp" />

                <SeekBar
                    android:id="@+id/seekbarBrightness"
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_weight="1"
                    android:max="200"
                    android:progress="100"
                    android:progressTint="#FFFFFF"
                    android:thumbTint="#FFFFFF" />

            </LinearLayout>

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="horizontal"
                android:gravity="center_vertical">

                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="Contraste"
                    android:textSize="12sp"
                    android:textColor="#FFFFFF"
                    android:layout_marginEnd="8dp" />

                <SeekBar
                    android:id="@+id/seekbarContrast"
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_weight="1"
                    android:max="200"
                    android:progress="100"
                    android:progressTint="#FFFFFF"
                    android:thumbTint="#FFFFFF" />

            </LinearLayout>

        </LinearLayout>

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:gravity="center"
            android:layout_marginBottom="4dp">

            <ImageButton
                android:id="@+id/btnGrid"
                style="@style/ToolButton"
                android:src="@drawable/ic_grid"
                android:contentDescription="Cuadricula"
                android:alpha="0.5" />

            <ImageButton
                android:id="@+id/btnMirror"
                style="@style/ToolButton"
                android:src="@drawable/ic_flip"
                android:contentDescription="Espejo"
                android:alpha="0.5" />

            <ImageButton
                android:id="@+id/btnRotate"
                style="@style/ToolButton"
                android:src="@drawable/ic_rotate"
                android:contentDescription="Rotar"
                android:alpha="1.0" />

            <ImageButton
                android:id="@+id/btnLock"
                style="@style/ToolButton"
                android:src="@drawable/ic_lock"
                android:contentDescription="Bloquear"
                android:alpha="0.5" />

            <ImageButton
                android:id="@+id/btnReset"
                style="@style/ToolButton"
                android:src="@drawable/ic_reset"
                android:contentDescription="Reset"
                android:alpha="1.0" />

            <ImageButton
                android:id="@+id/btnRuler"
                style="@style/ToolButton"
                android:src="@drawable/ic_ruler"
                android:contentDescription="Regla"
                android:alpha="0.5" />

            <ImageButton
                android:id="@+id/btnBrightness"
                style="@style/ToolButton"
                android:src="@drawable/ic_brightness"
                android:contentDescription="Brillo"
                android:alpha="0.5" />

        </LinearLayout>

    </LinearLayout>

    <ImageButton
        android:id="@+id/btnToggleControls"
        android:layout_width="40dp"
        android:layout_height="40dp"
        android:layout_marginEnd="12dp"
        android:layout_marginBottom="4dp"
        android:src="@android:drawable/arrow_down_float"
        android:background="#88000000"
        android:tint="#FFFFFF"
        android:contentDescription="Toggle controles"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintBottom_toTopOf="@id/controlsPanel" />

    <TextView
        android:id="@+id/tvImageSize"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:padding="6dp"
        android:textColor="#FFFFFF"
        android:textSize="13sp"
        android:background="#88000000"
        android:visibility="gone"
        android:elevation="5dp"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        android:layout_marginTop="60dp"
        android:layout_marginEnd="12dp" />

</androidx.constraintlayout.widget.ConstraintLayout>"""

with open(r'app\src\main\res\layout\activity_drawing.xml', 'w', encoding='utf-8') as f:
    f.write(content)
print('OK')