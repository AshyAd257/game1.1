@echo off
REM 木偶编辑器独立启动脚本
REM 此脚本可以独立启动编辑器，不需要运行游戏主程序

echo ========================================
echo    Puppet Editor Launcher
echo ========================================
echo.

REM 检查Java环境
where java >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Java not found in PATH
    echo Please install Java 17 or higher
    pause
    exit /b 1
)

REM 显示Java版本
echo Java version:
java -version
echo.

REM 设置类路径
set CP=target/classes
set CP=%CP%;C:/Users/29232/.m2/repository/org/jmonkeyengine/jme3-core/3.5.2-stable/jme3-core-3.5.2-stable.jar
set CP=%CP%;C:/Users/29232/.m2/repository/org/jmonkeyengine/jme3-desktop/3.5.2-stable/jme3-desktop-3.5.2-stable.jar
set CP=%CP%;C:/Users/29232/.m2/repository/org/jmonkeyengine/jme3-lwjgl3/3.5.2-stable/jme3-lwjgl3-3.5.2-stable.jar
set CP=%CP%;C:/Users/29232/.m2/repository/com/google/code/gson/gson/2.8.1/gson-2.8.1.jar
set CP=%CP%;C:/Users/29232/.m2/repository/com/google/guava/guava/32.1.2-jre/guava-32.1.2-jre.jar

REM 检查编译文件是否存在
if not exist "target\classes\com\Hecate\puppet\editor\PuppetEditorApp.class" (
    echo [ERROR] Puppet editor not compiled
    echo Please run: mvn compile
    pause
    exit /b 1
)

echo Starting Puppet Editor...
echo.

REM 启动编辑器
java -cp "%CP%" com.Hecate.puppet.editor.PuppetEditorLauncher

echo.
echo Editor closed.
pause
