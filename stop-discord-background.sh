#!/usr/bin/env bash
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$DIR"

PID_FILE="$DIR/.cornelius_discord.pid"

if [ -f "$PID_FILE" ]; then
    PID=$(cat "$PID_FILE")
    if ps -p "$PID" > /dev/null 2>&1; then
        echo "[CORNELIUS] Encerrando bot do Discord (PID: $PID)..."
        kill "$PID"
        rm -f "$PID_FILE"
        echo "[CORNELIUS] Bot do Discord parado com sucesso."
        exit 0
    else
        rm -f "$PID_FILE"
        echo "[CORNELIUS] Processo não encontrado. Arquivo PID limpo."
    fi
else
    echo "[CORNELIUS] Nenhum bot do Discord em execução em segundo plano."
fi

