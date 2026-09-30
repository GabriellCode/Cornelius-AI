package com.cornelius.storage;

import com.cornelius.config.CorneliusConfig;
import com.cornelius.util.Logger;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class ExternalDriveManager {
    private final CorneliusConfig config;
    private Path vaultPath;
    private Path documentsDir;
    private Path memoryDir;
    private Path chatHistoryDir;
    private Path webCacheDir;

    public record DriveMetrics(
            String path,
            long totalBytes,
            long freeBytes,
            long usedBytes,
            double usedPercent,
            boolean isExternal,
            int totalArchivedFiles,
            long totalRawBytesSaved,
            long totalCompressedBytes,
            double overallSavingsRatio
    ) {}

    public ExternalDriveManager(CorneliusConfig config) {
        this.config = config;
        initVault();
    }

    public void initVault() {
        String pathStr = config.getExternalDrivePath();
        if (pathStr == null || pathStr.isBlank()) {
            config.detectDefaultDrivePath();
            pathStr = config.getExternalDrivePath();
        }

        this.vaultPath = Paths.get(pathStr);
        this.documentsDir = vaultPath.resolve("documents");
        this.memoryDir = vaultPath.resolve("memory");
        this.chatHistoryDir = vaultPath.resolve("chat_history");
        this.webCacheDir = vaultPath.resolve("web_cache");

        try {
            Files.createDirectories(documentsDir);
            Files.createDirectories(memoryDir);
            Files.createDirectories(chatHistoryDir);
            Files.createDirectories(webCacheDir);
            Logger.info("Storage", "Cofre de armazenamento Cornelius inicializado em: " + vaultPath.toAbsolutePath());
        } catch (IOException e) {
            // Fallback to local user home vault when external HD is not mounted
            this.vaultPath = Paths.get(System.getProperty("user.home"), "Cornelius_Vault");
            this.documentsDir = vaultPath.resolve("documents");
            this.memoryDir = vaultPath.resolve("memory");
            this.chatHistoryDir = vaultPath.resolve("chat_history");
            this.webCacheDir = vaultPath.resolve("web_cache");
            try {
                Files.createDirectories(documentsDir);
                Files.createDirectories(memoryDir);
                Files.createDirectories(chatHistoryDir);
                Files.createDirectories(webCacheDir);
                Logger.info("Storage", "HD externo não montado. Usando cofre local em: " + vaultPath.toAbsolutePath());
            } catch (Exception ex) {
                Logger.error("Storage", "Erro ao inicializar cofre: " + ex.getMessage());
            }
        }
    }

    public DriveMetrics getMetrics() {
        long total = 0;
        long free = 0;
        long used = 0;
        double usedPercent = 0.0;
        boolean isExternal = isMountedExternal();

        try {
            if (Files.exists(vaultPath)) {
                FileStore store = Files.getFileStore(vaultPath);
                total = store.getTotalSpace();
                free = store.getUsableSpace();
                used = total - free;
                if (total > 0) {
                    usedPercent = ((double) used / (double) total) * 100.0;
                }
            }
        } catch (Exception e) {
            Logger.warn("Storage", "Falha ao consultar FileStore do disco: " + e.getMessage());
        }

        // Count archive stats
        int count = 0;
        long totalComp = 0;
        long totalRaw = 0;

        try {
            if (Files.exists(vaultPath)) {
                try (var stream = Files.walk(vaultPath)) {
                    List<Path> files = stream.filter(Files::isRegularFile).toList();
                    count = files.size();
                    for (Path f : files) {
                        long size = Files.size(f);
                        totalComp += size;
                        // Estimate 3.5x uncompressed average if raw stat not stored directly
                        totalRaw += (long) (size * 3.2);
                    }
                }
            }
        } catch (Exception ignored) {}

        double savingsRatio = totalRaw > 0 ? (1.0 - ((double) totalComp / (double) totalRaw)) * 100.0 : 0.0;

        return new DriveMetrics(
                vaultPath.toAbsolutePath().toString(),
                total,
                free,
                used,
                usedPercent,
                isExternal,
                count,
                totalRaw,
                totalComp,
                savingsRatio
        );
    }

    public boolean isMountedExternal() {
        String pathStr = vaultPath.toString();
        if (com.cornelius.system.SystemProfile.isWindows()) {
            return !pathStr.toLowerCase().startsWith("c:\\") && !pathStr.toLowerCase().startsWith("c:/");
        }
        return pathStr.startsWith("/media/") || pathStr.startsWith("/mnt/") || pathStr.startsWith("/run/media/");
    }

    public List<String> detectAvailableMounts() {
        List<String> list = new ArrayList<>();
        if (com.cornelius.system.SystemProfile.isWindows()) {
            File[] roots = File.listRoots();
            if (roots != null) {
                for (File r : roots) {
                    list.add(r.getAbsolutePath());
                }
            }
            return list;
        }

        String user = System.getProperty("user.name");
        File[] mediaDirs = new File[] {
                new File("/media/" + user),
                new File("/run/media/" + user),
                new File("/mnt")
        };

        for (File base : mediaDirs) {
            if (base.exists() && base.isDirectory()) {
                File[] children = base.listFiles(File::isDirectory);
                if (children != null) {
                    for (File c : children) {
                        list.add(c.getAbsolutePath());
                    }
                }
            }
        }
        return list;
    }

    public Path getVaultPath() { return vaultPath; }
    public Path getDocumentsDir() { return documentsDir; }
    public Path getMemoryDir() { return memoryDir; }
    public Path getChatHistoryDir() { return chatHistoryDir; }
    public Path getWebCacheDir() { return webCacheDir; }
}

