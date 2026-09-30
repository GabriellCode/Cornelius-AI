# 🎩 Cornelius - Mordomo Pessoal de Inteligência Artificial

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-21%2B-orange.svg)](https://openjdk.org/)
[![Android](https://img.shields.io/badge/Android-Client-green.svg)](Android/)

**Cornelius** é o seu mordomo pessoal de inteligência artificial de alto desempenho, desenvolvido com backend **100% Java 21 nativo** para o sistema **Linux Pop!_OS**.

O aplicativo roda localmente no seu notebook como um aplicativo executável desktop, gerencia e comprime dados em um **HD externo de 1TB**, acessa a **internet em tempo real**, monitora a telemetria da sua máquina e alimenta continuamente sua base de conhecimento.

---

## 🌟 Principais Recursos

1. **Backend 100% Java 21**:
   - Zero dependências externas pesadas — tudo funciona nativamente com o JDK 21.
   - Alta concorrência e responsividade com **Virtual Threads**.
   - Servidor HTTP e API REST embutida (`http://localhost:8080`).

2. **Cofre de Compressão & HD Externo (1TB)**:
   - **Algoritmo DEFLATE Nível 9** com verificação de integridade **SHA-256**.
   - Ingestão e compactação contínua de documentos, código-fonte, notas e memórias.
   - Monitoramento em tempo real do espaço ocupado, espaço livre e taxa de compressão acumulada no HD.

3. **Base de Conhecimento RAG Local**:
   - Cornelius se alimenta dos arquivos guardados e comprimidos no seu HD externo.
   - Motor de busca semântica e por palavras-chave com indexação **BM25 / TF-IDF**.

4. **Acesso à Internet & Ferramentas**:
   - Pesquisa na web em tempo real (DuckDuckGo).
   - Extração e leitura de páginas/URLs.
   - Monitor de telemetria do notebook (CPU, RAM, Disco, Bateria do notebook).

5. **Inteligência Artificial Híbrida**:
   - **Google Gemini API** (`gemini-2.5-flash`, `gemini-2.5-pro`).
   - **Ollama Local** (para operar 100% offline com `llama3.2`, `deepseek-r1`, `qwen2.5`, etc.).
   - Modo Automático com fallback inteligente caso esteja sem conexão.

6. **Interface Desktop Executável**:
   - Janela moderna em tema escuro com abas de Chat, Cofre do HD e Memórias.
   - Atalho `.desktop` para o lançador de aplicativos do **Pop!_OS**.

---

## 🚀 Como Executar

### 1. Compilar o Projeto
Para compilar e gerar o arquivo `cornelius.jar`:
```bash
./build.sh
```

### 2. Iniciar o Aplicativo Desktop (Recomendado)
Para abrir a interface gráfica desktop no seu Pop!_OS:
```bash
./run.sh
```

### 3. Modo Terminal / CLI
Caso deseje conversar diretamente pelo terminal:
```bash
./run.sh --cli
```

### 4. Modo Servidor Headless
Para rodar apenas a API local em segundo plano:
```bash
./run.sh --server
```

---

## ⚙️ Configurações

Ao abrir a interface gráfica, clique em **⚙️ Configurações** no topo direito ou edite o arquivo `~/.cornelius/config.json`:

- **Chave Google Gemini**: Insira sua chave da API do Gemini (ou defina a variável de ambiente `GEMINI_API_KEY`).
- **Ollama Local**: Caso utilize Ollama, informe a URL (`http://localhost:11434`) e o modelo desejado.
- **Caminho do HD Externo**: Selecione o ponto de montagem do seu HD de 1TB (ex: `/media/gabriell/MEU_HD/Cornelius_Vault`).
- **Nível de Compressão**: Ajuste de 1 a 9 (padrão: 9 máxima economia de espaço).
- **Nome de Tratamento**: Como Cornelius deve chamá-lo ("Senhor", "Mestre", etc.).

---

## 📦 Atalho no Menu de Aplicativos do Pop!_OS

Para instalar o Cornelius no menu de programas do seu sistema com o ícone personalizado:
```bash
./install-desktop.sh
```
Ou copie o arquivo `cornelius.desktop` para `~/.local/share/applications/`.

---

## 🧪 Bateria de Testes

Para executar a validação de todos os módulos (JSON, Compressão, HD, RAG e Telemetria):
```bash
java -cp Backend/bin com.cornelius.test.CorneliusTestSuite
```

---

## 📄 Licença

Este projeto está licenciado sob os termos da licença [MIT](LICENSE) © 2026 GabriellCode. Consulte o arquivo [LICENSE](LICENSE) para obter todos os detalhes.


