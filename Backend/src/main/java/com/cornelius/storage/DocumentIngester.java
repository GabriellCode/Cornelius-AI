package com.cornelius.storage;

import com.cornelius.config.CorneliusConfig;
import com.cornelius.util.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class DocumentIngester {
    private final KnowledgeBase knowledgeBase;
    private final ExternalDriveManager driveManager;
    private final CorneliusConfig config;

    private static final Set<String> TEXT_EXTENSIONS = Set.of(
            "txt", "md", "markdown", "json", "csv", "tsv", "log", "html", "htm", "xml",
            "java", "py", "sh", "bash", "c", "cpp", "h", "hpp", "sql", "yaml", "yml",
            "properties", "ini", "conf", "env", "rst", "org"
    );

    public record IngestionSummary(
            int filesProcessed,
            long totalOriginalBytes,
            long totalCompressedBytes,
            double savingsRatio,
            List<String> ingestedTitles
    ) {}

    public DocumentIngester(KnowledgeBase knowledgeBase, ExternalDriveManager driveManager, CorneliusConfig config) {
        this.knowledgeBase = knowledgeBase;
        this.driveManager = driveManager;
        this.config = config;
    }

    public IngestionSummary ingestPath(Path path) throws IOException {
        List<Path> targets = new ArrayList<>();
        if (Files.isDirectory(path)) {
            try (var stream = Files.walk(path)) {
                targets = stream.filter(Files::isRegularFile).toList();
            }
        } else if (Files.isRegularFile(path)) {
            targets.add(path);
        }

        int count = 0;
        long totalOrig = 0;
        long totalComp = 0;
        List<String> titles = new ArrayList<>();

        for (Path file : targets) {
            String name = file.getFileName().toString();
            String ext = getFileExtension(name);

            if (TEXT_EXTENSIONS.contains(ext.toLowerCase())) {
                try {
                    String content = Files.readString(file, StandardCharsets.UTF_8);
                    if (!content.isBlank()) {
                        // Archive original compressed
                        String archiveName = System.currentTimeMillis() + "_" + name + ".czip";
                        Path archivePath = driveManager.getDocumentsDir().resolve(archiveName);
                        CompressionEngine.CompressionResult res = CompressionEngine.compressFileToArchive(
                                file, archivePath, config.getCompressionLevel()
                        );

                        totalOrig += res.originalBytes();
                        totalComp += res.compressedBytes();

                        // Add to knowledge index
                        List<String> tags = List.of(ext, "hd_external", file.getParent().getFileName().toString());
                        knowledgeBase.addDocument(name, file.toAbsolutePath().toString(), content, tags);

                        titles.add(name);
                        count++;
                    }
                } catch (Exception e) {
                    Logger.warn("Ingester", "Falha ao processar arquivo " + name + ": " + e.getMessage());
                }
            } else if (ext.equalsIgnoreCase("pdf") || ext.equalsIgnoreCase("docx")) {
                // Read text strings heuristic for simple documents
                try {
                    byte[] bytes = Files.readAllBytes(file);
                    String rawText = extractPrintableText(bytes);
                    if (rawText.length() > 50) {
                        String archiveName = System.currentTimeMillis() + "_" + name + ".czip";
                        Path archivePath = driveManager.getDocumentsDir().resolve(archiveName);
                        CompressionEngine.CompressionResult res = CompressionEngine.compressFileToArchive(
                                file, archivePath, config.getCompressionLevel()
                        );
                        totalOrig += res.originalBytes();
                        totalComp += res.compressedBytes();

                        knowledgeBase.addDocument(name, file.toAbsolutePath().toString(), rawText, List.of(ext, "binary_extracted"));
                        titles.add(name);
                        count++;
                    }
                } catch (Exception ignored) {}
            }
        }

        double ratio = totalOrig > 0 ? (1.0 - ((double) totalComp / (double) totalOrig)) * 100.0 : 0.0;
        Logger.success("Ingester", String.format("Ingestão concluída: %d arquivos armazenados no HD com %.2f%% de economia.", count, ratio));

        return new IngestionSummary(count, totalOrig, totalComp, ratio, titles);
    }

    private String extractPrintableText(byte[] data) {
        StringBuilder sb = new StringBuilder();
        int consecutive = 0;
        StringBuilder currentWord = new StringBuilder();
        for (byte b : data) {
            char c = (char) (b & 0xFF);
            if ((c >= 32 && c <= 126) || c == '\n' || c == '\t' || c == '\r' || (c >= 192 && c <= 255)) {
                currentWord.append(c);
                consecutive++;
            } else {
                if (consecutive >= 4) {
                    sb.append(currentWord).append(" ");
                }
                currentWord.setLength(0);
                consecutive = 0;
            }
        }
        return sb.toString().trim();
    }

    private String getFileExtension(String name) {
        int idx = name.lastIndexOf('.');
        return (idx >= 0 && idx < name.length() - 1) ? name.substring(idx + 1) : "";
    }
}

