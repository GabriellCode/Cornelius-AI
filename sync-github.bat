@echo off
chcp 65001 >nul
title Sincronizando com GitHub - Cornelius.AI

echo ===================================================
echo [CORNELIUS] Sincronizando projeto com o GitHub...
echo ===================================================

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0sync-github.ps1" %*

if %errorlevel% equ 0 (
    echo.
    echo [SUCESSO] Repositorio GitHub atualizado!
) else (
    echo.
    echo [ERRO] Ocorreu uma falha na sincronizacao. Veja os logs em github_sync.log.
)

if "%~1"=="" (
    timeout /t 4 >nul
)
