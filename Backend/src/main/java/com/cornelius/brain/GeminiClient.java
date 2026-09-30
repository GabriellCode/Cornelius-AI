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
import java.util.concurrent.Executors;

public class GeminiClient implements LLMClient {
    private final CorneliusConfig config;
    private final HttpClient httpClient;

    public GeminiClient(CorneliusConfig config) {
        this.config = config;
        // HTTP/2 with Virtual Threads executor for maximum throughput and lowest latency
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .executor(Executors.newVirtualThreadPerTaskExecutor())
                .connectTimeout(Duration.ofSeconds(8))
                .build();
    }

    @Override
    public String generate(String systemPrompt, String userPrompt, List<ChatMessage> history) throws Exception {
        String apiKey = config.getGeminiApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Chave de API do Gemini não configurada. Configure em Configurações.");
        }

        List<String> modelsToTry = new ArrayList<>();
        String preferred = config.getGeminiModel();
        if (preferred != null && !preferred.isBlank()) {
            modelsToTry.add(preferred);
        }
        // Ordered by speed and responsiveness
        // Ordered by quota resilience, speed and responsiveness
        if (!modelsToTry.contains("gemini-2.5-flash")) modelsToTry.add("gemini-2.5-flash");
        if (!modelsToTry.contains("gemini-1.5-flash")) modelsToTry.add("gemini-1.5-flash");
        if (!modelsToTry.contains("gemini-1.5-flash-8b")) modelsToTry.add("gemini-1.5-flash-8b");
        if (!modelsToTry.contains("gemini-2.5-pro")) modelsToTry.add("gemini-2.5-pro");
        if (!modelsToTry.contains("gemini-1.5-pro")) modelsToTry.add("gemini-1.5-pro");
        if (!modelsToTry.contains("gemini-3.5-flash")) modelsToTry.add("gemini-3.5-flash");
        if (!modelsToTry.contains("gemini-3.5-flash-lite")) modelsToTry.add("gemini-3.5-flash-lite");
        if (!modelsToTry.contains("gemini-3.6-flash")) modelsToTry.add("gemini-3.6-flash");
        if (!modelsToTry.contains("gemini-3.7-flash")) modelsToTry.add("gemini-3.7-flash");

        Exception lastEx = null;
        for (String model : modelsToTry) {
            try {
                return callModel(apiKey, model, systemPrompt, userPrompt, history);
            } catch (Exception e) {
                lastEx = e;
                Logger.warn("Gemini", "Modelo " + model + " falhou (" + e.getMessage() + "). Tentando modelo de contingência...");
            }
        }

        throw (lastEx != null) ? lastEx : new RuntimeException("Falha ao comunicar com a API do Gemini.");
    }

    private String callModel(String apiKey, String model, String systemPrompt, String userPrompt, List<ChatMessage> history) throws Exception {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey;

        Map<String, Object> payload = new LinkedHashMap<>();

        // System instruction
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            Map<String, Object> sysInst = new LinkedHashMap<>();
            sysInst.put("parts", List.of(Map.of("text", systemPrompt)));
            payload.put("system_instruction", sysInst);
        }

        // Contents (limit to last 10 messages for low latency TTFT)
        List<Map<String, Object>> contents = new ArrayList<>();
        if (history != null) {
            int start = Math.max(0, history.size() - 10);
            for (int i = start; i < history.size(); i++) {
                ChatMessage msg = history.get(i);
                String role = "user".equalsIgnoreCase(msg.role()) ? "user" : "model";
                contents.add(Map.of(
                        "role", role,
                        "parts", List.of(Map.of("text", msg.content()))
                ));
            }
        }

        // Current message
        contents.add(Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", userPrompt))
        ));
        payload.put("contents", contents);

        // Low latency generation config
        Map<String, Object> genConfig = new LinkedHashMap<>();
        genConfig.put("temperature", 0.6);
        genConfig.put("maxOutputTokens", 2048);
        genConfig.put("topP", 0.95);
        payload.put("generationConfig", genConfig);

        String jsonPayload = JsonParser.toJson(payload);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            Logger.error("Gemini", "Erro da API Gemini [" + model + "] (" + response.statusCode() + "): " + response.body());
            Map<String, Object> errObj = JsonParser.parseObject(response.body());
            if (errObj != null && errObj.containsKey("error") && errObj.get("error") instanceof Map emap) {
                Object msg = emap.get("message");
                if (msg != null) {
                    throw new RuntimeException("Erro Gemini API (" + model + "): " + msg);
                }
            }
            throw new RuntimeException("Erro ao conectar à API Gemini (" + model + " - HTTP " + response.statusCode() + ")");
        }

        return extractResponseText(response.body());
    }

    @SuppressWarnings("unchecked")
    private String extractResponseText(String responseBody) {
        try {
            Map<String, Object> map = JsonParser.parseObject(responseBody);
            if (map == null || !map.containsKey("candidates")) {
                return "Perdão, senhor, a resposta do Gemini veio vazia.";
            }

            List<Object> candidates = (List<Object>) map.get("candidates");
            if (candidates.isEmpty()) {
                return "Perdão, senhor, nenhuma resposta foi gerada.";
            }

            Map<String, Object> firstCandidate = (Map<String, Object>) candidates.get(0);
            Map<String, Object> content = (Map<String, Object>) firstCandidate.get("content");
            List<Object> parts = (List<Object>) content.get("parts");

            StringBuilder text = new StringBuilder();
            for (Object part : parts) {
                if (part instanceof Map pmap && pmap.containsKey("text")) {
                    text.append(pmap.get("text"));
                }
            }
            return text.toString().trim();
        } catch (Exception e) {
            Logger.error("Gemini", "Falha ao decodificar JSON de resposta: " + e.getMessage());
            throw new RuntimeException("Erro ao processar resposta do modelo", e);
        }
    }

    @Override
    public boolean isAvailable() {
        String key = config.getGeminiApiKey();
        return key != null && !key.isBlank();
    }

    @Override
    public String getProviderName() {
        return "Google Gemini (" + config.getGeminiModel() + ")";
    }
}
