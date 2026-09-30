#!/bin/bash
set -e

APP_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SRC_DIR="$APP_DIR/Backend/src/main/java"
BIN_DIR="$APP_DIR/Backend/bin"
JAR_FILE="$APP_DIR/cornelius.jar"

echo -e "\e[34m[CORNELIUS]\e[0m Compilando fontes em Java 21..."

mkdir -p "$BIN_DIR"
rm -rf "$BIN_DIR"/*

# Find all java files
JAVA_FILES=$(find "$SRC_DIR" -name "*.java")

# Compile
javac -encoding UTF-8 -d "$BIN_DIR" $JAVA_FILES

echo -e "\e[32m[CORNELIUS]\e[0m Compilação concluída com sucesso!"

# Create manifest and Jar
echo -e "\e[34m[CORNELIUS]\e[0m Gerando arquivo executável: $JAR_FILE"
echo "Main-Class: com.cornelius.Main" > "$BIN_DIR/MANIFEST.MF"

cd "$BIN_DIR"
jar cfm "$JAR_FILE" MANIFEST.MF com/

chmod +x "$JAR_FILE"
echo -e "\e[32m[CORNELIUS]\e[0m Pacote executável gerado: $JAR_FILE"

