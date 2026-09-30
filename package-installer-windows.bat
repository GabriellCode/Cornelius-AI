@echo off
chcp 65001 >nul
title Criador de Instalador Windows (.MSI) - Cornelius.AI

echo =======================================================
echo    CRIADOR DE INSTALADOR WINDOWS (.MSI) - CORNELIUS
echo =======================================================
echo.

if exist "%~dp0Backend\src" (
    echo [1/3] Compilando arquivos Java...
    call "%~dp0build.bat"
    if %errorlevel% neq 0 (
        echo [ERRO] Falha na compilação.
        pause
        exit /b 1
    )
) else (
    echo [1/3] Usando cornelius.jar pré-compilado...
)

echo [2/3] Preparando arquivos de entrada...
if not exist "%~dp0dist" mkdir "%~dp0dist"
if not exist "%~dp0dist\input" mkdir "%~dp0dist\input"
copy /y "%~dp0cornelius.jar" "%~dp0dist\input\cornelius.jar" >nul

echo [3/3] Criando instalador .msi com atalho na Área de Trabalho e Menu Iniciar...
jpackage --type msi ^
  --name "Cornelius" ^
  --app-version "1.0.0" ^
  --vendor "Cornelius.AI" ^
  --input "%~dp0dist\input" ^
  --main-jar "cornelius.jar" ^
  --main-class "com.cornelius.Main" ^
  --java-options "-Xms1024m" ^
  --java-options "-Xmx4096m" ^
  --java-options "-Dfile.encoding=UTF-8" ^
  --icon "%~dp0cornelius.ico" ^
  --win-shortcut ^
  --win-menu ^
  --win-dir-chooser ^
  --dest "%~dp0dist"

if %errorlevel% equ 0 (
    echo.
    echo =======================================================
    echo  [SUCESSO] Instalador Windows gerado com sucesso em:
    echo  -> %~dp0dist\Cornelius-1.0.0.msi
    echo =======================================================
    echo.
    echo Dê 2 cliques no arquivo .msi para instalar o Cornelius no Windows!
) else (
    echo [ERRO] Falha ao gerar o instalador .msi. Verifique se o WiX Toolset ou JDK 21 estao instalados.
)

pause

