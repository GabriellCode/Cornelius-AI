package com.cornelius.storage;

import com.cornelius.config.CorneliusConfig;
import com.cornelius.util.JsonParser;
import com.cornelius.util.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class KnowledgeBase {
    private final ExternalDriveManager driveManager;
    private final CorneliusConfig config;
    private final List<DocumentChunk> chunks = new ArrayList<>();
    private final Map<String, List<Integer>> invertedIndex = new ConcurrentHashMap<>();
    private final Path indexPath;

    private static final Pattern WORD_PATTERN = Pattern.compile("[\\p{L}\\p{N}]+");
    private static final Set<String> STOPWORDS = Set.of(
            "a", "o", "as", "os", "de", "do", "da", "dos", "das", "em", "no", "na", "nos", "nas",
            "um", "uma", "uns", "umas", "para", "por", "com", "sem", "sobre", "entre", "que", "e",
            "ou", "se", "como", "mas", "mais", "muito", "seu", "sua", "seus", "suas", "meu", "minha",
            "the", "is", "at", "which", "on", "in", "and", "or", "to", "of", "for", "with", "it", "this"
    );

    public record DocumentChunk(
            String id,
            String sourceName,
            String sourcePath,
            String content,
            List<String> tags,
            long timestamp,
            int wordCount
    ) {}

    public record SearchResult(
            DocumentChunk chunk,
            double score,
            String highlight
    ) {}

    public KnowledgeBase(ExternalDriveManager driveManager, CorneliusConfig config) {
        this.driveManager = driveManager;
        this.config = config;
        this.indexPath = driveManager.getDocumentsDir().resolve("knowledge_index.czip");
        loadIndex();
    }

    public synchronized void addDocument(String sourceName, String sourcePath, String fullText, List<String> tags) {
        List<String> textChunks = splitIntoChunks(fullText, 600, 100);
        long now = System.currentTimeMillis();

        for (int i = 0; i < textChunks.size(); i++) {
            String text = textChunks.get(i);
            String chunkId = UUID.randomUUID().toString();
            String title = sourceName + " [Parte " + (i + 1) + "/" + textChunks.size() + "]";
            int wordCount = tokenize(text).size();

            DocumentChunk chunk = new DocumentChunk(chunkId, title, sourcePath, text, tags, now, wordCount);
            int index = chunks.size();
            chunks.add(chunk);

            // Index terms
            Set<String> words = new HashSet<>(tokenize(text));
            for (String w : words) {
                invertedIndex.computeIfAbsent(w, k -> new ArrayList<>()).add(index);
            }
        }

        saveIndex();
        Logger.info("KnowledgeBase", String.format("Documento '%s' ingerido com sucesso (%d blocos).", sourceName, textChunks.size()));
    }

    public synchronized List<SearchResult> search(String query, int maxResults) {
        List<String> queryTerms = tokenize(query);
        if (queryTerms.isEmpty() || chunks.isEmpty()) {
            return Collections.emptyList();
        }

        int N = chunks.size();
        double avgdl = chunks.stream().mapToInt(DocumentChunk::wordCount).average().orElse(1.0);
        double k1 = 1.2;
        double b = 0.75;

        Map<Integer, Double> scores = new HashMap<>();

        for (String term : queryTerms) {
            List<Integer> postingList = invertedIndex.get(term);
            if (postingList == null || postingList.isEmpty()) continue;

            int n = postingList.size();
            double idf = Math.log(1.0 + (N - n + 0.5) / (n + 0.5));

            for (int docIdx : postingList) {
                DocumentChunk doc = chunks.get(docIdx);
                long termFreq = tokenize(doc.content()).stream().filter(term::equals).count();
                double tf = (double) termFreq;
                double docLen = doc.wordCount();

                double score = idf * ((tf * (k1 + 1.0)) / (tf + k1 * (1.0 - b + b * (docLen / avgdl))));
                scores.merge(docIdx, score, Double::sum);
            }
        }

        return scores.entrySet().stream()
                .sorted(Map.Entry.<Integer, Double>comparingByValue().reversed())
                .limit(maxResults)
                .map(entry -> {
                    DocumentChunk chunk = chunks.get(entry.getKey());
                    return new SearchResult(chunk, entry.getValue(), createHighlight(chunk.content(), queryTerms));
                })
                .collect(Collectors.toList());
    }

    public String buildContextForQuery(String query, int maxResults) {
        List<SearchResult> results = search(query, maxResults);
        if (results.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder("=== CONHECIMENTO RELEVANTE RECUPERADO DO HD EXTERNO ===\n\n");
        for (int i = 0; i < results.size(); i++) {
            SearchResult res = results.get(i);
            sb.append(String.format("[%d] Fonte: %s (Relevância: %.2f)\n", i + 1, res.chunk().sourceName(), res.score()));
            sb.append(res.chunk().content()).append("\n\n");
        }
        return sb.toString();
    }

    private List<String> splitIntoChunks(String text, int targetWords, int overlapWords) {
        String[] words = text.split("\\s+");
        List<String> list = new ArrayList<>();
        if (words.length <= targetWords) {
            list.add(text);
            return list;
        }

        int start = 0;
        while (start < words.length) {
            int end = Math.min(start + targetWords, words.length);
            StringBuilder sb = new StringBuilder();
            for (int i = start; i < end; i++) {
                sb.append(words[i]).append(" ");
            }
            list.add(sb.toString().trim());
            if (end >= words.length) break;
            start += (targetWords - overlapWords);
        }
        return list;
    }

    private List<String> tokenize(String text) {
        List<String> list = new ArrayList<>();
        var matcher = WORD_PATTERN.matcher(text.toLowerCase(Locale.ROOT));
        while (matcher.find()) {
            String word = matcher.group();
            if (word.length() > 1 && !STOPWORDS.contains(word)) {
                list.add(word);
            }
        }
        return list;
    }

    private String createHighlight(String content, List<String> terms) {
        String lower = content.toLowerCase(Locale.ROOT);
        int firstPos = -1;
        for (String t : terms) {
            int pos = lower.indexOf(t);
            if (pos != -1 && (firstPos == -1 || pos < firstPos)) {
                firstPos = pos;
            }
        }
        if (firstPos == -1) {
            return content.substring(0, Math.min(200, content.length())) + "...";
        }
        int start = Math.max(0, firstPos - 60);
        int end = Math.min(content.length(), firstPos + 140);
        String snippet = content.substring(start, end).replaceAll("\\s+", " ").trim();
        return (start > 0 ? "..." : "") + snippet + (end < content.length() ? "..." : "");
    }

    public synchronized void saveIndex() {
        try {
            List<Map<String, Object>> list = new ArrayList<>();
            for (DocumentChunk c : chunks) {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("id", c.id());
                map.put("sourceName", c.sourceName());
                map.put("sourcePath", c.sourcePath());
                map.put("content", c.content());
                map.put("tags", c.tags());
                map.put("timestamp", c.timestamp());
                map.put("wordCount", c.wordCount());
                list.add(map);
            }
            String json = JsonParser.toJson(list);
            CompressionEngine.compressString(json, config.getCompressionLevel());
            CompressionEngine.compressFileToArchive(
                    writeTempJson(json),
                    indexPath,
                    config.getCompressionLevel()
            );
        } catch (Exception e) {
            Logger.error("KnowledgeBase", "Erro ao salvar índice comprimido: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public synchronized void loadIndex() {
        if (!Files.exists(indexPath)) return;
        try {
            byte[] decompressed = CompressionEngine.decompressArchive(indexPath);
            String json = new String(decompressed);
            List<Object> rawList = JsonParser.parseArray(json);
            chunks.clear();
            invertedIndex.clear();

            for (Object obj : rawList) {
                if (obj instanceof Map) {
                    Map<String, Object> m = (Map<String, Object>) obj;
                    String id = (String) m.get("id");
                    String sourceName = (String) m.get("sourceName");
                    String sourcePath = (String) m.get("sourcePath");
                    String content = (String) m.get("content");
                    List<String> tags = (List<String>) m.get("tags");
                    long timestamp = m.get("timestamp") instanceof Number n ? n.longValue() : 0L;
                    int wordCount = m.get("wordCount") instanceof Number n ? n.intValue() : 0;

                    DocumentChunk chunk = new DocumentChunk(id, sourceName, sourcePath, content, tags, timestamp, wordCount);
                    int idx = chunks.size();
                    chunks.add(chunk);

                    Set<String> words = new HashSet<>(tokenize(content));
                    for (String w : words) {
                        invertedIndex.computeIfAbsent(w, k -> new ArrayList<>()).add(idx);
                    }
                }
            }
            Logger.info("KnowledgeBase", String.format("Índice carregado do HD: %d blocos de conhecimento ativos.", chunks.size()));
        } catch (Exception e) {
            Logger.warn("KnowledgeBase", "Falha ao carregar índice de conhecimento anterior: " + e.getMessage());
        }
    }

    private Path writeTempJson(String content) throws IOException {
        Path temp = Files.createTempFile("cornelius_kb_", ".json");
        Files.writeString(temp, content);
        temp.toFile().deleteOnExit();
        return temp;
    }

    public int getChunkCount() { return chunks.size(); }
    public List<DocumentChunk> getAllChunks() { return Collections.unmodifiableList(chunks); }
}

