#!/usr/bin/env bash
set -e

# ===================================================================
#  CORNELIUS.AI - DISCORD BOT SERVICE RUNNER (LINUX / POP!_OS)
# ===================================================================

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$DIR"

echo "======================================================="
echo "   CORNELIUS.AI // SERVIÇO DEDICADO DO BOT DISCORD"
echo "======================================================="
echo ""

if [ ! -f "cornelius.jar" ]; then
    echo "[CORNELIUS] Compilando cornelius.jar..."
    ./build.sh
fi

echo "[CORNELIUS] Iniciando Cornelius em modo Servidor Discord 24/7..."
echo "[CORNELIUS] Pronto para receber requisições de celulares e computadores via Discord."
echo "Pressione Ctrl+C para encerrar."
echo ""

java -Xms1024m -Xmx4096m -Dfile.encoding=UTF-8 -jar cornelius.jar --server "$@"

