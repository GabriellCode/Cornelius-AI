#!/usr/bin/env bash
set -e

echo "=== [CORNELIUS] Instalador Automatizado do Ollama Local ==="
echo "Instalando Ollama no Pop!_OS Linux..."

curl -fsSL https://ollama.com/install.sh | sh

echo "Iniciando serviço e baixando o modelo leve recomendado (Llama 3.2 3B)..."
ollama serve &
sleep 3
ollama pull llama3.2

echo "=== [CORNELIUS] Ollama instalado e modelo llama3.2 pronto para uso offline! ==="

