@echo off
REM 清理日志并测试 Puppet 绑定效果

echo ========================================
echo 测试 Puppet 骨骼绑定（清理版）
echo ========================================
echo.

cd /d "C:\Users\29232\OneDrive\Desktop\game1(1)"

echo [1/2] 编译...
call "C:\IDEA2024\IntelliJ IDEA 2024.3.6\plugins\maven\lib\maven3\bin\mvn.cmd" compile -DskipTests -q

if %ERRORLEVEL% NEQ 0 (
    echo 编译失败！
    pause
    exit /b 1
)

echo [2/2] 启动游戏（启用调试输出）...
echo.
echo ====================================
echo 查看以下调试信息：
echo ====================================
echo 1. 绑定成功信息（启动时）
echo 2. 部件位置更新（每秒一次）
echo    - 部件位置坐标
echo    - 缩放比例
echo    - 是否可见
echo.
echo 如果看不到部件，检查：
echo    - 位置是否合理（应该在角色附近）
echo    - 缩放是否太小
echo    - 可见性是否为 true
echo ====================================
echo.

call "C:\IDEA2024\IntelliJ IDEA 2024.3.6\plugins\maven\lib\maven3\bin\mvn.cmd" exec:java -Dexec.mainClass="com.Hecate.Main"

pause

