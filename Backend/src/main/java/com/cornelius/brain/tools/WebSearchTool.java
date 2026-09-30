package com.cornelius.brain.tools;

import com.cornelius.util.Logger;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebSearchTool implements AgentTool {
    private final HttpClient httpClient;

    public WebSearchTool() {
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public String getName() {
        return "WebSearch";
    }

    @Override
    public String getDescription() {
        return "Pesquisa informações em tempo real na internet ou lê páginas web.";
    }

    @Override
    public String execute(String query) throws Exception {
        if (query == null || query.isBlank()) {
            return "Nenhum termo de pesquisa fornecido.";
        }

        query = query.trim();

        // If direct URL
        if (query.startsWith("http://") || query.startsWith("https://")) {
            return fetchPageContent(query);
        }

        return performDuckDuckGoSearch(query);
    }

    private String performDuckDuckGoSearch(String query) {
        try {
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String url = "https://html.duckduckgo.com/html/?q=" + encodedQuery;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                    .header("Accept-Language", "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7")
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                return "Falha na conexão com a internet (Código HTTP " + response.statusCode() + ").";
            }

            return parseDuckDuckGoHtml(response.body(), query);
        } catch (Exception e) {
            Logger.warn("WebSearch", "Erro ao executar pesquisa na web: " + e.getMessage());
            return "Não foi possível acessar a internet no momento: " + e.getMessage();
        }
    }

    private String parseDuckDuckGoHtml(String html, String query) {
        StringBuilder sb = new StringBuilder("### 🌐 Resultados da Pesquisa Web para: \"").append(query).append("\"\n\n");

        Pattern resultPattern = Pattern.compile("<a class=\"result__snippet[^\"]*\"[^>]*>(.*?)</a>", Pattern.DOTALL);
        Pattern titlePattern = Pattern.compile("<a class=\"result__url[^\"]*\"[^>]*>(.*?)</a>", Pattern.DOTALL);

        Matcher snippetMatcher = resultPattern.matcher(html);
        int count = 0;

        while (snippetMatcher.find() && count < 5) {
            String snippet = cleanHtml(snippetMatcher.group(1));
            if (!snippet.isBlank()) {
                count++;
                sb.append(count).append(". ").append(snippet).append("\n\n");
            }
        }

        if (count == 0) {
            // Fallback generic extraction
            Pattern genericPattern = Pattern.compile("<td[^>]*class=\"result-snippet\"[^>]*>(.*?)</td>", Pattern.DOTALL);
            Matcher gm = genericPattern.matcher(html);
            while (gm.find() && count < 5) {
                String snippet = cleanHtml(gm.group(1));
                if (!snippet.isBlank()) {
                    count++;
                    sb.append(count).append(". ").append(snippet).append("\n\n");
                }
            }
        }

        if (count == 0) {
            return "Nenhum resultado direto extraído para a busca \"" + query + "\".";
        }

        return sb.toString();
    }

    public String fetchPageContent(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) CorneliusButler/1.0")
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String body = response.body();
            String clean = cleanHtml(body);
            if (clean.length() > 4000) {
                clean = clean.substring(0, 4000) + "\n... [conteúdo truncado para brevidade]";
            }
            return "### 📄 Conteúdo extraído da URL: " + url + "\n\n" + clean;
        } catch (Exception e) {
            return "Erro ao ler a página " + url + ": " + e.getMessage();
        }
    }

    private String cleanHtml(String html) {
        if (html == null) return "";
        return html.replaceAll("<style[^>]*>.*?</style>", "")
                .replaceAll("<script[^>]*>.*?</script>", "")
                .replaceAll("<[^>]+>", " ")
                .replaceAll("&nbsp;", " ")
                .replaceAll("&quot;", "\"")
                .replaceAll("&amp;", "&")
                .replaceAll("&lt;", "<")
                .replaceAll("&gt;", ">")
                .replaceAll("&#39;", "'")
                .replaceAll("\\s+", " ")
                .trim();
    }
}

