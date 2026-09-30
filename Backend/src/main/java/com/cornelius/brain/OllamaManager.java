package com.cornelius.brain;

import com.cornelius.config.CorneliusConfig;
import com.cornelius.util.JsonParser;
import com.cornelius.util.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class OllamaManager {
    private static Process daemonProcess = null;
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    public static boolean isInstalled() {
        return findOllamaExecutable() != null;
    }

    public static String findOllamaExecutable() {
        String[] commonPaths = {
                "/usr/local/bin/ollama",
                "/usr/bin/ollama",
                "/bin/ollama",
                System.getProperty("user.home") + "/.local/bin/ollama",
                System.getProperty("user.home") + "/bin/ollama"
        };

        for (String p : commonPaths) {
            File f = new File(p);
            if (f.exists() && f.canExecute()) {
                return f.getAbsolutePath();
            }
        }

        // Try `which ollama`
        try {
            Process p = new ProcessBuilder("which", "ollama").start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line = reader.readLine();
                if (line != null && !line.isBlank()) {
                    File f = new File(line.trim());
                    if (f.exists() && f.canExecute()) {
                        return f.getAbsolutePath();
                    }
                }
            }
        } catch (Exception ignored) {}

        return null;
    }

    public static boolean isRunning(String ollamaUrl) {
        if (ollamaUrl == null || ollamaUrl.isBlank()) {
            ollamaUrl = "http://localhost:11434";
        }
        if (ollamaUrl.endsWith("/")) {
            ollamaUrl = ollamaUrl.substring(0, ollamaUrl.length() - 1);
        }

        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(ollamaUrl + "/api/tags"))
                    .timeout(Duration.ofSeconds(2))
                    .GET()
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            return resp.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean hasModel(String ollamaUrl, String modelName) {
        if (ollamaUrl == null || ollamaUrl.isBlank()) ollamaUrl = "http://localhost:11434";
        if (ollamaUrl.endsWith("/")) ollamaUrl = ollamaUrl.substring(0, ollamaUrl.length() - 1);

        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(ollamaUrl + "/api/tags"))
                    .timeout(Duration.ofSeconds(3))
                    .GET()
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                Map<String, Object> map = JsonParser.parseObject(resp.body());
                if (map != null && map.containsKey("models") && map.get("models") instanceof List list) {
                    for (Object item : list) {
                        if (item instanceof Map m && m.containsKey("name")) {
                            String name = (String) m.get("name");
                            if (name.startsWith(modelName)) {
                                return true;
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        return false;
    }

    public static synchronized void startDaemon(String ollamaUrl, Consumer<String> listener) throws Exception {
        if (isRunning(ollamaUrl)) {
            listener.accept("Ollama já está em execução e respondendo em " + ollamaUrl);
            return;
        }

        String execPath = findOllamaExecutable();
        if (execPath == null) {
            throw new IllegalStateException("Ollama não está instalado no sistema. Execute 'curl -fsSL https://ollama.com/install.sh | sh' no terminal.");
        }

        listener.accept("Iniciando serviço do Ollama local...");
        ProcessBuilder pb = new ProcessBuilder(execPath, "serve");
        pb.redirectErrorStream(true);

        File logDir = new File(System.getProperty("user.home") + "/.cornelius");
        logDir.mkdirs();
        File logFile = new File(logDir, "ollama.log");
        pb.redirectOutput(ProcessBuilder.Redirect.appendTo(logFile));

        daemonProcess = pb.start();

        // Wait up to 10 seconds for daemon to respond
        for (int i = 0; i < 20; i++) {
            Thread.sleep(500);
            if (isRunning(ollamaUrl)) {
                listener.accept("Ollama local iniciado com sucesso!");
                return;
            }
        }

        if (isRunning(ollamaUrl)) {
            listener.accept("Ollama local pronto.");
        } else {
            throw new RuntimeException("O serviço do Ollama foi iniciado mas não respondeu na porta 11434.");
        }
    }

    public static void pullModel(String ollamaUrl, String modelName, Consumer<String> listener) throws Exception {
        if (ollamaUrl == null || ollamaUrl.isBlank()) ollamaUrl = "http://localhost:11434";
        if (ollamaUrl.endsWith("/")) ollamaUrl = ollamaUrl.substring(0, ollamaUrl.length() - 1);

        listener.accept("Baixando modelo local '" + modelName + "'... Isso pode levar alguns minutos.");

        Map<String, Object> payload = Map.of("name", modelName, "stream", false);
        String json = JsonParser.toJson(payload);

        HttpRequest req = HttpRequest.newBuilder(URI.create(ollamaUrl + "/api/pull"))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofMinutes(15))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpClient longClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();

        HttpResponse<String> resp = longClient.send(req, HttpResponse.BodyHandlers.ofString());

        if (resp.statusCode() == 200) {
            listener.accept("Modelo '" + modelName + "' baixado e pronto para uso!");
        } else {
            throw new RuntimeException("Falha ao baixar modelo (HTTP " + resp.statusCode() + "): " + resp.body());
        }
    }
}

