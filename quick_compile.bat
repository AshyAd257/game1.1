@echo off
REM 快速编译测试

echo 正在编译...
cd /d "C:\Users\29232\OneDrive\Desktop\game1(1)"
call "C:\IDEA2024\IntelliJ IDEA 2024.3.6\plugins\maven\lib\maven3\bin\mvn.cmd" compile -DskipTests

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ================================
    echo 编译成功！
    echo ================================
    echo.
    echo 现在可以运行：
    echo 1. list_model_bones.bat - 查看骨骼结构
    echo 2. run_puppet_bone_test.bat - 测试游戏
    echo.
) else (
    echo.
    echo ================================
    echo 编译失败！
    echo ================================
)

pause
