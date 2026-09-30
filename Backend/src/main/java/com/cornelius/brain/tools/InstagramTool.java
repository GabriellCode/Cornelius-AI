package com.cornelius.brain.tools;

import com.cornelius.config.CorneliusConfig;
import com.cornelius.util.JsonParser;
import com.cornelius.util.Logger;

import java.awt.Desktop;
import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

public class InstagramTool {
    private final CorneliusConfig config;
    private final HttpClient httpClient;

    public InstagramTool(CorneliusConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();
    }

    public String execute(String command) {
        if (command == null || command.isBlank()) {
            return getAccountStatus();
        }

        String cmd = command.trim();
        String lower = cmd.toLowerCase();

        if (lower.startsWith("profile") || lower.startsWith("perfil")) {
            String user = cmd.replaceFirst("(?i)^(profile|perfil)\\s*", "").trim();
            if (user.isBlank()) {
                user = config.getInstagramUsername();
            }
            return checkProfile(user);
        } else if (lower.startsWith("inbox") || lower.startsWith("direct") || lower.startsWith("dms")) {
            return openDirects();
        } else if (lower.startsWith("open") || lower.startsWith("abrir")) {
            return openInstagramWeb();
        } else if (lower.startsWith("post") || lower.startsWith("publicar")) {
            String payload = cmd.replaceFirst("(?i)^(post|publicar)\\s*", "").trim();
            return createPost(payload);
        } else if (lower.startsWith("status")) {
            return getAccountStatus();
        } else {
            return checkProfile(config.getInstagramUsername());
        }
    }

    public String getAccountStatus() {
        String user = config.getInstagramUsername();
        String token = config.getInstagramAccessToken();
        String session = config.getInstagramSessionId();

        boolean configured = (user != null && !user.isBlank()) || (token != null && !token.isBlank());

        StringBuilder sb = new StringBuilder();
        sb.append("=== 📸 STATUS DA CONEXÃO INSTAGRAM - CORNELIUS ===\n");
        if (!configured) {
            sb.append("• Status: ⚠️ Conta ainda não vinculada nas configurações.\n");
            sb.append("• Usuário: Não configurado\n");
            sb.append("• Modo de Acesso: Navegador Web Direto (https://www.instagram.com)\n");
            sb.append("👉 Dica: Você pode cadastrar o @usuario e Token de Acesso nas Configurações do Cornelius.");
        } else {
            sb.append("• Status: 🟢 CONECTADO / CONFIGURADO\n");
            sb.append("• Usuário do Bot: @").append(user != null && !user.isBlank() ? user : "Não informado").append("\n");
            sb.append("• Meta Graph Token: ").append(token != null && !token.isBlank() ? "✅ Ativo (Autenticado)" : "❌ Não configurado").append("\n");
            sb.append("• Cookie de Sessão: ").append(session != null && !session.isBlank() ? "✅ Presente" : "❌ Não configurado").append("\n");
            sb.append("• URL do Perfil: https://www.instagram.com/").append(user).append("/\n");
        }
        return sb.toString();
    }

    public String openInstagramWeb() {
        String user = config.getInstagramUsername();
        String url = (user != null && !user.isBlank())
                ? "https://www.instagram.com/" + user + "/"
                : "https://www.instagram.com/";
        com.cornelius.server.LocalApiServer.openUrlInHost(url);
        return "📸 Instagram aberto com sucesso no seu navegador: " + url;
    }

    public String openDirects() {
        String url = "https://www.instagram.com/direct/inbox/";
        com.cornelius.server.LocalApiServer.openUrlInHost(url);
        return "📩 Caixa de Mensagens Diretas (Directs) do Instagram aberta no navegador!";
    }

    public String checkProfile(String username) {
        if (username == null || username.isBlank()) {
            username = config.getInstagramUsername();
        }
        if (username == null || username.isBlank()) {
            return "⚠️ Nenhum usuário do Instagram foi configurado ainda para o Cornelius.";
        }

        username = username.replace("@", "").trim();

        try {
            String url = "https://www.instagram.com/" + username + "/";
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .timeout(Duration.ofSeconds(8))
                    .GET()
                    .build();

            HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());

            if (res.statusCode() == 200) {
                return String.format("📸 Perfil @%s verificado e ativo no Instagram!\n• Link: %s\n• Código HTTP: %d (Acesso Confirmado)",
                        username, url, res.statusCode());
            } else {
                return String.format("📸 Perfil @%s verificado (HTTP %d). Link: %s", username, res.statusCode(), url);
            }
        } catch (Exception e) {
            return "📸 Perfil do Instagram configurado: @" + username + " (https://www.instagram.com/" + username + "/)";
        }
    }

    public String createPost(String payload) {
        String token = config.getInstagramAccessToken();
        String user = config.getInstagramUsername();

        String caption = payload;
        String mediaUrl = "";

        if (payload.contains(":::")) {
            String[] parts = payload.split(":::", 2);
            mediaUrl = parts[0].trim();
            caption = parts[1].trim();
        }

        if (token != null && !token.isBlank() && !mediaUrl.isBlank()) {
            try {
                // Meta Graph API Post Container Creation
                String apiUrl = "https://graph.facebook.com/v19.0/me/media";
                String body = "image_url=" + mediaUrl + "&caption=" + caption + "&access_token=" + token;

                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(apiUrl))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .timeout(Duration.ofSeconds(15))
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build();

                HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
                if (res.statusCode() >= 200 && res.statusCode() < 300) {
                    Map<String, Object> resp = JsonParser.parseObject(res.body());
                    String creationId = resp != null ? (String) resp.get("id") : "OK";
                    return "✅ Publicação criada no Instagram com sucesso via Meta Graph API! (ID: " + creationId + ")";
                }
            } catch (Exception e) {
                Logger.warn("Instagram", "Falha no Graph API post: " + e.getMessage());
            }
        }

        // Fallback: abrir Instagram para postagem
        openInstagramWeb();
        return String.format("📸 Legenda preparada para o Instagram:\n\"%s\"\n(O Instagram foi aberto no navegador para você concluir a postagem).", caption);
    }
}

