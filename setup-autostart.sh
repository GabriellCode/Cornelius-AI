#!/usr/bin/env bash
set -e

# ===================================================================
#  CORNELIUS.AI - CONFIGURAÇÃO DE INICIALIZAÇÃO AUTOMÁTICA NO BOOT
# ===================================================================

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$DIR"

echo "======================================================="
echo "   CORNELIUS.AI // CONFIGURAÇÃO DE INÍCIO COM O LINUX"
echo "======================================================="
echo ""

# 1. Garantir que o jar está compilado e atualizado
if [ ! -f "$DIR/cornelius.jar" ]; then
    echo "[CORNELIUS] Compilando cornelius.jar..."
    ./build.sh
fi

# 2. Criar diretórios de configuração do usuário se não existirem
mkdir -p "$HOME/.config/autostart"
mkdir -p "$HOME/.config/systemd/user"

# 3. Criar arquivo de serviço Systemd para inicialização em segundo plano
SYSTEMD_SERVICE="$HOME/.config/systemd/user/cornelius.service"
cat <<EOF > "$SYSTEMD_SERVICE"
[Unit]
Description=Cornelius AI - Mordomo Pessoal de Inteligência Artificial
After=network.target network-online.target
Wants=network-online.target

[Service]
Type=simple
WorkingDirectory=$DIR
ExecStart=/usr/bin/java -Xms1024m -Xmx4096m -Dfile.encoding=UTF-8 -jar $DIR/cornelius.jar --server
Restart=always
RestartSec=5
StandardOutput=append:$DIR/discord_bot.log
StandardError=append:$DIR/discord_bot.log
Environment=DISPLAY=:0
Environment=XAUTHORITY=%h/.Xauthority

[Install]
WantedBy=default.target
EOF

# 4. Criar entrada XDG Autostart para inicialização com a sessão gráfica (Pop!_OS / GNOME)
AUTOSTART_DESKTOP="$HOME/.config/autostart/cornelius.desktop"
cat <<EOF > "$AUTOSTART_DESKTOP"
[Desktop Entry]
Type=Application
Version=1.0
Name=Cornelius AI
Comment=Inicialização Automática do Cornelius AI no Boot
Exec=$DIR/start-discord-background.sh
Icon=$DIR/cornelius.png
Terminal=false
Categories=Utility;ArtificialIntelligence;
X-GNOME-Autostart-enabled=true
EOF

# 5. Ativar e iniciar o serviço via systemd
systemctl --user daemon-reload
systemctl --user enable cornelius.service
systemctl --user restart cornelius.service

# 6. Habilitar linger para que o serviço rode mesmo antes do login gráfico (opcional/recomendado)
if command -v loginctl >/dev/null 2>&1; then
    loginctl enable-linger "$USER" 2>/dev/null || true
fi

echo ""
echo "======================================================="
echo " [SUCESSO] Cornelius configurado para iniciar com o Linux!"
echo " -> Serviço Systemd: Ativo e Habilitado (cornelius.service)"
echo " -> XDG Autostart: Configurado em ~/.config/autostart/cornelius.desktop"
echo " -> Sempre que seu notebook ligar, o Cornelius iniciará automaticamente!"
echo " -> Status atual: $(systemctl --user is-active cornelius.service)"
echo "======================================================="
echo ""

