arbol   (tree /f):

El número de serie del volumen es A2F0-7254
C:.
│   AndroidManifest.xml
│   
├───java
│   └───com
│       └───ardrawing
│           └───app
│               ├───camera
│               │       OverlayView.kt
│               │       RulerView.kt
│               │       
│               ├───data
│               │       ProjectManager.kt
│               │       
│               ├───gallery
│               │       DrawingItem.kt
│               │       GalleryAdapter.kt
│               │       ProjectsAdapter.kt
│               │       
│               └───ui
│                       DrawingActivity.kt
│                       GalleryActivity.kt
│                       ProjectsActivity.kt
│                       SplashActivity.kt
│                       
├───res
│   ├───drawable
│   │       bg_circle_dark.xml
│   │       bg_controls_panel.xml
│   │       bg_tool_button.xml
│   │       ic_add_photo.xml
│   │       ic_arrow_back.xml
│   │       ic_brightness.xml
│   │       ic_flash.xml
│   │       ic_flip.xml
│   │       ic_grid.xml
│   │       ic_lock.xml
│   │       ic_pencil.xml
│   │       ic_reset.xml
│   │       ic_rotate.xml
│   │       ic_ruler.xml
│   │       ic_save.xml
│   │       sample_butterfly.png
│   │       sample_cat.png
│   │       sample_cityscape.png
│   │       sample_dog.png
│   │       sample_dragon.png
│   │       sample_house.png
│   │       sample_manga.png
│   │       sample_portrait.png
│   │       sample_rose.png
│   │       sample_skull.png
│   │       sample_tree.png
│   │       sample_wolf.png
│   │       
│   ├───layout
│   │       activity_drawing.xml
│   │       activity_gallery.xml
│   │       activity_projects.xml
│   │       activity_splash.xml
│   │       item_gallery.xml
│   │       item_project.xml
│   │       
│   ├───mipmap-hdpi
│   │       ic_launcher.png
│   │       ic_launcher_round.png
│   │       
│   ├───mipmap-xhdpi
│   │       ic_launcher.png
│   │       ic_launcher_round.png
│   │       
│   ├───mipmap-xxhdpi
│   │       ic_launcher.png
│   │       ic_launcher_round.png
│   │       
│   ├───mipmap-xxxhdpi
│   │       ic_launcher.png
│   │       ic_launcher_round.png
│   │       
│   └───values
│           colors.xml
│           strings.xml
│           styles.xml
│           themes.xml
│           
└───{java
    └───com
        └───ardrawing
            └───app
                └───{ui,camera,gallery,utils},res
                    └───{layout,drawable,values,menu,mipmap-hdpi,mipmap-xhdpi,mipmap-xxhdpi,raw}}
PS C:\Users\HP\Desktop\ARDrawing\app\src\main> 



Direcciones

app/src/main/java/com/ardrawing/app/

app/src/main/res/drawable/

app/src/main/res/

app/src/main/



Ejecutar complilar e instalar

.\gradlew.bat assembleDebug


C:\Android\platform-tools\adb.exe install -r app\build\outputs\apk\debug\app-debug.apk

C:\Android\platform-tools\adb.exe logcat --pid=$(C:\Android\platform-tools\adb.exe shell pidof com ardrawing.app)



.\gradlew.bat compileDebugKotlin 2>&1 | Select-String "error:"