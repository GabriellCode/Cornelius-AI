@echo off
chcp 65001 >nul
title Cornelius.AI - Discord Bot Dedicated Service

echo =======================================================
echo    CORNELIUS.AI // SERVICO DEDICADO DO BOT DISCORD
echo =======================================================
echo.

if not exist "%~dp0cornelius.jar" (
    echo [CORNELIUS] Compilando cornelius.jar...
    call "%~dp0build.bat"
)

echo [CORNELIUS] Iniciando Cornelius em modo Servidor Discord...
echo [CORNELIUS] Recebendo mensagens do celular e servidores pelo Discord.
echo Pressione Ctrl+C para encerrar.
echo.

java -Xms1024m -Xmx4096m -Dfile.encoding=UTF-8 -jar "%~dp0cornelius.jar" --server %*

