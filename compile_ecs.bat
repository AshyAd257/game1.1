@echo off
REM 编译ECS核心类的快速测试脚本
echo ================================
echo 编译ECS核心类
echo ================================

set SRC_DIR=src\main\java
set LIB_DIR=target\classes
set JME3_LIB=lib\*

echo.
echo [1/3] 编译核心接口和基类...
javac -d %LIB_DIR% -cp "%JME3_LIB%" ^
    %SRC_DIR%\com\Hecate\puppet\ecs\Component.java ^
    %SRC_DIR%\com\Hecate\puppet\ecs\Entity.java ^
    %SRC_DIR%\com\Hecate\puppet\ecs\System.java

if errorlevel 1 (
    echo 错误：核心类编译失败！
    pause
    exit /b 1
)

echo [OK] 核心接口和基类编译成功
echo.
echo [2/3] 编译管理器和组件...
javac -d %LIB_DIR% -cp "%LIB_DIR%;%JME3_LIB%" ^
    %SRC_DIR%\com\Hecate\puppet\ecs\ComponentManager.java ^
    %SRC_DIR%\com\Hecate\puppet\ecs\SystemManager.java ^
    %SRC_DIR%\com\Hecate\puppet\ecs\component\TransformComponent.java ^
    %SRC_DIR%\com\Hecate\puppet\ecs\component\HierarchyComponent.java

if errorlevel 1 (
    echo 错误：管理器和组件编译失败！
    pause
    exit /b 1
)

echo [OK] 管理器和组件编译成功
echo.
echo [3/3] 编译实体和系统...
javac -d %LIB_DIR% -cp "%LIB_DIR%;%JME3_LIB%" ^
    %SRC_DIR%\com\Hecate\puppet\ecs\entity\BoneEntity.java ^
    %SRC_DIR%\com\Hecate\puppet\ecs\system\TransformSystem.java

if errorlevel 1 (
    echo 错误：实体和系统编译失败！
    pause
    exit /b 1
)

echo [OK] 实体和系统编译成功
echo.
echo ================================
echo 所有ECS核心类编译成功！✓
echo ================================
echo.
echo 已编译的类：
echo   - Component, Entity, System (接口)
echo   - ComponentManager, SystemManager
echo   - TransformComponent, HierarchyComponent
echo   - BoneEntity, TransformSystem
echo.
pause
