package com.cornelius.config;

import com.cornelius.util.JsonParser;
import com.cornelius.util.Logger;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

public class CorneliusConfig {
    private static final String CONFIG_DIR = Paths.get(System.getProperty("user.home"), ".cornelius").toString();
    private static final String CONFIG_FILE = Paths.get(CONFIG_DIR, "config.json").toString();

    private String geminiApiKey = "";
    private String geminiModel = "gemini-3.5-flash";
    private String ollamaUrl = "http://localhost:11434";
    private String ollamaModel = "llama3.2";
    private String activeProvider = "OLLAMA"; // "GEMINI", "OLLAMA", "AUTO"
    private boolean useLocalModel = true; // true = prefer Ollama, false = Gemini
    private boolean discordEnabled = false; // disables Discord bot when false
    private String externalDrivePath = "";
    private int compressionLevel = 9;
    private boolean autoArchiveChat = true;
    private int serverPort = 8080;
    private String userName = "Senhor";
    private String personaTone = "FORMAL"; // "FORMAL", "WITTY", "TECHNICAL"
    private String discordToken = "";

    // Instagram Integration
    private String instagramUsername = "";
    private String instagramAccessToken = "";
    private String instagramSessionId = "";
    private boolean instagramEnabled = false;

    // GitHub Integration
    private String githubToken = "";
    private String githubRepo = "GabriellCode/Cornelius-AI";

    public CorneliusConfig() {
        // Look for environment variables as defaults
        String envKey = System.getenv("GEMINI_API_KEY");
        if (envKey != null && !envKey.isBlank()) {
            this.geminiApiKey = envKey.trim();
        }
        detectDefaultDrivePath();
    }

    public static CorneliusConfig load() {
        CorneliusConfig config = new CorneliusConfig();
        File file = new File(CONFIG_FILE);
        if (!file.exists()) {
            config.save();
            return config;
        }

        try {
            String content = Files.readString(file.toPath());
            Map<String, Object> map = JsonParser.parseObject(content);
            if (map != null) {
                if (map.containsKey("geminiApiKey") && map.get("geminiApiKey") != null && !((String) map.get("geminiApiKey")).isBlank()) {
                    config.setGeminiApiKey((String) map.get("geminiApiKey"));
                }
                if (map.containsKey("geminiModel") && map.get("geminiModel") != null) {
                    String model = (String) map.get("geminiModel");
                    if (model.contains("2.5") || model.contains("1.5")) {
                        model = "gemini-3.5-flash";
                    }
                    config.setGeminiModel(model);
                }
                if (map.containsKey("ollamaUrl")) config.setOllamaUrl((String) map.get("ollamaUrl"));
                if (map.containsKey("ollamaModel")) config.setOllamaModel((String) map.get("ollamaModel"));
                if (map.containsKey("activeProvider")) config.setActiveProvider((String) map.get("activeProvider"));
                if (map.containsKey("externalDrivePath")) config.setExternalDrivePath((String) map.get("externalDrivePath"));
                if (map.containsKey("compressionLevel") && map.get("compressionLevel") instanceof Number n) {
                    config.setCompressionLevel(n.intValue());
                }
                if (map.containsKey("autoArchiveChat") && map.get("autoArchiveChat") instanceof Boolean b) {
                    config.setAutoArchiveChat(b);
                }
                if (map.containsKey("serverPort") && map.get("serverPort") instanceof Number n) {
                    config.setServerPort(n.intValue());
                }
                if (map.containsKey("userName")) config.setUserName((String) map.get("userName"));
                if (map.containsKey("personaTone")) config.setPersonaTone((String) map.get("personaTone"));
                if (map.containsKey("discordToken")) config.setDiscordToken((String) map.get("discordToken"));
                if (map.containsKey("instagramUsername")) config.setInstagramUsername((String) map.get("instagramUsername"));
                if (map.containsKey("instagramAccessToken")) config.setInstagramAccessToken((String) map.get("instagramAccessToken"));
                if (map.containsKey("instagramSessionId")) config.setInstagramSessionId((String) map.get("instagramSessionId"));
                if (map.containsKey("instagramEnabled") && map.get("instagramEnabled") instanceof Boolean b) {
                    config.setInstagramEnabled(b);
                }
                if (map.containsKey("githubToken")) config.setGithubToken((String) map.get("githubToken"));
                if (map.containsKey("githubRepo")) config.setGithubRepo((String) map.get("githubRepo"));
            }
            Logger.info("Config", "Configurações carregadas com sucesso de: " + CONFIG_FILE);
        } catch (Exception e) {
            Logger.warn("Config", "Falha ao ler arquivo de configuração. Usando padrões. " + e.getMessage());
        }

        if (config.getExternalDrivePath() == null || config.getExternalDrivePath().isBlank()) {
            config.detectDefaultDrivePath();
        }

        return config;
    }

