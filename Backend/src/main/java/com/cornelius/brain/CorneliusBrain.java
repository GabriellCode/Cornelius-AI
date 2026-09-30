package com.cornelius.brain;

import com.cornelius.brain.tools.*;
import com.cornelius.config.CorneliusConfig;
import com.cornelius.storage.CompressionEngine;
import com.cornelius.storage.DocumentIngester;
import com.cornelius.storage.ExternalDriveManager;
import com.cornelius.storage.KnowledgeBase;
import com.cornelius.storage.MemoryVault;
import com.cornelius.util.JsonParser;
import com.cornelius.util.Logger;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CorneliusBrain {
    private final CorneliusConfig config;
    private final ExternalDriveManager driveManager;
    private final KnowledgeBase knowledgeBase;
    private final MemoryVault memoryVault;
    private final DocumentIngester ingester;

    private final GeminiClient geminiClient;
    private final OllamaClient ollamaClient;

    private final WebSearchTool webSearchTool;
    private final SystemMetricsTool systemMetricsTool;
    private final StorageTool storageTool;
    private final TerminalTool terminalTool;
    private final FileSystemTool fileSystemTool;
    private final AppLauncherTool appLauncherTool;
    private final InstagramTool instagramTool;

    private final List<ChatMessage> chatHistory = new CopyOnWriteArrayList<>();

    private static final Pattern ACTION_PATTERN = Pattern.compile("\\[ACTION:\\s*([a-zA-Z0-9_]+)\\s*\\|\\s*([^\\]|]+)(?:\\|\\s*([^\\]]+))?\\]");

    public record BrainResponse(
            String text,
            String providerUsed,
            List<String> toolsExecuted,
            long processingTimeMs
    ) {}

    public CorneliusBrain(CorneliusConfig config) {
        this.config = config;
        this.driveManager = new ExternalDriveManager(config);
        this.knowledgeBase = new KnowledgeBase(driveManager, config);
        this.memoryVault = new MemoryVault(driveManager, config);
        this.ingester = new DocumentIngester(knowledgeBase, driveManager, config);

        this.geminiClient = new GeminiClient(config);
        this.ollamaClient = new OllamaClient(config);

        this.webSearchTool = new WebSearchTool();
        this.systemMetricsTool = new SystemMetricsTool();
        this.storageTool = new StorageTool(driveManager, knowledgeBase, memoryVault, ingester);
        this.terminalTool = new TerminalTool();
        this.fileSystemTool = new FileSystemTool();
        this.appLauncherTool = new AppLauncherTool();
        this.instagramTool = new InstagramTool(config);
    }

    public BrainResponse processUserMessage(String userMessage) {
        long startTime = System.currentTimeMillis();
        List<String> toolsExecuted = new ArrayList<>();
        StringBuilder auxiliaryContext = new StringBuilder();

        userMessage = userMessage.trim();
        String lower = userMessage.toLowerCase(Locale.ROOT);

        List<CompletableFuture<Void>> parallelTasks = new ArrayList<>();

        // 1. System telemetry trigger (Parallel)
        if (lower.contains("sistema") || lower.contains("bateria") || lower.contains("cpu") ||
                lower.contains("ram") || lower.contains("memória") || lower.contains("hardware") ||
                lower.contains("temperatura") || lower.contains("notebook") || lower.contains("computador")) {
            parallelTasks.add(CompletableFuture.runAsync(() -> {
                String sysInfo = systemMetricsTool.execute("");
                synchronized (auxiliaryContext) {
                    auxiliaryContext.append("\n=== TELEMETRIA DO SISTEMA ===\n").append(sysInfo).append("\n");
                    toolsExecuted.add("SystemMetrics");
                }
            }));
        }

        // 2. Web Search trigger (Parallel)
        if (lower.contains("pesquise") || lower.contains("pesquisar") || lower.contains("busca na web") ||
                lower.contains("procure na internet") || lower.contains("notícias") || lower.contains("preço") ||
                lower.startsWith("http://") || lower.startsWith("https://")) {
            final String finalUserMsg = userMessage;
            parallelTasks.add(CompletableFuture.runAsync(() -> {
                try {
                    String searchPrompt = extractSearchQuery(finalUserMsg);
                    String webResults = webSearchTool.execute(searchPrompt);
                    synchronized (auxiliaryContext) {
                        auxiliaryContext.append("\n").append(webResults).append("\n");
                        toolsExecuted.add("WebSearch (" + searchPrompt + ")");
                    }
                } catch (Exception e) {
                    Logger.warn("Brain", "Falha na busca web: " + e.getMessage());
                }
            }));
        }

        // 3. Direct Natural Action Triggers (Audio, App Launch, URLs)
        if (lower.startsWith("abra ") || lower.startsWith("abrir ") || lower.startsWith("inicie ") ||
                lower.contains("volume") || lower.contains("mutar") || lower.contains("desmutar") ||
                lower.contains("mudo") || lower.contains("notificação")) {
            try {
                String appOutput = appLauncherTool.execute(userMessage);
                auxiliaryContext.append("\n=== AÇÃO EXECUTADA NO APARELHO ===\n").append(appOutput).append("\n");
                toolsExecuted.add("AppLauncher (" + userMessage + ")");
            } catch (Exception e) {
                auxiliaryContext.append("\n=== ERRO NA AÇÃO ===\n").append(e.getMessage()).append("\n");
            }
        }

        // 4. Direct Terminal & Shell native trigger
        if (userMessage.startsWith("$") || lower.startsWith("exec:") || lower.startsWith("terminal:") ||
                lower.contains("execute o comando") || lower.contains("rode o comando") || lower.contains("no terminal") ||
                lower.startsWith("ping ") || lower.startsWith("curl ") || lower.startsWith("uname ") ||
                lower.startsWith("ip ") || lower.startsWith("free ") || lower.startsWith("uptime") || lower.startsWith("df ")) {
            try {
                String cmd = extractTerminalCommand(userMessage);
                String termOutput = terminalTool.execute(cmd);
                auxiliaryContext.append("\n=== EXECUÇÃO DO TERMINAL ===\nComando: ").append(cmd).append("\nResultado:\n").append(termOutput).append("\n");
                toolsExecuted.add("Terminal (" + cmd + ")");
            } catch (Exception e) {
                auxiliaryContext.append("\n=== ERRO NO TERMINAL ===\n").append(e.getMessage()).append("\n");
            }
        }

        // 5. FileSystem native trigger
        if (lower.contains("liste os arquivos") || lower.contains("conteúdo da pasta") || lower.contains("leia o arquivo") ||
                lower.contains("crie o arquivo") || lower.contains("procure o arquivo") || lower.contains("minha pasta") ||
                lower.contains("downloads") || lower.contains("documentos") || lower.contains("~/") || lower.startsWith("ls ") || lower.startsWith("cat ")) {
            try {
                String fsOutput = fileSystemTool.execute(userMessage);
                auxiliaryContext.append("\n=== SISTEMA DE ARQUIVOS LOCAL ===\n").append(fsOutput).append("\n");
                toolsExecuted.add("FileSystem");
            } catch (Exception e) {
                auxiliaryContext.append("\n=== ERRO NO SISTEMA DE ARQUIVOS ===\n").append(e.getMessage()).append("\n");
            }
        }

        // 6. HD storage status trigger
        if (lower.contains("hd") || lower.contains("espaço") || lower.contains("armazenamento") ||
                lower.contains("disco") || lower.contains("compressão") || lower.contains("cofre")) {
            try {
                String storageInfo = storageTool.execute("status");
                auxiliaryContext.append("\n").append(storageInfo).append("\n");
                toolsExecuted.add("StorageStatus");
            } catch (Exception ignored) {}
        }

        // 7. Ollama Local AI trigger
        if (lower.contains("ollama") || lower.contains("ia local") || lower.contains("offline") || lower.contains("motor local")) {
            boolean running = OllamaManager.isRunning(config.getOllamaUrl());
            boolean installed = OllamaManager.isInstalled();
            String ollamaStatus;
            if (running) {
                ollamaStatus = "[Ollama Status: ONLINE na porta 11434 com modelo '" + config.getOllamaModel() + "']";
            } else if (installed) {
                try {
                    OllamaManager.startDaemon(config.getOllamaUrl(), s -> {});
                    ollamaStatus = "[Ollama Status: INICIADO com sucesso pelo Cornelius na porta 11434!]";
                } catch (Exception ex) {
                    ollamaStatus = "[Ollama Status: INSTALADO, mas falhou ao iniciar: " + ex.getMessage() + "]";
                }
            } else {
                ollamaStatus = "[Ollama Status: NÃO INSTALADO no sistema. Instrua o usuário a rodar 'curl -fsSL https://ollama.com/install.sh | sh' no terminal]";
            }
            auxiliaryContext.append("\n").append(ollamaStatus).append("\n");
            toolsExecuted.add("OllamaManager");
        }

        // 8. Instagram trigger
        if (lower.contains("instagram") || lower.contains("insta ") || lower.contains("direct do insta") || lower.contains("post do insta") || lower.contains("feed")) {
            try {
                String instaOutput = instagramTool.execute(userMessage);
                auxiliaryContext.append("\n=== FERRAMENTA DO INSTAGRAM ===\n").append(instaOutput).append("\n");
                toolsExecuted.add("Instagram");
            } catch (Exception e) {
                auxiliaryContext.append("\n=== ERRO NO INSTAGRAM ===\n").append(e.getMessage()).append("\n");
            }
        }

        // 9. Knowledge Base RAG Retrieval from 1TB HD (Parallel)
        final String finalQuery = userMessage;
        parallelTasks.add(CompletableFuture.runAsync(() -> {
            String ragContext = knowledgeBase.buildContextForQuery(finalQuery, 2);
            if (!ragContext.isBlank()) {
                synchronized (auxiliaryContext) {
                    auxiliaryContext.append("\n").append(ragContext).append("\n");
                    toolsExecuted.add("KnowledgeBase RAG");
                }
            }
        }));

        // 9. Memory Vault (Parallel)
        final String[] memoryHolder = new String[]{""};
        parallelTasks.add(CompletableFuture.runAsync(() -> {
            memoryHolder[0] = memoryVault.getFormattedMemoriesForPrompt();
        }));

        // Await all parallel tasks concurrently
        if (!parallelTasks.isEmpty()) {
            CompletableFuture.allOf(parallelTasks.toArray(new CompletableFuture[0])).join();
        }
        String memoryContext = memoryHolder[0];

        // 10. System Prompt Assembly
        String systemPrompt = PersonaPrompt.getSystemPrompt(
                config,
                memoryContext,
                auxiliaryContext.toString(),
                ""
        );

        // 11. Select LLM and execute
        String responseText;
        String providerName;

        try {
            LLMClient client = resolveLLMClient();
            providerName = client.getProviderName();
            responseText = client.generate(systemPrompt, userMessage, new ArrayList<>(chatHistory));
        } catch (Exception e1) {
            Logger.warn("Brain", "Falha no provedor primário: " + e1.getMessage() + ". Acionando contingência...");
            LLMClient fallback = getFallbackClient();
            if (fallback != null && fallback.isAvailable()) {
                try {
                    providerName = fallback.getProviderName();
                    responseText = fallback.generate(systemPrompt, userMessage, new ArrayList<>(chatHistory));
                } catch (Exception e2) {
                    Logger.error("Brain", "Provedor de contingência também falhou: " + e2.getMessage());
                    providerName = "Cornelius Offline Assistant";
                    responseText = generateOfflineButlerResponse(userMessage, auxiliaryContext.toString(), e1.getMessage());
                }
            } else {
                providerName = "Cornelius Offline Assistant";
                responseText = generateOfflineButlerResponse(userMessage, auxiliaryContext.toString(), e1.getMessage());
            }
        }

        // 12. Autonomous Action Post-Processing (Execute any [ACTION: ...] tags produced by LLM)
        responseText = executeAutonomousActions(responseText, toolsExecuted);

        // Record history
        chatHistory.add(ChatMessage.user(userMessage));
        chatHistory.add(ChatMessage.assistant(responseText));

        // Auto archive to compressed vault
        if (config.isAutoArchiveChat()) {
            archiveConversationSession();
        }

        long elapsed = System.currentTimeMillis() - startTime;
        return new BrainResponse(responseText, providerName, toolsExecuted, elapsed);
    }

    private String executeAutonomousActions(String text, List<String> toolsExecuted) {
        if (text == null || !text.contains("[ACTION:")) {
            return text;
        }

        Matcher matcher = ACTION_PATTERN.matcher(text);
        StringBuilder sb = new StringBuilder();

        while (matcher.find()) {
            String type = matcher.group(1).trim().toLowerCase();
            String target = matcher.group(2).trim();
            String content = matcher.group(3) != null ? matcher.group(3).trim() : "";

            String executionResult;
            try {
                switch (type) {
                    case "app":
                    case "open_app":
                        executionResult = appLauncherTool.execute(target);
                        toolsExecuted.add("Action:App (" + target + ")");
                        break;
                    case "audio":
                    case "volume":
                        executionResult = appLauncherTool.execute(target);
                        toolsExecuted.add("Action:Audio (" + target + ")");
                        break;
                    case "terminal":
                    case "shell":
                    case "cmd":
                        executionResult = terminalTool.execute(target);
                        toolsExecuted.add("Action:Terminal (" + target + ")");
                        break;
                    case "file_create":
                    case "file_write":
                        executionResult = fileSystemTool.execute("write " + target + " ::: " + content);
                        toolsExecuted.add("Action:FileWrite (" + target + ")");
                        break;
                    case "file_read":
                        executionResult = fileSystemTool.execute("read " + target);
                        toolsExecuted.add("Action:FileRead (" + target + ")");
                        break;
                    case "file_list":
                        executionResult = fileSystemTool.execute("list " + target);
                        toolsExecuted.add("Action:FileList (" + target + ")");
                        break;
                    case "notify":
                        executionResult = appLauncherTool.execute("notify:" + target);
                        toolsExecuted.add("Action:Notify (" + target + ")");
                        break;
                    case "web_search":
                        executionResult = webSearchTool.execute(target);
                        toolsExecuted.add("Action:WebSearch (" + target + ")");
                        break;
                    case "instagram":
                    case "insta":
                        executionResult = instagramTool.execute(target + (content != null && !content.isBlank() ? " ::: " + content : ""));
                        toolsExecuted.add("Action:Instagram (" + target + ")");
                        break;
                    default:
                        executionResult = appLauncherTool.execute(target);
                        toolsExecuted.add("Action:" + type + " (" + target + ")");
                        break;
                }
            } catch (Exception e) {
                executionResult = "Erro ao executar ação [" + type + "]: " + e.getMessage();
                Logger.error("Brain", executionResult);
            }

            matcher.appendReplacement(sb, Matcher.quoteReplacement("*(⚡ Ação Executada no Aparelho: " + executionResult + ")*"));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private LLMClient resolveLLMClient() {
        // If the user explicitly wants to use the local model, prefer Ollama when available.
        if (config.isUseLocalModel()) {
            if (ollamaClient.isAvailable()) {
                return ollamaClient;
            }
            // Fallback to Gemini if Ollama not available.
            if (geminiClient.isAvailable()) {
                return geminiClient;
            }
            throw new IllegalStateException("useLocalModel is true but no local Ollama client is available.");
        }
        // Existing behavior for AUTO or explicit provider selection.
        String provider = config.getActiveProvider();
        if ("GEMINI".equalsIgnoreCase(provider)) {
            return geminiClient;
        } else if ("OLLAMA".equalsIgnoreCase(provider)) {
            return ollamaClient;
        }
        // AUTO Mode: Prefer Gemini if configured, else Ollama if available.
        if (geminiClient.isAvailable()) {
            return geminiClient;
        }
        if (ollamaClient.isAvailable()) {
            return ollamaClient;
        }
        throw new IllegalStateException("Nenhum provedor de IA ativo ou configurado (Adicione sua chave Gemini ou inicie o Ollama).");
    }

    private LLMClient getFallbackClient() {
        String provider = config.getActiveProvider();
        if ("OLLAMA".equalsIgnoreCase(provider)) {
            return geminiClient.isAvailable() ? geminiClient : null;
        } else if ("GEMINI".equalsIgnoreCase(provider)) {
            return ollamaClient.isAvailable() ? ollamaClient : null;
        } else {
            // AUTO Mode: if one fails, try the other
            if (geminiClient.isAvailable()) return geminiClient;
            if (ollamaClient.isAvailable()) return ollamaClient;
        }
        return geminiClient.isAvailable() ? geminiClient : null;
    }

    private String extractSearchQuery(String text) {
        return text.replaceAll("(?i)pesquise\\s+(sobre\\s+)?(na\\s+internet\\s+)?(na\\s+web\\s+)?", "")
                .replaceAll("(?i)procure\\s+(sobre\\s+)?(na\\s+internet\\s+)?", "")
                .replaceAll("(?i)busque\\s+(sobre\\s+)?", "")
                .trim();
    }

    private String extractTerminalCommand(String text) {
        if (text.startsWith("$")) {
            return text.substring(1).trim();
        }
        if (text.toLowerCase().startsWith("exec:")) {
            return text.substring(5).trim();
        }
        if (text.toLowerCase().startsWith("terminal:")) {
            return text.substring(9).trim();
        }
        return text.replaceAll("(?i)^execute\\s+o\\s+comando\\s+", "")
                .replaceAll("(?i)^rode\\s+o\\s+comando\\s+", "")
                .replaceAll("(?i)^no\\s+terminal\\s+", "")
                .trim();
    }

    private String generateOfflineButlerResponse(String userMsg, String toolOutput, String errorDetail) {
        String userName = config.getUserName();
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Saudações, %s! Sou Cornelius, seu assistente pessoal.\n\n", userName));

        if (!toolOutput.isBlank()) {
            sb.append("Analisei os dados locais e ferramentas disponíveis para a sua solicitação:\n\n");
            sb.append(toolOutput).append("\n\n");
        }

        sb.append("*(Nota do Mordomo: O motor de raciocínio generativo avançado não pôde ser contatado no momento [")
                .append(errorDetail).append("]. Para ativar a inteligência completa, por favor insira sua chave da API do Gemini ou inicialize o Ollama local nas Configurações).*");

        return sb.toString();
    }

    public synchronized void archiveConversationSession() {
        try {
            if (chatHistory.isEmpty()) return;
            List<Map<String, Object>> list = new ArrayList<>();
            for (ChatMessage m : chatHistory) {
                list.add(Map.of("role", m.role(), "content", m.content(), "timestamp", m.timestamp()));
            }
            String json = JsonParser.toJson(list);
            Path archiveFile = driveManager.getChatHistoryDir().resolve("chat_session.czip");
            Path temp = Files.createTempFile("chat_hist_", ".json");
            Files.writeString(temp, json);
            CompressionEngine.compressFileToArchive(temp, archiveFile, config.getCompressionLevel());
            temp.toFile().deleteOnExit();
        } catch (Exception e) {
            Logger.warn("Brain", "Erro ao arquivar histórico de conversas: " + e.getMessage());
        }
    }

    public void clearHistory() {
        chatHistory.clear();
    }

    // Getters for subsystems
    public CorneliusConfig getConfig() { return config; }
    public ExternalDriveManager getDriveManager() { return driveManager; }
    public KnowledgeBase getKnowledgeBase() { return knowledgeBase; }
    public MemoryVault getMemoryVault() { return memoryVault; }
    public DocumentIngester getIngester() { return ingester; }
    public WebSearchTool getWebSearchTool() { return webSearchTool; }
    public SystemMetricsTool getSystemMetricsTool() { return systemMetricsTool; }
    public StorageTool getStorageTool() { return storageTool; }
    public TerminalTool getTerminalTool() { return terminalTool; }
    public InstagramTool getInstagramTool() { return instagramTool; }
    public List<ChatMessage> getChatHistory() { return Collections.unmodifiableList(chatHistory); }
}
