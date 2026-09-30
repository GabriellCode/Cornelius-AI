@echo off
chcp 65001 >nul
title Cornelius.AI - Autonomous Personal Butler

echo [CORNELIUS] Verificando ambiente Java 21...
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERRO] Java nao foi encontrado no seu Windows!
    echo Por favor, instale o Java 21 LTS: https://adoptium.net/temurin/releases/?version=21
    pause
    exit /b 1
)

if not exist "%~dp0cornelius.jar" (
    echo [CORNELIUS] Executavel cornelius.jar nao encontrado. Compilando...
    call "%~dp0build.bat"
)

echo [CORNELIUS] Iniciando Cornelius.AI no Windows (JVM Heap: 1GB - 4GB)...
start "" javaw -Xms1024m -Xmx4096m -Dfile.encoding=UTF-8 -jar "%~dp0cornelius.jar" %*
exit

