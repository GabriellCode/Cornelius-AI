package com.cornelius.server;

import com.cornelius.brain.CorneliusBrain;
import com.cornelius.config.CorneliusConfig;
import com.cornelius.storage.DocumentIngester;
import com.cornelius.storage.ExternalDriveManager;
import com.cornelius.system.AutostartManager;
import com.cornelius.system.SystemProfile;
import com.cornelius.util.JsonParser;
import com.cornelius.util.Logger;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Executors;

public class LocalApiServer {
    private final CorneliusBrain brain;
    private final CorneliusConfig config;
    private HttpServer server;

    public LocalApiServer(CorneliusBrain brain, CorneliusConfig config) {
        this.brain = brain;
        this.config = config;
    }

    public void start() {
        try {
            int port = config.getServerPort();
            boolean bound = false;
            for (int p = port; p < port + 10; p++) {
                try {
                    server = HttpServer.create(new InetSocketAddress(p), 0);
                    bound = true;
                    port = p;
                    break;
                } catch (IOException ignored) {}
            }
            if (!bound) {
                Logger.warn("Server", "Portas 8080-8089 ocupadas. Servidor HTTP local pausado para esta instância.");
                return;
            }
            server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());

            // Core API Endpoints
            server.createContext("/api/chat", new ChatHandler());
            server.createContext("/api/storage", new StorageHandler());
            server.createContext("/api/system", new SystemHandler());
            server.createContext("/api/status", new StatusHandler());
            server.createContext("/api/terminal", new TerminalHandler());
            server.createContext("/api/control", new ControlHandler());
            server.createContext("/api/logs", new LogsHandler());
            server.createContext("/api/memories", new MemoriesHandler());
            server.createContext("/api/health", new HealthHandler());

            // PWA & Web App static assets
            server.createContext("/manifest.json", new ManifestHandler());
            server.createContext("/sw.js", new ServiceWorkerHandler());
            server.createContext("/cornelius.png", new IconHandler());
            server.createContext("/mobile", new WebUiHandler());
            server.createContext("/", new WebUiHandler());

            server.start();
            Logger.success("Server", "Servidor HTTP local do Cornelius ativo em: http://localhost:" + port);
            Logger.info("Server", "Acesse no celular Android pela mesma rede Wi-Fi via: http://192.168.1.5:" + port);
        } catch (Exception e) {
            Logger.warn("Server", "Aviso no servidor HTTP: " + e.getMessage());
        }
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            Logger.info("Server", "Servidor HTTP encerrado.");
        }
    }

    // --- HANDLERS ---

    private class ChatHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, Map.of("error", "Método não permitido"));
                return;
            }

            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            Map<String, Object> req = JsonParser.parseObject(body);
            String message = (String) req.get("message");

            if (message == null || message.isBlank()) {
                sendJsonResponse(exchange, 400, Map.of("error", "Campo 'message' é obrigatório"));
                return;
            }

            CorneliusBrain.BrainResponse res = brain.processUserMessage(message);
            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("response", res.text());
            resp.put("provider", res.providerUsed());
            resp.put("tools", res.toolsExecuted());
            resp.put("timeMs", res.processingTimeMs());

            sendJsonResponse(exchange, 200, resp);
        }
    }

    private class StatusHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            Map<String, Object> resp = new LinkedHashMap<>();

            // OS & Telemetry
            resp.put("osName", System.getProperty("os.name"));
            resp.put("osVersion", System.getProperty("os.version"));
            resp.put("osArch", System.getProperty("os.arch"));
            resp.put("isWindows", SystemProfile.isWindows());
            resp.put("cpuCores", SystemProfile.getCpuCores());
            resp.put("powerTier", SystemProfile.getPowerTier().name());
            resp.put("powerTierDesc", SystemProfile.getPowerTierDescription());

            // RAM & JVM Heap
            long freeJvmBytes = Runtime.getRuntime().freeMemory();
            long totalJvmBytes = Runtime.getRuntime().totalMemory();
            long maxJvmBytes = Runtime.getRuntime().maxMemory();
            long usedJvmBytes = totalJvmBytes - freeJvmBytes;
            double totalRamGb = SystemProfile.getTotalRamGb();
            long divisor = maxJvmBytes > 0 ? maxJvmBytes : totalJvmBytes;

            Map<String, Object> ram = new LinkedHashMap<>();
            ram.put("totalRamGb", totalRamGb);
            ram.put("jvmUsedMb", usedJvmBytes / (1024 * 1024));
            ram.put("jvmTotalMb", totalJvmBytes / (1024 * 1024));
            ram.put("jvmMaxMb", maxJvmBytes / (1024 * 1024));
            ram.put("jvmPercent", (int) (usedJvmBytes * 100 / divisor));
            resp.put("ram", ram);

            // Disks
            List<Map<String, Object>> disks = new ArrayList<>();
            File[] roots = File.listRoots();
            if (roots != null) {
                for (File root : roots) {
                    long total = root.getTotalSpace();
                    long free = root.getUsableSpace();
                    if (total > 0) {
                        long used = total - free;
                        double percent = ((double) used / total) * 100.0;
                        disks.add(Map.of(
                                "path", root.getAbsolutePath(),
                                "totalGb", String.format(Locale.US, "%.1f", total / 1e9),
                                "freeGb", String.format(Locale.US, "%.1f", free / 1e9),
                                "usedGb", String.format(Locale.US, "%.1f", used / 1e9),
                                "percent", (int) percent
                        ));
                    }
                }
            }
            resp.put("disks", disks);

            // Autostart
            resp.put("autostartEnabled", AutostartManager.isAutostartEnabled());

            // Discord Status
            boolean discordOnline = config.getDiscordToken() != null && !config.getDiscordToken().isBlank();
            resp.put("discordOnline", discordOnline);
            resp.put("discordBotUser", "@Cornelius");

            // Vault 1TB
            ExternalDriveManager.DriveMetrics m = brain.getDriveManager().getMetrics();
            Map<String, Object> vault = new LinkedHashMap<>();
            vault.put("path", m.path());
            vault.put("isExternal", m.isExternal());
            vault.put("usedPercent", m.usedPercent());
            vault.put("freeGb", String.format(Locale.US, "%.1f", m.freeBytes() / 1e9));
            vault.put("totalGb", String.format(Locale.US, "%.1f", m.totalBytes() / 1e9));
            vault.put("savingsRatio", m.overallSavingsRatio());
            vault.put("filesCount", m.totalArchivedFiles());
            resp.put("vault", vault);

            resp.put("serverTime", java.time.LocalTime.now().toString().split("\\.")[0]);
            sendJsonResponse(exchange, 200, resp);
        }
    }

    private class TerminalHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, Map.of("error", "Método não permitido"));
                return;
            }

            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            Map<String, Object> req = JsonParser.parseObject(body);
            String cmd = (String) req.get("command");
            if (cmd == null || cmd.isBlank()) {
                sendJsonResponse(exchange, 400, Map.of("error", "Comando vazio"));
                return;
            }

            long start = System.currentTimeMillis();
            try {
                String output = brain.getTerminalTool().execute(cmd);
                long elapsed = System.currentTimeMillis() - start;
                Map<String, Object> resp = new LinkedHashMap<>();
                resp.put("output", output);
                resp.put("timeMs", elapsed);
                resp.put("exitCode", 0);
                sendJsonResponse(exchange, 200, resp);
            } catch (Exception e) {
                long elapsed = System.currentTimeMillis() - start;
                Map<String, Object> resp = new LinkedHashMap<>();
                resp.put("output", "Erro ao executar comando: " + e.getMessage());
                resp.put("timeMs", elapsed);
                resp.put("exitCode", 1);
                sendJsonResponse(exchange, 200, resp);
            }
        }
    }

    private class ControlHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, Map.of("error", "Método não permitido"));
                return;
            }

            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            Map<String, Object> req = JsonParser.parseObject(body);
            String action = (String) req.get("action");
            String param = (String) req.getOrDefault("param", "");

            Map<String, Object> resp = new LinkedHashMap<>();
            try {
                switch (action != null ? action.toLowerCase() : "") {
                    case "toggle_autostart" -> {
                        boolean cur = AutostartManager.isAutostartEnabled();
                        if (cur) AutostartManager.disableAutostart();
                        else AutostartManager.enableAutostart();
                        resp.put("success", true);
                        resp.put("autostartEnabled", AutostartManager.isAutostartEnabled());
                        resp.put("message", "Inicialização no Boot " + (AutostartManager.isAutostartEnabled() ? "HABILITADA" : "DESABILITADA"));
                    }
                    case "clear_chat" -> {
                        brain.clearHistory();
                        resp.put("success", true);
                        resp.put("message", "Histórico de conversa esvaziado.");
                    }
                    case "save_vault" -> {
                        brain.archiveConversationSession();
                        brain.getKnowledgeBase().saveIndex();
                        brain.getMemoryVault().saveMemories();
                        resp.put("success", true);
                        resp.put("message", "Cofre e memórias sincronizados no HD.");
                    }
                    case "lock_pc" -> {
                        if (SystemProfile.isWindows()) {
                            new ProcessBuilder("rundll32.exe", "user32.dll,LockWorkStation").start();
                        }
                        resp.put("success", true);
                        resp.put("message", "Computador bloqueado com sucesso.");
                    }
                    case "open_url" -> {
                        if (param != null && !param.isBlank()) {
                            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                                Desktop.getDesktop().browse(new URI(param));
                            }
                            resp.put("success", true);
                            resp.put("message", "URL aberta no PC: " + param);
                        } else {
                            resp.put("error", "URL inválida");
                        }
                    }
                    case "open_instagram" -> {
                        brain.getInstagramTool().openInstagramWeb();
                        resp.put("success", true);
                        resp.put("message", "Instagram aberto no computador.");
                    }
                    case "open_directs" -> {
                        brain.getInstagramTool().openDirects();
                        resp.put("success", true);
                        resp.put("message", "Directs do Instagram abertos no computador.");
                    }
                    case "restart_bot" -> {
                        Thread.ofVirtual().start(() -> {
                            try {
                                if (SystemProfile.isWindows()) {
                                    new ProcessBuilder("cmd.exe", "/c", "run-discord-bot.bat").start();
                                } else {
                                    new ProcessBuilder("systemctl", "--user", "restart", "cornelius.service").start();
                                }
                            } catch (Exception ignored) {}
                        });
                        resp.put("success", true);
                        resp.put("message", "Reinício do Bot Discord disparado.");
                    }
                    default -> resp.put("error", "Ação desconhecida: " + action);
                }
                sendJsonResponse(exchange, 200, resp);
            } catch (Exception e) {
                sendJsonResponse(exchange, 500, Map.of("error", e.getMessage()));
            }
        }
    }

    private class LogsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            String logContent = "Nenhum log registrado ainda.";
            try {
                Path logPath = Paths.get(System.getProperty("user.dir"), "discord_bot.log");
                if (Files.exists(logPath)) {
                    List<String> lines = Files.readAllLines(logPath);
                    int start = Math.max(0, lines.size() - 50);
                    StringBuilder sb = new StringBuilder();
                    for (int i = start; i < lines.size(); i++) {
                        sb.append(lines.get(i)).append("\n");
                    }
                    logContent = sb.toString();
                }
            } catch (Exception ignored) {}
            sendJsonResponse(exchange, 200, Map.of("logs", logContent));
        }
    }

    private class StorageHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                ExternalDriveManager.DriveMetrics m = brain.getDriveManager().getMetrics();
                Map<String, Object> resp = new LinkedHashMap<>();
                resp.put("vaultPath", m.path());
                resp.put("totalBytes", m.totalBytes());
                resp.put("freeBytes", m.freeBytes());
                resp.put("usedBytes", m.usedBytes());
                resp.put("usedPercent", m.usedPercent());
                resp.put("isExternal", m.isExternal());
                resp.put("archivedFiles", m.totalArchivedFiles());
                resp.put("rawBytesSaved", m.totalRawBytesSaved());
                resp.put("compressedBytes", m.totalCompressedBytes());
                resp.put("savingsRatio", m.overallSavingsRatio());
                resp.put("indexedKnowledgeChunks", brain.getKnowledgeBase().getChunkCount());
                sendJsonResponse(exchange, 200, resp);
                return;
            }

            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                Map<String, Object> req = JsonParser.parseObject(body);
                String pathStr = (String) req.get("path");
                if (pathStr == null || pathStr.isBlank()) {
                    sendJsonResponse(exchange, 400, Map.of("error", "Caminho do arquivo/pasta não fornecido"));
                    return;
                }

                DocumentIngester.IngestionSummary sum = brain.getIngester().ingestPath(Paths.get(pathStr));
                sendJsonResponse(exchange, 200, Map.of(
                        "filesProcessed", sum.filesProcessed(),
                        "originalBytes", sum.totalOriginalBytes(),
                        "compressedBytes", sum.totalCompressedBytes(),
                        "savingsRatio", sum.savingsRatio(),
                        "titles", sum.ingestedTitles()
                ));
                return;
            }

            sendJsonResponse(exchange, 405, Map.of("error", "Método não permitido"));
        }
    }

    private class SystemHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            String info = brain.getSystemMetricsTool().execute("");
            sendJsonResponse(exchange, 200, Map.of("telemetry", info));
        }
    }

    private class MemoriesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 200, Map.of("memories", brain.getMemoryVault().getAllMemories()));
                return;
            }
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                Map<String, Object> req = JsonParser.parseObject(body);
                String category = (String) req.getOrDefault("category", "NOTE");
                String text = (String) req.get("text");
                if (text != null && !text.isBlank()) {
                    var entry = brain.getMemoryVault().addMemory(category, text, 5);
                    sendJsonResponse(exchange, 200, Map.of("success", true, "memory", entry));
                    return;
                }
                sendJsonResponse(exchange, 400, Map.of("error", "Texto da memória é obrigatório"));
                return;
            }
            sendJsonResponse(exchange, 405, Map.of("error", "Método não permitido"));
        }
    }

    private class HealthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            sendJsonResponse(exchange, 200, Map.of(
                    "status", "healthy",
                    "agent", "Cornelius",
                    "os", System.getProperty("os.name"),
                    "java", System.getProperty("java.version"),
                    "provider", brain.getConfig().getActiveProvider()
            ));
        }
    }

    private class ManifestHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            String manifest = """
                    {
                      "name": "Cornelius.AI Mobile",
                      "short_name": "Cornelius",
                      "description": "Mordomo Pessoal Autônomo e Controle do Computador",
                      "start_url": "/",
                      "display": "standalone",
                      "background_color": "#09090b",
                      "theme_color": "#09090b",
                      "orientation": "portrait-primary",
                      "icons": [
                        {
                          "src": "/cornelius.png",
                          "sizes": "192x192 512x512",
                          "type": "image/png",
                          "purpose": "any maskable"
                        }
                      ]
                    }
                    """;
            byte[] bytes = manifest.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/manifest+json; charset=UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    private class ServiceWorkerHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            String sw = """
                    self.addEventListener('install', (e) => { self.skipWaiting(); });
                    self.addEventListener('activate', (e) => { e.waitUntil(clients.claim()); });
                    self.addEventListener('fetch', (e) => {
                        e.respondWith(fetch(e.request).catch(() => caches.match(e.request)));
                    });
                    """;
            byte[] bytes = sw.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/javascript; charset=UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    private class IconHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Path icon = Paths.get(System.getProperty("user.dir"), "cornelius.png");
            if (!Files.exists(icon)) {
                icon = Paths.get(System.getProperty("user.dir"), "..", "cornelius.png").normalize();
            }
            if (!Files.exists(icon)) {
                icon = Paths.get("e:\\APP\\cornelius.png");
            }
            if (!Files.exists(icon)) {
                icon = Paths.get(System.getProperty("user.dir"), "logo.png");
            }
            if (Files.exists(icon)) {
                byte[] bytes = Files.readAllBytes(icon);
                exchange.getResponseHeaders().set("Content-Type", "image/png");
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            } else {
                exchange.sendResponseHeaders(404, -1);
            }
        }
    }

    private class WebUiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (!"/".equals(path) && !"/index.html".equals(path) && !"/mobile".equals(path)) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }

            String html = generateWebUiHtml();

            byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    private void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }

    private void sendJsonResponse(HttpExchange exchange, int statusCode, Object data) throws IOException {
        String json = JsonParser.toJson(data);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private String generateWebUiHtml() {
        try {
            Path[] candidates = new Path[]{
                Paths.get(System.getProperty("user.dir"), "Frontend", "mobile.html"),
                Paths.get(System.getProperty("user.dir"), "..", "Frontend", "mobile.html").normalize(),
                Paths.get("e:\\APP\\Frontend\\mobile.html"),
                Paths.get(System.getProperty("user.dir"), "mobile.html"),
                Paths.get(System.getProperty("user.dir"), "..", "mobile.html").normalize(),
                Paths.get("e:\\APP\\home.html"),
                Paths.get(System.getProperty("user.dir"), "home.html")
            };
            for (Path p : candidates) {
                if (Files.exists(p)) {
                    return Files.readString(p, StandardCharsets.UTF_8);
                }
            }
        } catch (Exception ignored) {}
        return "<!DOCTYPE html><html><head><title>Cornelius AI</title></head><body style='background:#09090b;color:#fff;font-family:sans-serif;padding:20px;'><h2>Cornelius.AI Server Active</h2><p>Acesse o painel no aplicativo Android ou navegador.</p></body></html>";
    }
}
