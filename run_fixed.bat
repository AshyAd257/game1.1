@echo off
REM 重新编译并运行 - 使用正确的骨骼映射

echo ========================================
echo 重新编译并测试 Puppet 骨骼绑定
echo ========================================
echo.

cd /d "C:\Users\29232\OneDrive\Desktop\game1(1)"

echo [1/2] 快速编译...
call "C:\IDEA2024\IntelliJ IDEA 2024.3.6\plugins\maven\lib\maven3\bin\mvn.cmd" compile -DskipTests -q

if %ERRORLEVEL% NEQ 0 (
    echo 编译失败！
    pause
    exit /b 1
)

echo 编译成功！
echo.
echo [2/2] 启动游戏...
echo.
echo ====================================
echo 骨骼映射已修复为：
echo ====================================
echo   Head -> head (小写)
echo   Neck -> neck (小写)
echo   Body -> body (小写)
echo.
echo 现在应该能看到：
echo   ✓ 绑定成功: Head -> head
echo   ✓ 绑定成功: Neck -> neck
echo   ✓ 绑定成功: Body -> body
echo   ✓ 所有部件绑定完成，总数: 3
echo ====================================
echo.

call "C:\IDEA2024\IntelliJ IDEA 2024.3.6\plugins\maven\lib\maven3\bin\mvn.cmd" exec:java -Dexec.mainClass="com.Hecate.Main" -q

pause
