@echo off
chcp 65001 >nul
title Cornelius.AI - Sincronizacao GitHub (100% Java 21)

echo ===================================================
echo [CORNELIUS] Sincronizacao Nativa em Java 21...
echo ===================================================

java -Dfile.encoding=UTF-8 -cp "%~dp0cornelius.jar" com.cornelius.system.GitHubSyncEngine %*

if %errorlevel% equ 0 (
    echo.
    echo [SUCESSO] Repositorio GitHub sincronizado com sucesso via Java 21!
) else (
    echo.
    echo [ERRO] Ocorreu uma falha. Consulte os logs em github_sync.log.
)

if "%~1"=="" (
    timeout /t 3 >nul
)
