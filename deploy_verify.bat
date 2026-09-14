@echo off
REM ========================================
REM 部署验证脚本
REM 验证新ECS系统是否正常工作
REM ========================================

cd /d "%~dp0"

set JAVA_HOME=C:\Users\29232\.jdks\ms-17.0.16
set JAVA="%JAVA_HOME%\bin\java.exe"
set CP=target\classes;C:\Users\29232\.m2\repository\org\jmonkeyengine\jme3-core\3.5.2-stable\jme3-core-3.5.2-stable.jar;C:\Users\29232\.m2\repository\org\jmonkeyengine\jme3-desktop\3.5.2-stable\jme3-desktop-3.5.2-stable.jar;C:\Users\29232\.m2\repository\com\google\code\gson\gson\2.8.1\gson-2.8.1.jar;C:\Users\29232\.m2\repository\com\google\guava\guava\32.1.2-jre\guava-32.1.2-jre.jar

echo.
echo ========================================
echo 部署验证 - 步骤1/3
echo 测试ECS性能
echo ========================================
echo.

%JAVA% -cp "%CP%" com.Hecate.puppet.ecs.benchmark.PerformanceBenchmark

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [错误] ECS性能测试失败
    pause
    exit /b 1
)

echo.
echo ========================================
echo 部署验证 - 步骤2/3
echo 测试动画状态机
echo ========================================
echo.

%JAVA% -cp "%CP%" com.Hecate.puppet.animation2.example.AnimationStateMachineExample

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [错误] 动画状态机测试失败
    pause
    exit /b 1
)

echo.
echo ========================================
echo 部署验证 - 步骤3/3
echo 测试兼容性转换
echo ========================================
echo.

%JAVA% -cp "%CP%" com.Hecate.puppet.compat.example.CompatibilityExample

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [错误] 兼容性测试失败
    pause
    exit /b 1
)

echo.
echo ========================================
echo 部署验证完成！✓
echo ========================================
echo.
echo 所有测试通过：
echo   ✓ ECS性能测试
echo   ✓ 动画状态机测试
echo   ✓ 兼容性转换测试
echo.
echo 新系统已就绪，可以开始使用！
echo.
pause
