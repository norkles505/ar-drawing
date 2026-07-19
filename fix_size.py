content = """<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="#FFFFFF"
        android:pathData="M21,6H3C1.9,6 1,6.9 1,8v8c0,1.1 0.9,2 2,2h18c1.1,0 2,-0.9 2,-2V8C23,6.9 22.1,6 21,6zM21,16H3V8h18V16zM5,15h2v-2H5V15zM5,11h2V9H5V11zM9,15h6v-2H9V15zM9,11h6V9H9V11zM17,15h2v-2h-2V15zM17,11h2V9h-2V11z"/>
</vector>"""

with open(r'app\src\main\res\drawable\ic_size.xml', 'w', encoding='utf-8') as f:
    f.write(content)
print('OK')