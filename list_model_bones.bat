@echo off
REM 列出 armlegmesh.glb 模型的所有骨骼名称

echo ========================================
echo 查看 3D 模型骨骼结构
echo ========================================
echo.

cd /d "C:\Users\29232\OneDrive\Desktop\game1(1)"

echo [1/2] 编译工具...
call "C:\IDEA2024\IntelliJ IDEA 2024.3.6\plugins\maven\lib\maven3\bin\mvn.cmd" compile -DskipTests -q

if %ERRORLEVEL% NEQ 0 (
    echo 编译失败！
    pause
    exit /b 1
)

echo [2/2] 运行骨骼查看工具...
echo.

call "C:\IDEA2024\IntelliJ IDEA 2024.3.6\plugins\maven\lib\maven3\bin\mvn.cmd" exec:java -Dexec.mainClass="com.Hecate.tools.ListModelBones" -q

echo.
pause
