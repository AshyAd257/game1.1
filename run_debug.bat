@echo off
cd /d "%~dp0"
echo === 编译项目 ===
call mvn compile
if %ERRORLEVEL% NEQ 0 (
    echo 编译失败！
    pause
    exit /b 1
)

echo.
echo === 运行游戏 ===
echo 按I键应该可以打开木偶编辑器
echo 控制台会显示按键事件
echo.
call mvn exec:java -Dexec.mainClass="com.Hecate.Main"
pause
