@echo off
setlocal
cd /d "%~dp0"
if not exist "target\classes\com\Hecate\puppet\editor\PuppetEditorLauncher.class" (
    echo Please compile first: mvn -Pwith-editor compile
    pause
    exit /b 1
)
if not exist "target\volume-deps\jme3-core-3.5.2-stable.jar" (
    echo Please prepare dependencies: mvn dependency:copy-dependencies -DoutputDirectory=target/volume-deps
    pause
    exit /b 1
)
set "PUPPET_EDITOR_JAVA=java"
if exist "C:\Program Files\Java\jdk-17\bin\java.exe" set "PUPPET_EDITOR_JAVA=C:\Program Files\Java\jdk-17\bin\java.exe"
"%PUPPET_EDITOR_JAVA%" -cp "target\classes;target\volume-deps\*" com.Hecate.puppet.editor.PuppetEditorLauncher %*
if errorlevel 1 pause
endlocal
