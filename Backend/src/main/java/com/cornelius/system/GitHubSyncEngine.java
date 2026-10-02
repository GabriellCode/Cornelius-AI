package com.cornelius.system;

import com.cornelius.config.CorneliusConfig;
import com.cornelius.util.JsonParser;
import com.cornelius.util.Logger;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Motor 100% nativo em Java 21 LTS para sincronização e publicação contínua com o GitHub.
 * Utiliza a API REST Git Database (Blobs, Trees, Commits, Refs) com zero dependências externas.
 */
public class GitHubSyncEngine {

    public record SyncResult(
            boolean success,
            String message,
            String commitSha,
            int filesUploaded,
            int totalFiles,
            long elapsedMs
    ) {}

    private static final String DEFAULT_REPO = "GabriellCode/Cornelius-AI";
    private static final String USER_AGENT = "Cornelius-AI-Java21-Deployer";
    private static final File LOG_FILE = new File("E:\\APP\\github_sync.log");

    // Padrões de arquivos e diretórios que NUNCA devem ser enviados ao repositório
    private static final List<Pattern> IGNORE_PATTERNS = List.of(
            Pattern.compile(".*\\.class$", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*[\\\\/]Backend[\\\\/]bin[\\\\/]?.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*[\\\\/]bin[\\\\/]?.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*\\.jar$", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*\\.war$", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*\\.ear$", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*\\.log$", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*\\.pid$", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*discord_bot\\.log$", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*out\\.log$", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*error\\.log$", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*github_sync\\.log$", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*sources\\.txt$", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*\\.tmp$", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*\\.bak$", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*[\\\\/]dist[\\\\/]?.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*[\\\\/]dist-windows-portable[\\\\/]?.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*[\\\\/]Cornelius-Windows-Portable[\\\\/]?.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*\\.zip$", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*\\.msi$", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*\\.exe$", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*[\\\\/]Cornelius_Vault[\\\\/]?.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*\\.czip$", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*[\\\\/]Android[\\\\/]\\.gradle[\\\\/]?.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*[\\\\/]Android[\\\\/]build[\\\\/]?.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*[\\\\/]Android[\\\\/]app[\\\\/]build[\\\\/]?.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*[\\\\/]Android[\\\\/]\\.idea[\\\\/]?.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*local\\.properties$", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*\\.apk$", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*\\.aab$", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*[\\\\/]\\.gradle[\\\\/]?.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*[\\\\/]build[\\\\/]?.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*[\\\\/]\\.idea[\\\\/]?.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*\\.iml$", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*[\\\\/]\\.vscode[\\\\/]?.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*test_script\\.js$", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*[\\\\/]\\.git[\\\\/]?.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*\\.env$", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*cornelius\\.properties$", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*scratch.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*\\.cornelius_discord\\.pid$", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*github\\.token$", Pattern.CASE_INSENSITIVE)
    );

    public static SyncResult sync(String commitMessage) {
        CorneliusConfig config = CorneliusConfig.load();
        return sync(config, commitMessage);
    }

    public static SyncResult sync(CorneliusConfig config, String commitMessage) {
        long startTime = System.currentTimeMillis();
        log("=================================================");
        log("Iniciando sincronização automática com GitHub (100% Java 21 Nativo)...");

        String token = resolveToken(config);
        if (token == null || token.isBlank()) {
            String err = "Token do GitHub não configurado no config.json nem no ambiente.";
            log("[ERRO] " + err);
            return new SyncResult(false, err, null, 0, 0, System.currentTimeMillis() - startTime);
        }

        String repo = (config != null && config.getGithubRepo() != null && !config.getGithubRepo().isBlank())
                ? config.getGithubRepo()
                : DEFAULT_REPO;

        File rootDir = resolveProjectRoot();
        log("Diretório base do projeto: " + rootDir.getAbsolutePath());

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        try {
            // 1. Validar autenticação no GitHub
            String userUrl = "https://api.github.com/user";
            HttpRequest userReq = buildGetRequest(userUrl, token);
            HttpResponse<String> userResp = client.send(userReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (userResp.statusCode() != 200) {
                String err = "Falha ao autenticar no GitHub (HTTP " + userResp.statusCode() + "): " + userResp.body();
                log("[ERRO] " + err);
                return new SyncResult(false, err, null, 0, 0, System.currentTimeMillis() - startTime);
            }
            Map<String, Object> userObj = JsonParser.parseObject(userResp.body());
            String login = (String) userObj.getOrDefault("login", "unknown");
            log("Autenticado com sucesso como: " + login + " | Repositório: " + repo);

            // 2. Obter estado atual da branch main e mapa de blobs remotos
            String parentCommitSha = null;
            Map<String, String> remoteBlobs = new HashMap<>();

            String refUrl = "https://api.github.com/repos/" + repo + "/git/refs/heads/main";
            HttpRequest refReq = buildGetRequest(refUrl, token);
            HttpResponse<String> refResp = client.send(refReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (refResp.statusCode() == 200) {
                Map<String, Object> refObj = JsonParser.parseObject(refResp.body());
                Object objField = refObj.get("object");
                if (objField instanceof Map<?, ?> objectMap) {
                    parentCommitSha = (String) objectMap.get("sha");
                }

                if (parentCommitSha != null) {
                    String commitUrl = "https://api.github.com/repos/" + repo + "/git/commits/" + parentCommitSha;
                    HttpResponse<String> commitResp = client.send(buildGetRequest(commitUrl, token), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                    if (commitResp.statusCode() == 200) {
                        Map<String, Object> commitObj = JsonParser.parseObject(commitResp.body());
                        Object treeField = commitObj.get("tree");
                        String treeSha = (treeField instanceof Map<?, ?> treeMap) ? (String) treeMap.get("sha") : null;

                        if (treeSha != null) {
                            String treeUrl = "https://api.github.com/repos/" + repo + "/git/trees/" + treeSha + "?recursive=1";
                            HttpResponse<String> treeResp = client.send(buildGetRequest(treeUrl, token), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                            if (treeResp.statusCode() == 200) {
                                Map<String, Object> fullTreeObj = JsonParser.parseObject(treeResp.body());
                                Object treeList = fullTreeObj.get("tree");
                                if (treeList instanceof List<?> items) {
                                    for (Object it : items) {
                                        if (it instanceof Map<?, ?> m) {
                                            if ("blob".equals(m.get("type"))) {
                                                String path = (String) m.get("path");
                                                String sha = (String) m.get("sha");
                                                if (path != null && sha != null) {
                                                    remoteBlobs.put(path, sha);
                                                }
                                            }
                                        }
                                    }
                                }
                                log("Árvore remota carregada (" + remoteBlobs.size() + " arquivos existentes no GitHub).");
                            }
                        }
                    }
                }
            } else {
                log("Aviso: Histórico prévio da branch main não encontrado (HTTP " + refResp.statusCode() + "). Criando nova base.");
            }

            // 3. Varrer arquivos locais válidos
            List<File> localFiles = scanLocalFiles(rootDir);
            log("Arquivos locais válidos para sincronização: " + localFiles.size());

            // 4. Calcular SHA dos arquivos locais e fazer upload apenas dos alterados/novos
            List<Map<String, Object>> treeEntries = new ArrayList<>();
            int uploadedCount = 0;
            int reusedCount = 0;

            for (File file : localFiles) {
                String fullPath = file.getAbsolutePath();
                String relPath = fullPath.substring(rootDir.getAbsolutePath().length());
                if (relPath.startsWith(File.separator)) {
                    relPath = relPath.substring(1);
                }
                relPath = relPath.replace('\\', '/');

                byte[] fileBytes = Files.readAllBytes(file.toPath());
                String localSha = computeGitBlobSha(fileBytes);

                String blobSha;
                if (remoteBlobs.containsKey(relPath) && localSha.equalsIgnoreCase(remoteBlobs.get(relPath))) {
                    blobSha = remoteBlobs.get(relPath);
                    reusedCount++;
                } else {
                    // Upload do blob usando base64 (seguro contra encoding e binários)
                    String base64Content = Base64.getEncoder().encodeToString(fileBytes);
                    Map<String, Object> blobPayload = Map.of(
                            "content", base64Content,
                            "encoding", "base64"
                    );

                    String blobUrl = "https://api.github.com/repos/" + repo + "/git/blobs";
                    HttpRequest blobReq = buildPostRequest(blobUrl, token, JsonParser.toJson(blobPayload));
                    HttpResponse<String> blobResp = client.send(blobReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

                    if (blobResp.statusCode() != 201) {
                        log("[ERRO] Falha ao enviar blob para " + relPath + " (HTTP " + blobResp.statusCode() + "): " + blobResp.body());
                        continue;
                    }

                    Map<String, Object> blobResObj = JsonParser.parseObject(blobResp.body());
                    blobSha = (String) blobResObj.get("sha");
                    uploadedCount++;
                    log("-> Enviado (" + uploadedCount + "): " + relPath);
                }

                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("path", relPath);
                entry.put("mode", "100644");
                entry.put("type", "blob");
                entry.put("sha", blobSha);
                treeEntries.add(entry);
            }

            log("Blobs processados: " + uploadedCount + " novos/modificados enviados, " + reusedCount + " mantidos idênticos.");

            if (uploadedCount == 0 && parentCommitSha != null) {
                log("Nenhum arquivo modificado detectado. Repositório já está 100% atualizado!");
                return new SyncResult(true, "Repositório já está em sincronia total.", parentCommitSha, 0, localFiles.size(), System.currentTimeMillis() - startTime);
            }

            // 5. Criar nova árvore Git
            log("Criando árvore Git no GitHub...");
            Map<String, Object> treePayload = new LinkedHashMap<>();
            treePayload.put("tree", treeEntries);

            String createTreeUrl = "https://api.github.com/repos/" + repo + "/git/trees";
            HttpRequest createTreeReq = buildPostRequest(createTreeUrl, token, JsonParser.toJson(treePayload));
            HttpResponse<String> createTreeResp = client.send(createTreeReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (createTreeResp.statusCode() != 201) {
                String err = "Erro ao criar árvore Git (HTTP " + createTreeResp.statusCode() + "): " + createTreeResp.body();
                log("[ERRO] " + err);
                return new SyncResult(false, err, null, uploadedCount, localFiles.size(), System.currentTimeMillis() - startTime);
            }

            Map<String, Object> createdTreeObj = JsonParser.parseObject(createTreeResp.body());
            String newTreeSha = (String) createdTreeObj.get("sha");

            // 6. Criar Commit
            String msg = (commitMessage != null && !commitMessage.isBlank())
                    ? commitMessage
                    : "auto-sync via Cornelius AI (100% Java 21 nativo)";

            Map<String, Object> commitPayload = new LinkedHashMap<>();
            commitPayload.put("message", msg);
            commitPayload.put("tree", newTreeSha);
            if (parentCommitSha != null) {
                commitPayload.put("parents", List.of(parentCommitSha));
            } else {
                commitPayload.put("parents", Collections.emptyList());
            }

            String createCommitUrl = "https://api.github.com/repos/" + repo + "/git/commits";
            HttpRequest createCommitReq = buildPostRequest(createCommitUrl, token, JsonParser.toJson(commitPayload));
            HttpResponse<String> createCommitResp = client.send(createCommitReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (createCommitResp.statusCode() != 201) {
                String err = "Erro ao criar commit (HTTP " + createCommitResp.statusCode() + "): " + createCommitResp.body();
                log("[ERRO] " + err);
                return new SyncResult(false, err, null, uploadedCount, localFiles.size(), System.currentTimeMillis() - startTime);
            }

            Map<String, Object> createdCommitObj = JsonParser.parseObject(createCommitResp.body());
            String newCommitSha = (String) createdCommitObj.get("sha");
            log("Commit criado com sucesso: " + newCommitSha + " ('" + msg + "')");

            // 7. Atualizar referência da branch main
            boolean branchUpdated = false;
            if (parentCommitSha != null) {
                Map<String, Object> updateRefPayload = Map.of(
                        "sha", newCommitSha,
                        "force", false
                );
                HttpRequest updateRefReq = HttpRequest.newBuilder()
                        .uri(URI.create(refUrl))
                        .header("Authorization", "Bearer " + token)
                        .header("Accept", "application/vnd.github.v3+json")
                        .header("User-Agent", USER_AGENT)
                        .header("Content-Type", "application/json")
                        .method("PATCH", HttpRequest.BodyPublishers.ofString(JsonParser.toJson(updateRefPayload), StandardCharsets.UTF_8))
                        .build();

                HttpResponse<String> updateRefResp = client.send(updateRefReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                if (updateRefResp.statusCode() == 200) {
                    branchUpdated = true;
                } else {
                    log("[ERRO] Falha ao atualizar branch main (HTTP " + updateRefResp.statusCode() + "): " + updateRefResp.body());
                }
            } else {
                Map<String, Object> createRefPayload = Map.of(
                        "ref", "refs/heads/main",
                        "sha", newCommitSha
                );
                String createRefUrl = "https://api.github.com/repos/" + repo + "/git/refs";
                HttpRequest createRefReq = buildPostRequest(createRefUrl, token, JsonParser.toJson(createRefPayload));
                HttpResponse<String> createRefResp = client.send(createRefReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                if (createRefResp.statusCode() == 201) {
                    branchUpdated = true;
                } else {
                    log("[ERRO] Falha ao criar branch main (HTTP " + createRefResp.statusCode() + "): " + createRefResp.body());
                }
            }

            if (branchUpdated) {
                log("Branch 'main' atualizada com sucesso para " + newCommitSha + "!");
                log("=================================================");
                log("SINCRONIZACAO COM GITHUB CONCLUIDA COM SUCESSO (100% JAVA 21)!");
                log("Link: https://github.com/" + repo);
                log("=================================================");
                return new SyncResult(true, "Sincronizado com sucesso: " + newCommitSha, newCommitSha, uploadedCount, localFiles.size(), System.currentTimeMillis() - startTime);
            } else {
                return new SyncResult(false, "Falha ao apontar branch main para novo commit.", newCommitSha, uploadedCount, localFiles.size(), System.currentTimeMillis() - startTime);
            }

        } catch (Exception e) {
            String err = "Erro inesperado na sincronização GitHub em Java: " + e.getMessage();
            log("[ERRO] " + err);
            Logger.error("GitHubSync", err);
            return new SyncResult(false, err, null, 0, 0, System.currentTimeMillis() - startTime);
        }
    }

    /**
     * Calcula o hash SHA-1 Git padrão de um arquivo: "blob <tamanho>\0<conteúdo>"
     */
    public static String computeGitBlobSha(byte[] bytes) {
        try {
            byte[] header = ("blob " + bytes.length + "\0").getBytes(StandardCharsets.US_ASCII);
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            md.update(header);
            md.update(bytes);
            byte[] digest = md.digest();
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new RuntimeException("Falha ao calcular hash Git SHA-1", e);
        }
    }

    private static List<File> scanLocalFiles(File rootDir) {
        List<File> result = new ArrayList<>();
        collectFilesRecursive(rootDir, result);
        result.sort(Comparator.comparing(File::getAbsolutePath));
        return result;
    }

    private static void collectFilesRecursive(File dir, List<File> result) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (isIgnored(f)) continue;
            if (f.isDirectory()) {
                collectFilesRecursive(f, result);
            } else if (f.isFile()) {
                result.add(f);
            }
        }
    }

    private static boolean isIgnored(File f) {
        String path = f.getAbsolutePath();
        for (Pattern p : IGNORE_PATTERNS) {
            if (p.matcher(path).matches()) {
                return true;
            }
        }
        return false;
    }

    private static HttpRequest buildGetRequest(String url, String token) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", USER_AGENT)
                .GET()
                .build();
    }

    private static HttpRequest buildPostRequest(String url, String token, String jsonBody) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", USER_AGENT)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();
    }

    private static File resolveProjectRoot() {
        File eApp = new File("E:\\APP");
        if (eApp.exists() && eApp.isDirectory()) {
            return eApp;
        }
        return new File(".").getAbsoluteFile();
    }

    private static String resolveToken(CorneliusConfig config) {
        if (config != null && config.getGithubToken() != null && !config.getGithubToken().isBlank()) {
            return config.getGithubToken();
        }
        String env = System.getenv("GITHUB_TOKEN");
        if (env != null && !env.isBlank()) {
            return env.trim();
        }
        File f = new File(System.getProperty("user.home"), ".cornelius" + File.separator + "github.token");
        if (f.exists()) {
            try {
                return Files.readString(f.toPath()).trim();
            } catch (Exception ignored) {}
        }
        return null;
    }

    private static void log(String msg) {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String line = "[" + time + "] " + msg;
        System.out.println(line);
        Logger.info("GitHubSync", msg);
        try {
            Files.writeString(LOG_FILE.toPath(), line + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.APPEND);
        } catch (Exception ignored) {}
    }

    /**
     * Ponto de entrada para execução direta via linha de comando ou scripts Java:
     * java -cp cornelius.jar com.cornelius.system.GitHubSyncEngine "mensagem do commit"
     */
    public static void main(String[] args) {
        String msg = args.length > 0 ? String.join(" ", args) : "Manual sync via Cornelius Java Engine";
        SyncResult res = sync(msg);
        System.out.println("Resultado: " + (res.success() ? "SUCESSO" : "FALHA") + " - " + res.message());
        System.exit(res.success() ? 0 : 1);
    }
}
