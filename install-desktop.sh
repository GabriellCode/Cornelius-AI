#!/bin/bash
set -e

APP_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JAR_FILE="$APP_DIR/cornelius.jar"
ICON_FILE="$APP_DIR/cornelius.png"
DESKTOP_DIR="$HOME/.local/share/applications"
DESKTOP_FILE="$DESKTOP_DIR/cornelius.desktop"

mkdir -p "$DESKTOP_DIR"

if [ ! -f "$JAR_FILE" ]; then
    echo -e "\e[34m[CORNELIUS]\e[0m Compilando aplicativo..."
    bash "$APP_DIR/build.sh"
fi

cat <<EOF > "$DESKTOP_FILE"
[Desktop Entry]
Version=1.0
Type=Application
Name=Cornelius.AI
GenericName=Mordomo Pessoal de Inteligência Artificial
Comment=Assistente Pessoal Autônomo com compressão em HD externo de 1TB e busca web
Exec=java -Xms1024m -Xmx4096m -Dfile.encoding=UTF-8 -jar "$JAR_FILE"
Icon=$ICON_FILE
Terminal=false
Categories=Utility;Office;ArtificialIntelligence;
StartupNotify=true
StartupWMClass=com-cornelius-Main
EOF

chmod +x "$DESKTOP_FILE"

echo -e "\e[32m[CORNELIUS]\e[0m Atalho de desktop instalado com sucesso em:"
echo " -> $DESKTOP_FILE"
echo -e "\e[32m[CORNELIUS]\e[0m O Cornelius agora aparece no menu de aplicativos do Pop!_OS!"

