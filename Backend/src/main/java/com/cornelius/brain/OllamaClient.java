package com.cornelius.brain;

import com.cornelius.config.CorneliusConfig;
import com.cornelius.util.JsonParser;
import com.cornelius.util.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

public class OllamaClient implements LLMClient {
    private final CorneliusConfig config;
    private final HttpClient httpClient;

    public OllamaClient(CorneliusConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(6))
                .build();
    }

    @Override
    public String generate(String systemPrompt, String userPrompt, List<ChatMessage> history) throws Exception {
        String baseUrl = getBaseUrl();

        // 1. Auto-discover installed models in Ollama to prevent 404 errors
        List<String> installedModels = getInstalledModels(baseUrl);
        if (installedModels.isEmpty()) {
            throw new IllegalStateException("Nenhum modelo baixado no Ollama local. Use o botão '⚡ Start Ollama' ou execute 'ollama pull llama3.2'.");
        }

        String targetModel = resolveBestModel(config.getOllamaModel(), installedModels);

        // 2. First attempt: /api/chat
        try {
            return callChatApi(baseUrl, targetModel, systemPrompt, userPrompt, history);
        } catch (Exception e1) {
            Logger.warn("Ollama", "Falha no endpoint /api/chat (" + e1.getMessage() + "). Tentando /api/generate...");
            // 3. Second attempt: /api/generate (fallback for models without chat templates or on 500 errors)
            try {
                return callGenerateApi(baseUrl, targetModel, systemPrompt, userPrompt);
            } catch (Exception e2) {
                Logger.error("Ollama", "Falha em ambos os endpoints do Ollama: " + e2.getMessage());
                throw new RuntimeException("Ollama local retornou erro (" + e1.getMessage() + ").");
            }
        }
    }

    private String callChatApi(String baseUrl, String model, String systemPrompt, String userPrompt, List<ChatMessage> history) throws Exception {
        String url = baseUrl + "/api/chat";
        List<Map<String, String>> messages = new ArrayList<>();

        if (systemPrompt != null && !systemPrompt.isBlank()) {
            messages.add(Map.of("role", "system", "content", systemPrompt));
        }

        if (history != null) {
            // Keep last 10 messages to avoid Ollama 500 context overflow
            int start = Math.max(0, history.size() - 10);
            for (int i = start; i < history.size(); i++) {
                ChatMessage msg = history.get(i);
                messages.add(Map.of("role", msg.role(), "content", msg.content()));
            }
        }

        messages.add(Map.of("role", "user", "content", userPrompt));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", model);
        payload.put("messages", messages);
        payload.put("stream", false);

        Map<String, Object> options = new LinkedHashMap<>();
        options.put("temperature", 0.7);
        options.put("num_predict", 1024);
        payload.put("options", options);

        String jsonPayload = JsonParser.toJson(payload);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(60))
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("HTTP " + response.statusCode() + ": " + response.body());
        }

        Map<String, Object> map = JsonParser.parseObject(response.body());
        if (map != null && map.containsKey("message") && map.get("message") instanceof Map msgMap) {
            Object content = msgMap.get("content");
            if (content != null) {
                return content.toString().trim();
            }
        }

        throw new RuntimeException("Resposta vazia do endpoint /api/chat");
    }

    private String callGenerateApi(String baseUrl, String model, String systemPrompt, String userPrompt) throws Exception {
        String url = baseUrl + "/api/generate";

        StringBuilder fullPrompt = new StringBuilder();
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            fullPrompt.append("### Instruções do Sistema:\n").append(systemPrompt).append("\n\n");
        }
        fullPrompt.append("### Usuário:\n").append(userPrompt).append("\n\n### Cornelius:\n");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", model);
        payload.put("prompt", fullPrompt.toString());
        payload.put("stream", false);

        String jsonPayload = JsonParser.toJson(payload);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(60))
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("HTTP " + response.statusCode() + ": " + response.body());
        }

        Map<String, Object> map = JsonParser.parseObject(response.body());
        if (map != null && map.containsKey("response")) {
            return map.get("response").toString().trim();
        }

        throw new RuntimeException("Resposta vazia do endpoint /api/generate");
    }

    private String resolveBestModel(String configured, List<String> available) {
        if (available.isEmpty()) return configured != null ? configured : "llama3.2";

        if (configured != null && !configured.isBlank()) {
            // Check exact match or prefix match (e.g. "llama3.2" matches "llama3.2:latest")
            for (String m : available) {
                if (m.equalsIgnoreCase(configured) || m.startsWith(configured + ":") || m.startsWith(configured)) {
                    return m;
                }
            }
        }

        // Return first available model
        return available.get(0);
    }

    public List<String> getInstalledModels(String baseUrl) {
        List<String> list = new ArrayList<>();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/tags"))
                    .timeout(Duration.ofSeconds(3))
                    .GET()
                    .build();
            HttpResponse<String> res = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() == 200) {
                Map<String, Object> map = JsonParser.parseObject(res.body());
                if (map != null && map.containsKey("models") && map.get("models") instanceof List rawList) {
                    for (Object item : rawList) {
                        if (item instanceof Map m && m.containsKey("name")) {
                            list.add((String) m.get("name"));
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        return list;
    }

    private String getBaseUrl() {
        String baseUrl = config.getOllamaUrl();
        if (baseUrl == null || baseUrl.isBlank()) baseUrl = "http://localhost:11434";
        if (baseUrl.endsWith("/")) baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        return baseUrl;
    }

    @Override
    public boolean isAvailable() {
        try {
            List<String> models = getInstalledModels(getBaseUrl());
            return !models.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String getProviderName() {
        return "Ollama Local (" + config.getOllamaModel() + ")";
    }
}
