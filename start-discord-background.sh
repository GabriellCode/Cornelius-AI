#!/usr/bin/env bash
set -e

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$DIR"

PID_FILE="$DIR/.cornelius_discord.pid"
LOG_FILE="$DIR/discord_bot.log"

if [ -f "$PID_FILE" ]; then
    PID=$(cat "$PID_FILE")
    if ps -p "$PID" > /dev/null 2>&1; then
        echo "[CORNELIUS] Bot do Discord já está em execução no fundo (PID: $PID)."
        exit 0
    fi
fi

if [ ! -f "cornelius.jar" ]; then
    ./build.sh
fi

nohup java -Xms1024m -Xmx4096m -Dfile.encoding=UTF-8 -jar cornelius.jar --server > "$LOG_FILE" 2>&1 &
NEW_PID=$!
echo "$NEW_PID" > "$PID_FILE"

echo "======================================================="
echo " [SUCESSO] Cornelius Discord Bot INICIADO em Segundo Plano!"
echo " -> PID: $NEW_PID"
echo " -> Logs em tempo real: tail -f discord_bot.log"
echo " -> Para parar: ./stop-discord-background.sh"
echo "======================================================="

