@echo off
REM 编译日志工具类的快速测试脚本
echo ================================
echo 编译日志工具类
echo ================================

set SRC_DIR=src\main\java
set LIB_DIR=target\classes
set JME3_LIB=lib\*

echo.
echo 编译日志工具...
javac -d %LIB_DIR% -cp "%JME3_LIB%" ^
    %SRC_DIR%\com\Hecate\util\DuplicateLogFilter.java ^
    %SRC_DIR%\com\Hecate\util\LogInitializer.java

if errorlevel 1 (
    echo 错误：日志工具编译失败！
    pause
    exit /b 1
)

echo [OK] 日志工具编译成功
echo.
echo ================================
echo 日志工具编译成功！✓
echo ================================
echo.
echo 已编译的类：
echo   - DuplicateLogFilter (日志去重过滤器)
echo   - LogInitializer (日志初始化工具)
echo.
echo 提示：这些工具已在Main.java中自动配置
echo       重新编译游戏后即可生效
echo.
pause
