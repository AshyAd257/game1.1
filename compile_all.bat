@echo off
echo ========================================
echo 编译整个项目
echo ========================================
echo.

cd /d "%~dp0"

set JAVA_HOME=C:\Users\29232\.jdks\ms-17.0.16
set JAVAC="%JAVA_HOME%\bin\javac.exe"
set CP=target\classes;C:\Users\29232\.m2\repository\org\jmonkeyengine\jme3-core\3.5.2-stable\jme3-core-3.5.2-stable.jar;C:\Users\29232\.m2\repository\org\jmonkeyengine\jme3-desktop\3.5.2-stable\jme3-desktop-3.5.2-stable.jar;C:\Users\29232\.m2\repository\com\google\code\gson\gson\2.8.1\gson-2.8.1.jar;C:\Users\29232\.m2\repository\com\google\guava\guava\32.1.2-jre\guava-32.1.2-jre.jar

echo 步骤1: 创建临时文件列表...
dir /s /b src\main\java\*.java > sources.txt

echo.
echo 步骤2: 编译所有Java文件...
%JAVAC% -encoding UTF-8 -cp "%CP%" -d target\classes @sources.txt

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo 编译成功！
    echo ========================================
    echo.
    echo 检查 Main.class...
    if exist target\classes\com\Hecate\Main.class (
        echo [OK] Main.class 已生成
    ) else (
        echo [警告] Main.class 未找到
    )
    echo.
    echo 现在可以运行游戏了：
    echo   方法1: 在 IntelliJ IDEA 中运行 Main.java
    echo   方法2: 运行 run_debug.bat
    echo.
) else (
    echo.
    echo ========================================
    echo 编译失败！
    echo ========================================
    echo.
    echo 请查看上面的错误信息
    echo 或在 IntelliJ IDEA 中使用 Maven 编译
    echo.
)

del sources.txt 2>nul

pause
