package com.cornelius.bot;

import com.cornelius.brain.CorneliusBrain;
import com.cornelius.config.CorneliusConfig;
import com.cornelius.util.JsonParser;
import com.cornelius.util.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class DiscordBotService implements WebSocket.Listener {
    private final CorneliusBrain brain;
    private final CorneliusConfig config;
    private final HttpClient httpClient;
    private WebSocket webSocket;
    private ScheduledExecutorService heartbeatExecutor;
    private ScheduledExecutorService supervisorExecutor;
    private Integer lastSequence = null;
    private String sessionId = null;
    private String resumeGatewayUrl = null;
    private boolean isRunning = false;
    private final AtomicBoolean isConnecting = new AtomicBoolean(false);
    private String botUserId = null;
    private String botUsername = "Cornelius";
    private final Object wsSendLock = new Object();
    private final AtomicLong lastHeartbeatAck = new AtomicLong(System.currentTimeMillis());

    // Intents: GUILDS (1) + GUILD_MESSAGES (512) + DIRECT_MESSAGES (4096) + MESSAGE_CONTENT (32768) = 37377
    private int currentIntents = 37377;

    public DiscordBotService(CorneliusBrain brain, CorneliusConfig config) {
        this.brain = brain;
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        startImmortalSupervisor();
    }

    private void startImmortalSupervisor() {
        supervisorExecutor = Executors.newSingleThreadScheduledExecutor();
        // Check health every 8 seconds - if disconnected, automatically reconnect
        supervisorExecutor.scheduleWithFixedDelay(() -> {
            try {
                String token = config.getDiscordToken();
                if (token != null && !token.isBlank() && !isRunning && !isConnecting.get()) {
                    Logger.warn("DiscordBot", "Supervisor: Conexão inativa detectada. Reconectando automaticamente...");
                    start();
                }
            } catch (Throwable t) {
                Logger.error("DiscordBot", "Erro no supervisor de conexão: " + t.getMessage());
            }
        }, 10, 8, TimeUnit.SECONDS);
    }

    public synchronized void start() {
        String token = config.getDiscordToken();
        if (token == null || token.isBlank()) {
            Logger.info("DiscordBot", "Token do Discord não configurado. Serviço pausado.");
            return;
        }

        if (isConnecting.get()) {
            return;
        }
        isConnecting.set(true);

        if (isRunning) {
            stop();
            closeSocketSilently();
        }

        token = token.trim();

        // 1. Pre-flight REST verification
        if (!validateToken(token)) {
            isConnecting.set(false);
            return;
        }

        Logger.info("DiscordBot", "Conectando Cornelius ao Discord Gateway (Intents: " + currentIntents + ")...");

        try {
            HttpClient wsClient = HttpClient.newHttpClient();
            String gatewayUri = (resumeGatewayUrl != null && !resumeGatewayUrl.isBlank())
                    ? resumeGatewayUrl + "/?v=10&encoding=json"
                    : "wss://gateway.discord.gg/?v=10&encoding=json";

            CompletableFuture<WebSocket> wsFuture = wsClient.newWebSocketBuilder()
                    .connectTimeout(Duration.ofSeconds(15))
                    .buildAsync(URI.create(gatewayUri), this);

            this.webSocket = wsFuture.get(15, TimeUnit.SECONDS);
            this.isRunning = true;
            this.lastHeartbeatAck.set(System.currentTimeMillis());
            Logger.success("DiscordBot", "Conexão WebSocket com Discord estabelecida!");
        } catch (Exception e) {
            Logger.error("DiscordBot", "Falha ao conectar ao Discord: " + e.getMessage());
            Logger.error("DiscordBot", "Falha ao conectar ao Discord: " + e.getMessage() + ". Agendando nova tentativa em 4 segundos...");
            this.isRunning = false;
            scheduleReconnect(4);
        } finally {
            isConnecting.set(false);
        }
    }

    private boolean validateToken(String token) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("https://discord.com/api/v10/users/@me"))
                    .header("Authorization", "Bot " + token)
                    .timeout(Duration.ofSeconds(8))
                    .GET()
                    .build();

            HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() == 200) {
                Map<String, Object> user = JsonParser.parseObject(res.body());
                if (user != null) {
                    botUsername = (String) user.get("username");
                    botUserId = (String) user.get("id");
                    Logger.success("DiscordBot", "Token validado com sucesso! Bot: @" + botUsername + " (ID: " + botUserId + ")");
                    return true;
                }
            } else if (res.statusCode() == 401) {
                Logger.error("DiscordBot", "❌ TOKEN DO DISCORD INVÁLIDO OU EXPIRADO (HTTP 401 Unauthorized).");
                Logger.warn("DiscordBot", "👉 Acesse https://discord.com/developers/applications, selecione seu Bot, vá em 'Bot' -> 'Reset Token', copie o novo token e cole no Cornelius.");
                return false;
            } else {
                Logger.warn("DiscordBot", "Aviso da API do Discord (HTTP " + res.statusCode() + "): " + res.body());
            }
        } catch (Exception e) {
            Logger.warn("DiscordBot", "Verificação prévia do token falhou por rede (" + e.getMessage() + "). Tentando gateway diretamente...");
            Logger.warn("DiscordBot", "Verificação prévia do token falhou (" + e.getMessage() + "). Conectando via Gateway...");
        }
        return true;
    }

    public synchronized void stop() {
        isRunning = false;
        closeSocketSilently();
        Logger.info("DiscordBot", "Serviço do Discord encerrado manualmente.");
    }

    private void closeSocketSilently() {
        if (heartbeatExecutor != null && !heartbeatExecutor.isShutdown()) {
            heartbeatExecutor.shutdownNow();
        }
        if (webSocket != null) {
            try {
                webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Stopping");
                webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Reconnecting");
            } catch (Exception ignored) {}
            webSocket = null;
        }
        Logger.info("DiscordBot", "Serviço do Discord encerrado.");
    }

    public boolean isRunning() {
        return isRunning && webSocket != null;
    }

    @Override
    public void onOpen(WebSocket webSocket) {
        this.webSocket = webSocket;
        this.isRunning = true;
        this.lastHeartbeatAck.set(System.currentTimeMillis());
        Logger.info("DiscordBot", "Canal de comunicação do Discord aberto.");
        webSocket.request(1);
    }

    private final StringBuilder buffer = new StringBuilder();

    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
        try {
            this.webSocket = webSocket;
            synchronized (buffer) {
                buffer.append(data);
                if (last) {
                    String fullMessage = buffer.toString();
                    buffer.setLength(0);
                    handleGatewayMessage(fullMessage);
                }
            }
        } catch (Throwable t) {
            Logger.error("DiscordBot", "Erro ao processar frame de texto: " + t.getMessage());
        } finally {
            webSocket.request(1);
        }
        return null;
    }

    @Override
    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
        Logger.warn("DiscordBot", "Conexão Discord fechada (" + statusCode + "): " + reason);
        this.isRunning = false;
        if (heartbeatExecutor != null) heartbeatExecutor.shutdownNow();

        // Error code 4014 = Disallowed Intents -> fallback to standard unprivileged intents (4609 = Guilds + DMs + Messages)
        // If intent rejected (4014)
        if (statusCode == 4014 && currentIntents != 4609) {
            Logger.warn("DiscordBot", "Intent privilegiada (Message Content) não ativada no Portal. Reconectando com modo DM & Mentions padrão (4609)...");
            Logger.warn("DiscordBot", "Intent privilegiada (Message Content) recusada. Reconectando com modo DM & Mentions padrão (4609)...");
            currentIntents = 4609;
            Executors.newSingleThreadScheduledExecutor().schedule(() -> {
                try {
                    start();
                } catch (Exception ex) {
                    Logger.error("DiscordBot", "Erro ao reconectar: " + ex.getMessage());
                }
            }, 2, TimeUnit.SECONDS);
            scheduleReconnect(2);
        } else if (statusCode == 4004) {
            Logger.error("DiscordBot", "Autenticação recusada pelo Discord (Token inválido ou expirado).");
            Logger.error("DiscordBot", "Autenticação recusada pelo Discord (Token inválido).");
        } else {
            // AUTOMATIC SELF-HEALING RECONNECT FOR ANY OTHER CODE (1000, 1001, 1006, etc.)
            Logger.info("DiscordBot", "🔄 Reconectando automaticamente ao Discord em 3 segundos...");
            scheduleReconnect(3);
        }

        return null;
    }

    @Override
    public void onError(WebSocket webSocket, Throwable error) {
        Logger.error("DiscordBot", "Erro no WebSocket do Discord: " + error.getMessage());
        this.isRunning = false;
        scheduleReconnect(3);
        webSocket.request(1);
    }

    private void handleGatewayMessage(String json) {
        try {
            Map<String, Object> packet = JsonParser.parseObject(json);
            if (packet == null) return;

            Object opObj = packet.get("op");
            int op = (opObj instanceof Number n) ? n.intValue() : -1;

            if (packet.containsKey("s") && packet.get("s") instanceof Number s) {
                lastSequence = s.intValue();
            }

            String eventName = (String) packet.get("t");
            if (op != 11) { // Don't spam heartbeat acks
                Logger.info("DiscordBot", "Evento Gateway: op=" + op + (eventName != null ? ", t=" + eventName : ""));
            }

            // Opcode 10: Hello -> Start heartbeat & Identify
            // Opcode 10: Hello -> Start heartbeat & Identify/Resume
            if (op == 10) {
                if (packet.get("d") instanceof Map d) {
                    long interval = d.get("heartbeat_interval") instanceof Number h ? h.longValue() : 41250;
                    startHeartbeat(interval);
                    sendIdentify();
                    if (sessionId != null && lastSequence != null) {
                        sendResume();
                    } else {
                        sendIdentify();
                    }
                }
            }
            // Opcode 11: Heartbeat ACK
            else if (op == 11) {
                // Heartbeat acknowledged by Discord
                lastHeartbeatAck.set(System.currentTimeMillis());
            }
            // Opcode 7: Reconnect requested by Discord
            else if (op == 7) {
                Logger.warn("DiscordBot", "Discord solicitou reconexão (Opcode 7). Reconectando imediatamente...");
                closeSocketSilently();
                scheduleReconnect(1);
            }
            // Opcode 9: Invalid Session
            else if (op == 9) {
                Logger.warn("DiscordBot", "Sessão Discord invalidada (Opcode 9). Reiniciando identificação limpa...");
                sessionId = null;
                lastSequence = null;
                closeSocketSilently();
                scheduleReconnect(2);
            }
            // Dispatch Events (Opcode 0)
            else if (op == 0 && "MESSAGE_CREATE".equalsIgnoreCase(eventName)) {
                if (packet.get("d") instanceof Map msgData) {
                    handleMessageCreate(msgData);
                }
            }
            else if (op == 0 && "READY".equalsIgnoreCase(eventName)) {
    if (packet.get("d") instanceof Map d && d.containsKey("user") && d.get("user") instanceof Map user) {
        botUsername = (String) user.get("username");
        botUserId = (String) user.get("id");
        Logger.success("DiscordBot", "🟢 Cornelius está ONLINE no Discord como @" + botUsername + " (ID: " + botUserId + ")!");
    }
    if (packet.get("d") instanceof Map d) {
        if (d.containsKey("session_id")) {
            this.sessionId = (String) d.get("session_id");
        }
        if (d.containsKey("resume_gateway_url")) {
            this.resumeGatewayUrl = (String) d.get("resume_gateway_url");
        }
        if (d.containsKey("user") && d.get("user") instanceof Map user) {
            botUsername = (String) user.get("username");
            botUserId = (String) user.get("id");
            Logger.success("DiscordBot", "🟢 Cornelius está ONLINE no Discord como @" + botUsername + " (ID: " + botUserId + ")!");
        }
    }
}
            else if (op == 0 && "RESUMED".equalsIgnoreCase(eventName)) {
                Logger.success("DiscordBot", "🟢 Sessão do Discord recuperada com sucesso (RESUMED)!");
            }
        } catch (Exception e) {
            Logger.error("DiscordBot", "Erro ao processar pacote do gateway: " + e.getMessage());
        }
    }

    private void scheduleReconnect(int delaySeconds) {
        if (isConnecting.get()) return;
        Executors.newSingleThreadScheduledExecutor().schedule(() -> {
            try {
                start();
            } catch (Exception ex) {
                Logger.error("DiscordBot", "Erro na tentativa de reconexão: " + ex.getMessage());
            }
        }, delaySeconds, TimeUnit.SECONDS);
    }

    private void startHeartbeat(long intervalMs) {
        if (heartbeatExecutor != null && !heartbeatExecutor.isShutdown()) {
            heartbeatExecutor.shutdownNow();
        }
        heartbeatExecutor = Executors.newSingleThreadScheduledExecutor();
        heartbeatExecutor.scheduleAtFixedRate(() -> {
            try {
                // Heartbeat ACK watchdog
                long now = System.currentTimeMillis();
                if (now - lastHeartbeatAck.get() > intervalMs * 2.5) {
                    Logger.warn("DiscordBot", "Heartbeat ACK atrasado há mais de " + (now - lastHeartbeatAck.get()) + "ms. Forçando reconexão preventiva...");
                    closeSocketSilently();
                    scheduleReconnect(1);
                    return;
                }

                Map<String, Object> heartbeat = new HashMap<>();
                heartbeat.put("op", 1);
                heartbeat.put("d", lastSequence);
                sendWsPayload(JsonParser.toJson(heartbeat));
            } catch (Exception e) {
                Logger.warn("DiscordBot", "Falha ao enviar heartbeat: " + e.getMessage());
            }
        }, (long) (intervalMs * Math.random()), intervalMs, TimeUnit.MILLISECONDS);
    }

    private void sendResume() {
        String token = config.getDiscordToken();
        Map<String, Object> resume = new LinkedHashMap<>();
        resume.put("op", 6);

        Map<String, Object> d = new LinkedHashMap<>();
        d.put("token", token);
        d.put("session_id", sessionId);
        d.put("seq", lastSequence);
        resume.put("d", d);

        sendWsPayload(JsonParser.toJson(resume));
        Logger.info("DiscordBot", "Enviando pacote RESUME para recuperar sessão anterior...");
    }

    private void sendIdentify() {
        String token = config.getDiscordToken();
        Map<String, Object> identify = new LinkedHashMap<>();
        identify.put("op", 2);

        Map<String, Object> d = new LinkedHashMap<>();
        d.put("token", token);
        d.put("intents", currentIntents);

        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("os", "linux");
        properties.put("browser", "cornelius");
        properties.put("device", "cornelius");
        d.put("properties", properties);

        // Explicit Online Presence & Custom Activity Status
        Map<String, Object> activity = new LinkedHashMap<>();
        activity.put("name", "ao seu dispor, senhor 🎩");
        activity.put("type", 0); // 0 = Playing

        Map<String, Object> presence = new LinkedHashMap<>();
        presence.put("status", "online");
        presence.put("since", 0);
        presence.put("afk", false);
        presence.put("activities", List.of(activity));
        d.put("presence", presence);

        identify.put("d", d);

        sendWsPayload(JsonParser.toJson(identify));
        Logger.info("DiscordBot", "Autenticando Bot e definindo status ONLINE com Discord Gateway...");
    }

    private void sendWsPayload(String json) {
        synchronized (wsSendLock) {
            if (webSocket != null) {
                try {
                    webSocket.sendText(json, true).join();
                } catch (Exception e) {
                    Logger.error("DiscordBot", "Erro ao enviar payload no WebSocket: " + e.getMessage());
                }
            }
        }
    }

    private void handleMessageCreate(Map<String, Object> msgData) {
        try {
            // Check author
            String authorId = null;
            String authorName = "Usuário";
            if (msgData.containsKey("author") && msgData.get("author") instanceof Map author) {
                authorId = (String) author.get("id");
                authorName = (String) author.get("username");
                if (botUserId != null && botUserId.equals(authorId)) {
                    return; // Ignore own messages
                }
                if (Boolean.TRUE.equals(author.get("bot"))) {
                    return; // Ignore other bots
                }
            }

            String content = (String) msgData.get("content");
            String channelId = (String) msgData.get("channel_id");
            if (channelId == null) return;

            Logger.info("DiscordBot", "📩 Mensagem recebida de @" + authorName + " (Canal " + channelId + "): '" + content + "'");

            if (content == null || content.isBlank()) {
                // If content is empty because of permissions on server, check for mentions
                if (msgData.containsKey("mentions") && msgData.get("mentions") instanceof List mentions && !mentions.isEmpty()) {
                    content = "Olá Cornelius!";
                } else {
                    return;
                }
            }

            // Strip mentions or prefixes like <@botId> or !c or /cornelius
            // Strip mentions or prefixes
            if (botUserId != null) {
                content = content.replaceAll("<@!?" + botUserId + ">", "").trim();
            }
            if (content.startsWith("!c ") || content.startsWith("!c:") || content.startsWith("/c ")) {
                content = content.substring(3).trim();
            }

            if (content.isBlank()) content = "Olá Cornelius!";

            Logger.info("DiscordBot", "⚡ Processando comando de @" + authorName + ": " + content);

            // Send typing indicator to Discord
            sendTypingIndicator(channelId);

            final String cleanPrompt = content;

            // Process with Cornelius Brain in virtual thread
            Thread.ofVirtual().name("Cornelius-Discord-Task").start(() -> {
                try {
                    CorneliusBrain.BrainResponse response = brain.processUserMessage(cleanPrompt);
                    sendMessageToChannel(channelId, response.text());
                } catch (Exception e) {
                    sendMessageToChannel(channelId, "Perdão, senhor, ocorreu uma falha ao processar sua mensagem: " + e.getMessage());
                }
            });
        } catch (Exception e) {
            Logger.error("DiscordBot", "Falha ao processar mensagem do Discord: " + e.getMessage());
        }
    }

    public void sendTypingIndicator(String channelId) {
        String token = config.getDiscordToken();
        if (token == null || token.isBlank()) return;
        try {
            String url = "https://discord.com/api/v10/channels/" + channelId + "/typing";
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bot " + token)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(5))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
            httpClient.sendAsync(req, HttpResponse.BodyHandlers.discarding());
        } catch (Exception ignored) {}
    }

    public void sendMessageToChannel(String channelId, String text) {
        String token = config.getDiscordToken();
        if (token == null || token.isBlank()) return;

        // Discord 2000 character limit split
        List<String> chunks = splitIntoDiscordChunks(text, 1950);

        for (String chunk : chunks) {
            try {
                String url = "https://discord.com/api/v10/channels/" + channelId + "/messages";
                Map<String, Object> payload = Map.of("content", chunk);
                String json = JsonParser.toJson(payload);

                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Authorization", "Bot " + token)
                        .header("Content-Type", "application/json")
                        .timeout(Duration.ofSeconds(15))
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build();

                HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
                if (res.statusCode() >= 200 && res.statusCode() < 300) {
                    Logger.success("DiscordBot", "✅ Resposta enviada com sucesso no Discord (Canal: " + channelId + ")!");
                } else {
                    Logger.error("DiscordBot", "❌ Erro da API do Discord ao enviar mensagem (" + res.statusCode() + "): " + res.body());
                }
            } catch (Exception e) {
                Logger.error("DiscordBot", "Erro ao enviar resposta no Discord: " + e.getMessage());
            }
        }
    }

    private List<String> splitIntoDiscordChunks(String text, int maxLen) {
        List<String> chunks = new ArrayList<>();
        if (text == null) return chunks;
        while (text.length() > maxLen) {
            int splitIdx = text.lastIndexOf('\n', maxLen);
            if (splitIdx == -1) splitIdx = maxLen;
            chunks.add(text.substring(0, splitIdx));
            text = text.substring(splitIdx).trim();
        }
        if (!text.isEmpty()) {
            chunks.add(text);
        }
        return chunks;
    }
}