    public void save() {
        try {
            Path dir = Paths.get(CONFIG_DIR);
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("geminiApiKey", geminiApiKey);
            map.put("geminiModel", geminiModel);
            map.put("ollamaUrl", ollamaUrl);
            map.put("ollamaModel", ollamaModel);
            map.put("activeProvider", activeProvider);
            map.put("externalDrivePath", externalDrivePath);
            map.put("compressionLevel", compressionLevel);
            map.put("autoArchiveChat", autoArchiveChat);
            map.put("serverPort", serverPort);
            map.put("userName", userName);
            map.put("personaTone", personaTone);
            map.put("discordToken", discordToken);
            map.put("instagramUsername", instagramUsername);
            map.put("instagramAccessToken", instagramAccessToken);
            map.put("instagramSessionId", instagramSessionId);
            map.put("instagramEnabled", instagramEnabled);
            map.put("githubToken", githubToken);
            map.put("githubRepo", githubRepo);

            String json = JsonParser.toPrettyJson(map);
            Files.writeString(Paths.get(CONFIG_FILE), json);
            Logger.success("Config", "Configurações salvas em: " + CONFIG_FILE);
        } catch (IOException e) {
            Logger.error("Config", "Erro ao salvar configurações: " + e.getMessage());
        }
    }

    public void detectDefaultDrivePath() {
        if (com.cornelius.system.SystemProfile.isWindows()) {
            File[] roots = File.listRoots();
            if (roots != null) {
                for (File root : roots) {
                    String path = root.getAbsolutePath();
                    if (!path.toLowerCase().startsWith("c:")) {
                        this.externalDrivePath = Paths.get(path, "Cornelius_Vault").toString();
                        return;
                    }
                }
            }
            this.externalDrivePath = Paths.get(CONFIG_DIR, "external_vault").toString();
            return;
        }

        String user = System.getProperty("user.name");
        File mediaDir = new File("/media/" + user);
        if (mediaDir.exists() && mediaDir.isDirectory()) {
            File[] mounts = mediaDir.listFiles(File::isDirectory);
            if (mounts != null && mounts.length > 0) {
                this.externalDrivePath = Paths.get(mounts[0].getAbsolutePath(), "Cornelius_Vault").toString();
                return;
            }
        }
        File mntDir = new File("/mnt");
        if (mntDir.exists() && mntDir.isDirectory()) {
            File[] mounts = mntDir.listFiles(File::isDirectory);
            if (mounts != null && mounts.length > 0) {
                this.externalDrivePath = Paths.get(mounts[0].getAbsolutePath(), "Cornelius_Vault").toString();
                return;
            }
        }
        // Fallback local vault path
        this.externalDrivePath = Paths.get(CONFIG_DIR, "external_vault").toString();
    }

    // Getters and Setters
    public String getGeminiApiKey() { return geminiApiKey; }
    public void setGeminiApiKey(String geminiApiKey) { this.geminiApiKey = geminiApiKey; }

    public String getGeminiModel() { return geminiModel; }
    public void setGeminiModel(String geminiModel) { this.geminiModel = geminiModel; }

    public String getOllamaUrl() { return ollamaUrl; }
    public void setOllamaUrl(String ollamaUrl) { this.ollamaUrl = ollamaUrl; }

    public String getOllamaModel() { return ollamaModel; }
    public void setOllamaModel(String ollamaModel) { this.ollamaModel = ollamaModel; }

    public String getActiveProvider() { return activeProvider; }
    public void setActiveProvider(String activeProvider) { this.activeProvider = activeProvider; }

    public String getExternalDrivePath() { return externalDrivePath; }
    public void setExternalDrivePath(String externalDrivePath) { this.externalDrivePath = externalDrivePath; }

    public int getCompressionLevel() { return compressionLevel; }
    public void setCompressionLevel(int compressionLevel) { this.compressionLevel = Math.clamp(compressionLevel, 1, 9); }

    public boolean isAutoArchiveChat() { return autoArchiveChat; }
    public void setAutoArchiveChat(boolean autoArchiveChat) { this.autoArchiveChat = autoArchiveChat; }

    public boolean isUseLocalModel() { return useLocalModel; }
    public void setUseLocalModel(boolean useLocalModel) { this.useLocalModel = useLocalModel; }

    public boolean isDiscordEnabled() { return discordEnabled; }
    public void setDiscordEnabled(boolean discordEnabled) { this.discordEnabled = discordEnabled; }

    public boolean isInstagramEnabled() { return instagramEnabled; }
    public void setInstagramEnabled(boolean instagramEnabled) { this.instagramEnabled = instagramEnabled; }
    public void setServerPort(int serverPort) { this.serverPort = serverPort; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getPersonaTone() { return personaTone; }
    public void setPersonaTone(String personaTone) { this.personaTone = personaTone; }

    public String getDiscordToken() { return discordToken; }
    public void setDiscordToken(String discordToken) { this.discordToken = discordToken; }

    public String getInstagramUsername() { return instagramUsername; }
    public void setInstagramUsername(String instagramUsername) { this.instagramUsername = instagramUsername; }

    public String getInstagramAccessToken() { return instagramAccessToken; }
    public void setInstagramAccessToken(String instagramAccessToken) { this.instagramAccessToken = instagramAccessToken; }

    public String getInstagramSessionId() { return instagramSessionId; }
    public void setInstagramSessionId(String instagramSessionId) { this.instagramSessionId = instagramSessionId; }

    public int getServerPort() { return serverPort; }
    public String getGithubToken() { return githubToken; }
    public void setGithubToken(String githubToken) { this.githubToken = githubToken; }
    public String getGithubRepo() { return githubRepo; }
    public void setGithubRepo(String githubRepo) { this.githubRepo = githubRepo; }
}

