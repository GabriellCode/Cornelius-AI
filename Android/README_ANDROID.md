# 📱 Cornelius.AI para Android

O Cornelius agora conta com uma experiência móvel completa para controle total do computador a partir de qualquer smartphone Android, com **4 abas dedicadas**:

1. 💬 **Conversa**: Chat com IA, badges de ferramentas, microfone com reconhecimento de voz e markdown formatado.
2. 🎛️ **Controle**: Ligar/reiniciar Bot do Discord, salvar cofre de 1TB no HD, alternar autostart do Windows, bloquear tela do PC e abrir links/redes sociais.
3. 📊 **Status do Computador**: Telemetria em tempo real com mostradores visuais de CPU, RAM total de 32GB, memória JVM (4GB), espaço nos discos (C: e D:) e console de logs do sistema.
4. 💻 **Terminal Interativo**: Console PowerShell/CMD interativo para digitar comandos remotamente com retorno de texto em tempo real e atalhos rápidos (`ipconfig`, `dir`, `Get-Process`, etc.).

---

## 🚀 Método 1: Instalação Instantânea no Celular (Recomendado - 100% PWA)

Você **NÃO precisa compilar nada** para ter o app no seu celular agora mesmo:

1. Certifique-se de que o Cornelius está rodando no computador (execute `Cornelius.bat` ou `run.bat`).
2. Conecte seu celular Android na **mesma rede Wi-Fi** do computador.
3. No navegador do seu celular (Google Chrome, Edge ou Samsung Internet), acesse:
   ```
   http://192.168.1.5:8080
   ```
4. O navegador exibirá um aviso ou você pode tocar no menu de 3 pontinhos (**⋮**) e selecionar:
   * **"Instalar aplicativo"** ou **"Adicionar à tela inicial"**.
5. Um ícone do Cornelius será criado na sua gaveta de aplicativos do Android. Ao abrir, ele roda em **tela cheia nativa** (sem barra de URL do navegador), com suporte a toques, gestos e vibração!

---

## 🛠️ Método 2: Abrir no Android Studio & Gerar .APK Nativo

Este repositório inclui um projeto nativo Android Studio pronto em Kotlin:

1. Abra o **Android Studio** instalado no seu computador (`C:\Program Files\Android\Android Studio`).
2. Clique em **File > Open** (ou "Open Project" na tela inicial).
3. Selecione a pasta:
   ```
   E:\APP\Android
   ```
4. O Android Studio sincronizará o Gradle automaticamente.
5. Para gerar o instalador do aplicativo no seu celular:
   * Vá em **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
   * O Android Studio gerará o arquivo `app-debug.apk`.
   * Envie o `.apk` para o seu celular Android e instale!

