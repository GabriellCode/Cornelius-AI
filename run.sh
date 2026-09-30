#!/bin/bash
set -e

APP_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JAR_FILE="$APP_DIR/cornelius.jar"

if [ ! -f "$JAR_FILE" ]; then
    echo -e "\e[33m[CORNELIUS]\e[0m Executável não encontrado. Compilando projeto primeiro..."
    bash "$APP_DIR/build.sh"
fi

echo -e "\e[32m[CORNELIUS]\e[0m Lançando Mordomo Pessoal..."
exec java -Xms1024m -Xmx4096m -Dfile.encoding=UTF-8 -jar "$JAR_FILE" "$@"

