# 🎩 Cornelius - Mordomo Pessoal de Inteligência Artificial

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-21%2B-orange.svg)](https://openjdk.org/)
[![Platform](https://img.shields.io/badge/Platform-Windows%20%7C%20Linux%20(Pop!_OS)-blue.svg)](#)
[![Android](https://img.shields.io/badge/Android-Client-green.svg)](Android/)

**Cornelius** é o seu mordomo pessoal de inteligência artificial de alto desempenho, desenvolvido com backend **100% Java 21 nativo**. O sistema é multiplataforma, com suporte completo e otimizado para **Windows** (sistema onde roda atualmente) e **Linux (Pop!_OS / Ubuntu)**.

O aplicativo opera localmente no seu computador como um assistente desktop completo, gerencia e comprime dados em um **HD externo de 1TB**, acessa a **internet em tempo real**, monitora a telemetria da máquina, controla o PC e alimenta continuamente sua base de conhecimento, além de oferecer controle remoto via **aplicativo Android e navegador**.

---

## 🌟 Principais Recursos

1. **Backend 100% Java 21 Nativo & Multiplataforma**:
   - Zero dependências externas pesadas — tudo funciona nativamente com o JDK 21.
   - Compatibilidade total com **Windows** (PowerShell, VBS silencioso, `.bat`) e **Linux** (Bash, `.desktop`).
   - Alta concorrência e responsividade utilizando **Virtual Threads** (Project Loom).
   - Servidor HTTP e API REST embutida (`http://localhost:8080`).

2. **Cofre de Compressão & HD Externo (1TB)**:
   - **Algoritmo DEFLATE Nível 9** com verificação de integridade **SHA-256**.
   - Ingestão e compactação contínua de documentos, código-fonte, notas e memórias.
   - Monitoramento em tempo real do espaço ocupado, espaço livre e taxa de compressão acumulada no HD.

3. **Base de Conhecimento RAG Local**:
   - Cornelius se alimenta dos arquivos guardados e comprimidos no seu HD externo.
   - Motor de busca semântica e por palavras-chave com indexação **BM25 / TF-IDF**.

4. **Acesso à Internet & Ferramentas Autônomas**:
   - Pesquisa na web em tempo real (DuckDuckGo).
   - Extração e leitura de páginas/URLs.
   - Monitor de telemetria da máquina (CPU, RAM, Disco, Bateria).
   - Bot oficial integrado para o **Discord**.

5. **Inteligência Artificial Híbrida**:
   - **Google Gemini API** (`gemini-2.5-flash`, `gemini-3.5-flash`).
   - **Ollama Local** (para operar 100% offline com `llama3.2`, `deepseek-r1`, etc.).
   - Modo Automático com fallback inteligente caso esteja sem conexão de rede.

6. **Aplicativo Mobile & Cliente Android**:
   - 4 abas completas: **Conversa**, **Controle do Computador** (suspender, desligar, reiniciar, travar), **Status do PC** em tempo real e **Terminal Remoto** (PowerShell/Bash).
   - Acesso rápido como **PWA** no navegador mobile ou como app nativo compilado pelo **Android Studio** na pasta `Android/`.

---

## 🚀 Como Executar

### 1. Compilar o Projeto
Para compilar o código Java e gerar o arquivo `cornelius.jar`:

- **Windows:**
  ```cmd
  build.bat
  ```
- **Linux (Pop!_OS / Ubuntu):**
  ```bash
  ./build.sh
  ```

### 2. Iniciar o Aplicativo Desktop (Recomendado)
Para abrir a interface gráfica do Cornelius:

- **Windows:**
  - Dê dois cliques em `Cornelius.bat` para iniciar diretamente.
  - Ou use `Cornelius.vbs` para inicialização silenciosa em segundo plano (sem janela preta de console).
  - Ou via prompt de comando:
    ```cmd
    run.bat
    ```
- **Linux:**
  ```bash
  ./run.sh
  ```

### 3. Modo Terminal / CLI
Caso deseje conversar diretamente pelo terminal:
- **Windows (PowerShell ou Prompt de Comando):**
  ```cmd
  run.bat --cli
  ```
- **Linux:**
  ```bash
  ./run.sh --cli
  ```

### 4. Modo Servidor Headless & Mobile
Para iniciar somente a API local e o servidor web para o aplicativo Android:
- **Windows:**
  ```cmd
  run.bat --server
  ```
- **Linux:**
  ```bash
  ./run.sh --server
  ```

---

## 📱 Aplicativo Mobile & Android

Cornelius disponibiliza controle remoto completo diretamente do seu celular:
- **Versão Web / PWA:** Acesse no celular pela mesma rede Wi-Fi pelo endereço: `http://<IP-DO-COMPUTADOR>:8080/mobile`
- **Projeto Nativo Android Studio:** Localizado na pasta [`Android/`](Android/), basta abrir no Android Studio, conectar seu aparelho Android e clicar em **Run** para gerar e instalar o aplicativo.

---

## ⚙️ Configurações

Ao abrir a interface gráfica, clique em **⚙️ Configurações** no topo direito ou edite o arquivo de configuração gerado automaticamente:
- No **Windows:** `C:\Users\<SeuUsuario>\.cornelius\config.json`
- No **Linux:** `~/.cornelius/config.json`

Campos configuráveis:
- **Chave Google Gemini**: Sua chave da API do Gemini (ou variável de ambiente `GEMINI_API_KEY`).
- **Ollama Local**: URL do serviço (`http://localhost:11434`) e modelo desejado.
- **Caminho do HD Externo**:
  - No Windows: `D:\Cornelius_Vault` (ou letra correspondente ao seu drive).
  - No Linux: `/media/<usuario>/MEU_HD/Cornelius_Vault`.
- **Nível de Compressão**: Ajuste de 1 a 9 (padrão: 9 máxima economia de espaço).
- **Nome de Tratamento**: Como Cornelius deve chamá-lo ("Senhor", "Mestre", etc.).

---

## 🧪 Bateria de Testes

Para executar a validação de todos os módulos (JSON, Compressão, HD, RAG e Telemetria):
- **Windows:**
  ```cmd
  java -cp Backend/bin com.cornelius.test.CorneliusTestSuite
  ```
- **Linux:**
  ```bash
  java -cp Backend/bin com.cornelius.test.CorneliusTestSuite
  ```

---

## 📄 Licença

Este projeto está licenciado sob os termos da licença [MIT](LICENSE) © 2026 GabriellCode. Consulte o arquivo [LICENSE](LICENSE) para obter todos os detalhes.
