@echo off
REM 清理版启动脚本 - 只显示关键信息

echo ========================================
echo Puppet 骨骼绑定测试（清理版）
echo ========================================
echo.

cd /d "C:\Users\29232\OneDrive\Desktop\game1(1)"

echo [1/2] 编译中...
call "C:\IDEA2024\IntelliJ IDEA 2024.3.6\plugins\maven\lib\maven3\bin\mvn.cmd" compile -DskipTests -q

if %ERRORLEVEL% NEQ 0 (
    echo 编译失败！
    pause
    exit /b 1
)

echo [2/2] 启动游戏...
echo.
echo ====================================
echo 已禁用的输出：
echo ====================================
echo ✓ GLTF 插值警告（已过滤）
echo ✓ InputManager 警告（已过滤）
echo ✓ 模型旋转调试（已禁用）
echo ✓ 部件位置重复输出（只显示一次）
echo.
echo ====================================
echo 关注启动时的输出：
echo ====================================
echo - 绑定成功: Body -> body
echo - 绑定成功: Neck -> neck
echo - 绑定成功: Head -> head
echo - 首次更新 - 部件 body 位置: ...
echo - 首次更新 - 部件 neck 位置: ...
echo - 首次更新 - 部件 head 位置: ...
echo ====================================
echo.

call "C:\IDEA2024\IntelliJ IDEA 2024.3.6\plugins\maven\lib\maven3\bin\mvn.cmd" exec:java -Dexec.mainClass="com.Hecate.Main"

pause
