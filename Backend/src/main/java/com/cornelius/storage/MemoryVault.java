package com.cornelius.storage;

import com.cornelius.config.CorneliusConfig;
import com.cornelius.util.JsonParser;
import com.cornelius.util.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class MemoryVault {
    private final ExternalDriveManager driveManager;
    private final CorneliusConfig config;
    private final List<MemoryEntry> memories = new ArrayList<>();
    private final Path memoryFile;

    public record MemoryEntry(
            String id,
            String category, // "FACT", "PREFERENCE", "TASK", "NOTE", "CONVERSATION_SUMMARY"
            String text,
            long createdAt,
            int priority
    ) {}

    public MemoryVault(ExternalDriveManager driveManager, CorneliusConfig config) {
        this.driveManager = driveManager;
        this.config = config;
        this.memoryFile = driveManager.getMemoryDir().resolve("memories.czip");
        loadMemories();
    }

    public synchronized MemoryEntry addMemory(String category, String text, int priority) {
        String id = UUID.randomUUID().toString().substring(0, 8);
        MemoryEntry entry = new MemoryEntry(id, category.toUpperCase(Locale.ROOT), text, System.currentTimeMillis(), priority);
        memories.add(entry);
        saveMemories();
        Logger.butler("Nova memória arquivada no cofre: [" + category + "] " + text);
        return entry;
    }

    public synchronized boolean deleteMemory(String id) {
        boolean removed = memories.removeIf(m -> m.id().equals(id));
        if (removed) {
            saveMemories();
        }
        return removed;
    }

    public synchronized String getFormattedMemoriesForPrompt() {
        if (memories.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder("=== MEMÓRIAS DE LONGO PRAZO DO CORNELIUS (COFRE HD) ===\n");
        for (MemoryEntry m : memories) {
            sb.append(String.format("• [%s] %s\n", m.category(), m.text()));
        }
        return sb.toString();
    }

    public synchronized List<MemoryEntry> getAllMemories() {
        return Collections.unmodifiableList(new ArrayList<>(memories));
    }

    public synchronized void saveMemories() {
        try {
            List<Map<String, Object>> list = new ArrayList<>();
            for (MemoryEntry m : memories) {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("id", m.id());
                map.put("category", m.category());
                map.put("text", m.text());
                map.put("createdAt", m.createdAt());
                map.put("priority", m.priority());
                list.add(map);
            }
            String json = JsonParser.toJson(list);
            Path temp = Files.createTempFile("cornelius_mem_", ".json");
            Files.writeString(temp, json);
            CompressionEngine.compressFileToArchive(temp, memoryFile, config.getCompressionLevel());
            temp.toFile().deleteOnExit();
        } catch (Exception e) {
            Logger.error("MemoryVault", "Erro ao salvar memórias comprimidas: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public synchronized void loadMemories() {
        if (!Files.exists(memoryFile)) {
            // Seed default butler initial memories
            addMemory("PREFERENCE", "O usuário utiliza o sistema operacional Pop!_OS Linux em seu notebook.", 5);
            addMemory("FACT", "O assistente se chama Cornelius e serve como mordomo pessoal e conselheiro técnico.", 5);
            addMemory("PREFERENCE", "Dados e memórias devem ser comprimidos no HD externo com prioridade máxima de economia de espaço.", 5);
            return;
        }

        try {
            byte[] decompressed = CompressionEngine.decompressArchive(memoryFile);
            String json = new String(decompressed);
            List<Object> raw = JsonParser.parseArray(json);
            memories.clear();
            for (Object obj : raw) {
                if (obj instanceof Map) {
                    Map<String, Object> m = (Map<String, Object>) obj;
                    String id = (String) m.get("id");
                    String category = (String) m.get("category");
                    String text = (String) m.get("text");
                    long createdAt = m.get("createdAt") instanceof Number n ? n.longValue() : 0L;
                    int priority = m.get("priority") instanceof Number n ? n.intValue() : 1;
                    memories.add(new MemoryEntry(id, category, text, createdAt, priority));
                }
            }
            Logger.info("MemoryVault", String.format("%d memórias carregadas do cofre do HD externo.", memories.size()));
        } catch (Exception e) {
            Logger.warn("MemoryVault", "Falha ao descompactar cofre de memórias: " + e.getMessage());
        }
    }
}

