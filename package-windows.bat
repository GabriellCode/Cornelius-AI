@echo off
chcp 65001 >nul
title Empacotador Nativo Cornelius.AI para Windows (.EXE)

echo =======================================================
echo    GERADOR DE APLICATIVO NATIVO WINDOWS (.EXE)
echo =======================================================
echo.

if exist "%~dp0Backend\src" (
    echo [1/3] Compilando arquivos Java e empacotando cornelius.jar...
    call "%~dp0build.bat"
    if %errorlevel% neq 0 (
        echo [ERRO] Falha ao compilar cornelius.jar.
        pause
        exit /b 1
    )
) else (
    echo [1/3] Usando cornelius.jar pré-compilado...
)

echo [2/3] Preparando pasta de distribuição...
if not exist "%~dp0dist" mkdir "%~dp0dist"
if not exist "%~dp0dist\input" mkdir "%~dp0dist\input"
copy /y "%~dp0cornelius.jar" "%~dp0dist\input\cornelius.jar" >nul

echo [3/3] Criando executável nativo independente Cornelius.exe com jpackage...
if exist "%~dp0cornelius.ico" (
    jpackage --type app-image ^
      --name "Cornelius" ^
      --input "%~dp0dist\input" ^
      --main-jar "cornelius.jar" ^
      --main-class "com.cornelius.Main" ^
      --java-options "-Xms1024m" ^
      --java-options "-Xmx4096m" ^
      --java-options "-Dfile.encoding=UTF-8" ^
      --icon "%~dp0cornelius.ico" ^
      --dest "%~dp0dist"
) else (
    jpackage --type app-image ^
      --name "Cornelius" ^
      --input "%~dp0dist\input" ^
      --main-jar "cornelius.jar" ^
      --main-class "com.cornelius.Main" ^
      --java-options "-Xms1024m" ^
      --java-options "-Xmx4096m" ^
      --java-options "-Dfile.encoding=UTF-8" ^
      --dest "%~dp0dist"
)

if %errorlevel% equ 0 (
    echo.
    echo =======================================================
    echo  [SUCESSO] Seu aplicativo nativo Windows foi criado em:
    echo  -> %~dp0dist\Cornelius\Cornelius.exe
    echo =======================================================
    echo.
    echo Basta abrir a pasta 'dist\Cornelius' e dar 2 cliques no Cornelius.exe!
    echo Ele funciona de forma 100%% independente e portatil.
) else (
    echo [ERRO] Nao foi possivel executar o jpackage. Verifique se o JDK 21 esta instalado e configurado no PATH.
)

pause
