#!/usr/bin/env bash
set -e

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$DIR"

echo "======================================================="
echo "   CORNELIUS.AI // DESATIVAR INICIALIZAÇÃO COM O LINUX"
echo "======================================================="

# Parar e desabilitar systemd service
if systemctl --user is-enabled cornelius.service >/dev/null 2>&1; then
    systemctl --user stop cornelius.service || true
    systemctl --user disable cornelius.service || true
    rm -f "$HOME/.config/systemd/user/cornelius.service"
    systemctl --user daemon-reload
    echo "[CORNELIUS] Serviço systemd removido."
fi

# Remover XDG autostart
if [ -f "$HOME/.config/autostart/cornelius.desktop" ]; then
    rm -f "$HOME/.config/autostart/cornelius.desktop"
    echo "[CORNELIUS] Entrada XDG Autostart removida."
fi

echo "[SUCESSO] Inicialização automática desativada."

