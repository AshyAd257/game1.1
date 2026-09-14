@echo off
REM 编译并运行游戏以测试新的 defaultChara1 人物模型
REM 更新：已集成 PuppetPlayerController 到游戏中

echo ====================================
echo 编译并启动游戏测试新人物模型 v2
echo ====================================
echo.

cd /d "C:\Users\29232\OneDrive\Desktop\game1(1)"

echo [1/3] 清理旧的编译文件...
call "C:\IDEA2024\IntelliJ IDEA 2024.3.6\plugins\maven\lib\maven3\bin\mvn.cmd" clean -q

echo [2/3] 编译项目（包含 Puppet 集成）...
call "C:\IDEA2024\IntelliJ IDEA 2024.3.6\plugins\maven\lib\maven3\bin\mvn.cmd" compile -DskipTests -q

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ================================
    echo 编译失败！请检查错误信息。
    echo ================================
    pause
    exit /b 1
)

echo 编译成功！
echo.
echo [3/3] 启动游戏...
echo.
echo ====================================
echo 预期效果：
echo ====================================
echo 1. 游戏启动后会在玩家位置显示 puppet 人物
echo 2. 人物包含三个部件：
echo    - 头部：使用 deafultface1.png（旋转条贴图）
echo    - 脖子：使用 neck1.png
echo    - 身体：使用 body1.png（旋转条贴图）
echo 3. 当你转动视角时，人物会自动旋转显示不同角度
echo 4. 控制台会输出："Puppet 人物控制器已初始化"
echo.
echo ====================================
echo 故障排除：
echo ====================================
echo - 如果看不到人物，检查控制台是否有报错
echo - 如果贴图显示为紫色，说明贴图加载失败
echo - 按 F3 可能会显示调试信息（如果游戏支持）
echo.
echo 按 Ctrl+C 退出游戏
echo ====================================
echo.

call "C:\IDEA2024\IntelliJ IDEA 2024.3.6\plugins\maven\lib\maven3\bin\mvn.cmd" exec:java -Dexec.mainClass="com.Hecate.Main"

pause
