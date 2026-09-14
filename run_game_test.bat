@echo off
REM 编译并运行游戏以测试新的 defaultChara1 人物模型

echo ====================================
echo 编译并启动游戏测试新人物模型
echo ====================================
echo.

cd /d "C:\Users\29232\OneDrive\Desktop\game1(1)"

echo [1/2] 编译项目...
call "C:\IDEA2024\IntelliJ IDEA 2024.3.6\plugins\maven\lib\maven3\bin\mvn.cmd" clean compile -DskipTests -q

if %ERRORLEVEL% NEQ 0 (
    echo 编译失败！
    pause
    exit /b 1
)

echo 编译成功！
echo.
echo [2/2] 启动游戏...
echo.
echo 提示：游戏启动后，新的人物模型应该会显示：
echo   - 使用旋转条状贴图的面部
echo   - 使用旋转条状贴图的身体
echo   - 脖子部件连接头和身体
echo.
echo 按 Ctrl+C 退出游戏
echo.

call "C:\IDEA2024\IntelliJ IDEA 2024.3.6\plugins\maven\lib\maven3\bin\mvn.cmd" exec:java -Dexec.mainClass="com.Hecate.Main" -q

pause
