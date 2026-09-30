@echo off
chcp 65001 >nul
title Compilando Cornelius.AI

echo [CORNELIUS] Compilando codigo-fonte Java 21 no Windows...
if not exist "%~dp0Backend\src" (
    if exist "%~dp0cornelius.jar" (
        echo [CORNELIUS] Codigo-fonte nao presente nesta pasta. Usando cornelius.jar existente.
        exit /b 0
    ) else (
        echo [ERRO] Pasta Backend\src nao encontrada!
        pause
        exit /b 1
    )
)
if not exist "%~dp0Backend\bin" mkdir "%~dp0Backend\bin"

dir /s /b "%~dp0Backend\src\main\java\*.java" > "%~dp0sources.txt"

javac -encoding UTF-8 -d "%~dp0Backend\bin" @"%~dp0sources.txt"
if %errorlevel% neq 0 (
    echo [ERRO] Falha na compilacao!
    del "%~dp0sources.txt" >nul 2>&1
    pause
    exit /b 1
)

del "%~dp0sources.txt" >nul 2>&1

echo [CORNELIUS] Criando pacote executavel cornelius.jar...
jar --create --file "%~dp0cornelius.jar" --main-class com.cornelius.Main -C "%~dp0Backend\bin" .

echo [CORNELIUS] cornelius.jar gerado com sucesso!
if exist "%~dp0dist-windows-portable" copy /y "%~dp0cornelius.jar" "%~dp0dist-windows-portable\cornelius.jar" >nul
if exist "%~dp0Cornelius-Windows-Portable\dist-windows-portable" copy /y "%~dp0cornelius.jar" "%~dp0Cornelius-Windows-Portable\dist-windows-portable\cornelius.jar" >nul

