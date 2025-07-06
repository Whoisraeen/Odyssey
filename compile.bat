@echo off
for /r src %%f in (*.java) do (
    echo Compiling %%f
    javac -cp "lib/*" "%%f" -d bin
    if errorlevel 1 (
        echo Error compiling %%f
        exit /b 1
    )
)
echo Compilation completed successfully!