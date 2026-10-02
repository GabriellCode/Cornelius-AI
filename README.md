# 🎩 Cornelius - Mordomo Pessoal de Inteligência Artificial

[![License: Non-Commercial](https://img.shields.io/badge/License-Non--Commercial%20(Hb%20Head%20Black)-red.svg)](LICENSE)
[![Instagram](https://img.shields.io/badge/Instagram-%40corneliusai.1-E4405F?logo=instagram&logoColor=white)](https://www.instagram.com/corneliusai.1/)
[![Discord Bot](https://img.shields.io/badge/Discord-Convidar%20Bot-5865F2?logo=discord&logoColor=white)](https://discord.com/oauth2/authorize?client_id=1542459980010102784)
[![Java](https://img.shields.io/badge/Java-21%2B-orange.svg)](https://openjdk.org/)
[![Platform](https://img.shields.io/badge/Platform-Windows%20%7C%20Linux-blue.svg)](#)
[![Android](https://img.shields.io/badge/Android-Client-green.svg)](Android/)

**Cornelius** é o seu mordomo pessoal de inteligência artificial de alto desempenho, desenvolvido com backend **100% Java 21 nativo**. O sistema é multiplataforma, com suporte completo e otimizado para **Windows** (sistema onde roda atualmente) e **Linux (Pop!_OS / Ubuntu)**.

O aplicativo opera localmente no seu computador como um assistente desktop completo, gerencia e comprime dados em um **HD externo de 1TB**, acessa a **internet em tempo real**, monitora a telemetria da máquina, controla o PC e alimenta continuamente sua base de conhecimento, além de oferecer controle remoto via **aplicativo Android e navegador**.

---

## 🌐 Conexões Oficiais

* 📸 **Instagram Oficial:** Acompanhe o desenvolvimento e postagens do Cornelius no perfil:  
  👉 **[@corneliusai.1](https://www.instagram.com/corneliusai.1/)**

* 🤖 **Adicionar Cornelius ao Discord:** Convide o bot do Cornelius diretamente para o seu servidor Discord:  
  👉 **[Clique aqui para autorizar e adicionar ao Discord](https://discord.com/oauth2/authorize?client_id=1542459980010102784)**

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
   - 4 abas completas: **Conversa**, **Controle do Computador** (suspender, desligar, reiniciar, travar, bloquear), **Status do PC** em tempo real e **Terminal Remoto** (PowerShell/Bash).
   - Acesso rápido como **PWA** no navegador mobile ou como app nativo compilado pelo **Android Studio** na pasta `Android/`.

7. **Sincronização com GitHub 100% Java 21 Nativo**:
   - Motor próprio em Java puro utilizando a API REST Git Database (Blobs, Trees, Commits, Refs).
   - Zero dependências externas ou scripts de shell adicionais.
   - Atualização do repositório com 1 clique na aba Controle do aplicativo mobile, por comando no chat com o Cornelius, ou via linha de comando Java.

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

## 📄 Licença & Direitos Autorais (Copyright)

Este projeto é protegido por direitos autorais e distribuído sob a **Licença Source-Available Não Comercial**:
- 👁️ **Permitido:** Visualizar o código-fonte, estudar a arquitetura, compilar e executar localmente para testes, aprendizado, avaliação e uso pessoal não comercial.
- 🚫 **Proibido:** Qualquer uso para fins comerciais, revenda, distribuição comercial, monetização direta ou indireta, ou oferta do software como serviço pago/comercial sem autorização expressa prévia por escrito.
- ⚖️ **Propriedade Intelectual & Copyright:** © 2026 **Hb Head Black**. Todos os direitos reservados.

Para consultar todos os termos jurídicos e condições na íntegra, veja o arquivo [LICENSE](LICENSE).
