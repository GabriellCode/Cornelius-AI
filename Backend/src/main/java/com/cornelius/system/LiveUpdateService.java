package com.cornelius.system;

import com.cornelius.brain.CorneliusBrain;
import com.cornelius.config.CorneliusConfig;
import com.cornelius.util.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class LiveUpdateService {
    private final CorneliusBrain brain;
    private final CorneliusConfig config;
    private final List<Consumer<UpdateEvent>> listeners = new CopyOnWriteArrayList<>();
    private WatchService watchService;
    private final Map<WatchKey, Path> keyPathMap = new ConcurrentHashMap<>();
    private final ScheduledExecutorService debounceExecutor = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> pendingNotification = null;
    private volatile boolean isRunning = false;

    public record UpdateEvent(
            String filename,
            String changeType,
            long timestamp,
            boolean isSourceCode,
            boolean isConfig,
            boolean isDocument
    ) {}

    public LiveUpdateService(CorneliusBrain brain, CorneliusConfig config) {
        this.brain = brain;
        this.config = config;
    }

    public void addListener(Consumer<UpdateEvent> listener) {
        listeners.add(listener);
    }

    public void removeListener(Consumer<UpdateEvent> listener) {
        listeners.remove(listener);
    }

    public synchronized void start() {
        if (isRunning) return;

        try {
            this.watchService = FileSystems.getDefault().newWatchService();
            this.isRunning = true;

            // 1. Watch Project Source Code & Workspace
            Path projectDir = Paths.get("").toAbsolutePath();
            registerRecursive(projectDir.resolve("Backend/src"));
            registerPath(projectDir);

            // 2. Watch User Config directory (~/.cornelius)
            Path configDir = Paths.get(System.getProperty("user.home"), ".cornelius");
            if (Files.exists(configDir)) {
                registerPath(configDir);
            }

            // 3. Watch Vault Documents folder
            Path docsDir = brain.getDriveManager().getDocumentsDir();
            if (docsDir != null && Files.exists(docsDir)) {
                registerRecursive(docsDir);
            }

            // Start background monitoring thread
            Thread.ofVirtual().name("Cornelius-LiveUpdate-Watcher").start(this::watchLoop);
            Logger.success("LiveUpdate", "Sistema de Atualizações em Tempo Real ATIVADO. Monitorando código, cofre e configurações.");
        } catch (Exception e) {
            Logger.warn("LiveUpdate", "Não foi possível iniciar o WatchService: " + e.getMessage());
            this.isRunning = false;
        }
    }

    public synchronized void stop() {
        isRunning = false;
        if (watchService != null) {
            try {
                watchService.close();
            } catch (IOException ignored) {}
        }
        debounceExecutor.shutdownNow();
    }

    private void registerPath(Path path) {
        if (path == null || !Files.exists(path) || !Files.isDirectory(path)) return;
        try {
            WatchKey key = path.register(
                    watchService,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.ENTRY_DELETE
            );
            keyPathMap.put(key, path);
        } catch (Exception ignored) {}
    }

    private void registerRecursive(Path root) {
        if (root == null || !Files.exists(root)) return;
        try {
            Files.walkFileTree(root, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    // Skip git and build output directories
                    String name = dir.getFileName().toString();
                    if (name.equals(".git") || name.equals("bin") || name.equals("target") || name.equals("dist")) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    registerPath(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (Exception ignored) {}
    }

    private void watchLoop() {
        while (isRunning && watchService != null) {
            WatchKey key;
            try {
                key = watchService.take();
            } catch (InterruptedException | ClosedWatchServiceException e) {
                break;
            }

            Path parentDir = keyPathMap.get(key);
            if (parentDir == null) {
                key.reset();
                continue;
            }

            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();
                if (kind == StandardWatchEventKinds.OVERFLOW) continue;

                @SuppressWarnings("unchecked")
                WatchEvent<Path> ev = (WatchEvent<Path>) event;
                Path filename = ev.context();
                Path fullPath = parentDir.resolve(filename);

                String nameStr = filename.toString();
                // Ignore temporary swap/scratch files
                if (nameStr.startsWith(".") || nameStr.endsWith("~") || nameStr.endsWith(".swp") || nameStr.endsWith(".tmp")) {
                    continue;
                }

                // If new directory, register it
                if (kind == StandardWatchEventKinds.ENTRY_CREATE && Files.isDirectory(fullPath)) {
                    registerRecursive(fullPath);
                }

                boolean isSource = nameStr.endsWith(".java") || nameStr.endsWith(".html") || nameStr.endsWith(".css") || nameStr.endsWith(".js");
                boolean isConfig = nameStr.endsWith(".json") || nameStr.contains("config");
                boolean isDocument = nameStr.endsWith(".txt") || nameStr.endsWith(".pdf") || nameStr.endsWith(".md") || nameStr.endsWith(".docx");

                UpdateEvent updateEvent = new UpdateEvent(
                        nameStr,
                        kind.name().replace("ENTRY_", ""),
                        System.currentTimeMillis(),
                        isSource,
                        isConfig,
                        isDocument
                );

                debounceAndNotify(updateEvent);
            }

            boolean valid = key.reset();
            if (!valid) {
                keyPathMap.remove(key);
            }
        }
    }

    private synchronized void debounceAndNotify(UpdateEvent event) {
        if (pendingNotification != null && !pendingNotification.isDone()) {
            pendingNotification.cancel(false);
        }

        // Debounce 600ms to allow multi-file saves to batch together
        pendingNotification = debounceExecutor.schedule(() -> {
            Logger.info("LiveUpdate", "Alteração em tempo real detectada: " + event.filename() + " [" + event.changeType() + "]");

            // Auto reload config if config changed
            if (event.isConfig()) {
                CorneliusConfig newConfig = CorneliusConfig.load();
                config.setActiveProvider(newConfig.getActiveProvider());
                config.setGeminiModel(newConfig.getGeminiModel());
                config.setGeminiApiKey(newConfig.getGeminiApiKey());
                config.setOllamaUrl(newConfig.getOllamaUrl());
                config.setOllamaModel(newConfig.getOllamaModel());
                config.setPersonaTone(newConfig.getPersonaTone());
                config.setUserName(newConfig.getUserName());
                config.setDiscordToken(newConfig.getDiscordToken());
            }

            // Notify UI listeners
            for (Consumer<UpdateEvent> listener : listeners) {
                try {
                    listener.accept(event);
                } catch (Exception e) {
                    Logger.error("LiveUpdate", "Erro no listener de atualização: " + e.getMessage());
                }
            }
        }, 600, TimeUnit.MILLISECONDS);
    }
}

