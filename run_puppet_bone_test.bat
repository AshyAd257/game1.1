@echo off
REM 编译并运行游戏测试 Puppet 骨骼绑定系统

echo ========================================
echo 编译并启动游戏 - Puppet 骨骼绑定测试
echo ========================================
echo.

cd /d "C:\Users\29232\OneDrive\Desktop\game1(1)"

echo [1/3] 清理旧的编译文件...
call "C:\IDEA2024\IntelliJ IDEA 2024.3.6\plugins\maven\lib\maven3\bin\mvn.cmd" clean -q

echo [2/3] 编译项目...
call "C:\IDEA2024\IntelliJ IDEA 2024.3.6\plugins\maven\lib\maven3\bin\mvn.cmd" compile -DskipTests

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ================================
    echo 编译失败！请检查上面的错误信息。
    echo ================================
    pause
    exit /b 1
)

echo.
echo 编译成功！
echo.
echo [3/3] 启动游戏...
echo.
echo ====================================
echo 预期效果：
echo ====================================
echo 1. 控制台会输出骨骼绑定信息：
echo    - "找到骨架，骨骼数量: X"
echo    - "骨骼 0: XXX" (列出所有骨骼名)
echo    - "绑定成功: Head -> Head"
echo    - "所有部件绑定完成，总数: 3"
echo.
echo 2. 游戏中会看到：
echo    - 3D 人物模型
echo    - Puppet 面部附加在头部骨骼上
echo    - Puppet 身体附加在脊柱骨骼上
echo    - Puppet 脖子附加在脖子骨骼上
echo.
echo 3. 部件会随骨骼动画移动和旋转
echo.
echo ====================================
echo 注意事项：
echo ====================================
echo - 首次运行请查看控制台输出的骨骼名称
echo - 如果绑定失败，会提示 "未找到骨骼 XXX"
echo - 根据实际骨骼名调整代码中的映射
echo.
echo 按 Ctrl+C 可以退出游戏
echo ====================================
echo.

call "C:\IDEA2024\IntelliJ IDEA 2024.3.6\plugins\maven\lib\maven3\bin\mvn.cmd" exec:java -Dexec.mainClass="com.Hecate.Main"

echo.
echo 游戏已退出
pause
