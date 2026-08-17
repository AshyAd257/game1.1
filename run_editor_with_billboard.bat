@echo off
echo ========================================
echo 木偶编辑器 - Billboard模式测试
echo ========================================
echo.
echo 按 B键 可以切换Billboard模式:
echo   - DISABLED (3D立体模式)
echo   - UNIFIED (纸人模式)
echo   - INDEPENDENT (独立旋转,不推荐)
echo.
echo 当前默认: DISABLED (适合制作立方体等3D图形)
echo ========================================
echo.

"C:\Users\29232\.jdks\ms-17.0.16\bin\java.exe" -cp "target/classes;C:\Users\29232\.m2\repository\org\jmonkeyengine\jme3-core\3.5.2-stable\jme3-core-3.5.2-stable.jar;C:\Users\29232\.m2\repository\org\jmonkeyengine\jme3-desktop\3.5.2-stable\jme3-desktop-3.5.2-stable.jar;C:\Users\29232\.m2\repository\org\jmonkeyengine\jme3-lwjgl3\3.5.2-stable\jme3-lwjgl3-3.5.2-stable.jar;C:\Users\29232\.m2\repository\com\google\code\gson\gson\2.8.1\gson-2.8.1.jar;C:\Users\29232\.m2\repository\com\google\guava\guava\32.1.2-jre\guava-32.1.2-jre.jar;C:\Users\29232\.m2\repository\org\lwjgl\lwjgl\3.3.1\lwjgl-3.3.1.jar;C:\Users\29232\.m2\repository\org\lwjgl\lwjgl-glfw\3.3.1\lwjgl-glfw-3.3.1.jar;C:\Users\29232\.m2\repository\org\lwjgl\lwjgl-opengl\3.3.1\lwjgl-opengl-3.3.1.jar;C:\Users\29232\.m2\repository\org\lwjgl\lwjgl-openal\3.3.1\lwjgl-openal-3.3.1.jar;C:\Users\29232\.m2\repository\org\lwjgl\lwjgl\3.3.1\lwjgl-3.3.1-natives-windows.jar;C:\Users\29232\.m2\repository\org\lwjgl\lwjgl-glfw\3.3.1\lwjgl-glfw-3.3.1-natives-windows.jar;C:\Users\29232\.m2\repository\org\lwjgl\lwjgl-opengl\3.3.1\lwjgl-opengl-3.3.1-natives-windows.jar;C:\Users\29232\.m2\repository\org\lwjgl\lwjgl-openal\3.3.1\lwjgl-openal-3.3.1-natives-windows.jar" com.Hecate.puppet.editor.PuppetEditorApp

pause
