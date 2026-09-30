package com.cornelius.brain.tools;

import java.io.File;
import java.nio.file.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileSystemTool implements AgentTool {
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault());

    @Override
    public String getName() {
        return "FileSystem";
    }

    @Override
    public String getDescription() {
        return "Lê, escreve, lista e busca arquivos e diretórios no sistema de arquivos do notebook (ex: ~/Downloads, ~/Documentos, /media, etc.).";
    }

    @Override
    public String execute(String input) throws Exception {
        if (input == null || input.isBlank()) {
            return "Erro: Nenhuma instrução de arquivo fornecida.";
        }

        input = input.trim();
        String[] parts = input.split("\\s+", 2);
        String action = parts[0].toLowerCase();
        String target = parts.length > 1 ? parts[1].trim() : "";

        // Resolve home tilde
        target = resolvePath(target);

        switch (action) {
            case "list":
            case "ls":
                return listDirectory(target.isBlank() ? System.getProperty("user.home") : target);
            case "read":
            case "cat":
                return readFile(target);
            case "write":
                String[] writeParts = target.split(":::", 2);
                if (writeParts.length < 2) {
                    return "Uso para escrita: write <caminho_arquivo> ::: <conteudo>";
                }
                return writeFile(resolvePath(writeParts[0].trim()), writeParts[1].trim());
            case "search":
            case "find":
                return findFiles(target);
            default:
                // Default heuristic: If path exists, read or list it
                File f = new File(resolvePath(input));
                if (f.exists()) {
                    return f.isDirectory() ? listDirectory(f.getAbsolutePath()) : readFile(f.getAbsolutePath());
                }
                return listDirectory(System.getProperty("user.home"));
        }
    }

    private String resolvePath(String path) {
        if (path.startsWith("~")) {
            return System.getProperty("user.home") + path.substring(1);
        }
        return path;
    }

    private String listDirectory(String pathStr) {
        Path p = Paths.get(pathStr);
        if (!Files.exists(p)) {
            return "Diretório não encontrado: " + pathStr;
        }
        if (!Files.isDirectory(p)) {
            return readFile(pathStr);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Conteúdo do Diretório [").append(p.toAbsolutePath()).append("]:\n");

        try (Stream<Path> stream = Files.list(p)) {
            List<Path> entries = stream.sorted((a, b) -> {
                boolean aDir = Files.isDirectory(a);
                boolean bDir = Files.isDirectory(b);
                if (aDir != bDir) return aDir ? -1 : 1;
                return a.getFileName().compareTo(b.getFileName());
            }).limit(100).collect(Collectors.toList());

            for (Path entry : entries) {
                boolean isDir = Files.isDirectory(entry);
                long size = isDir ? 0 : Files.size(entry);
                Instant modTime = Files.getLastModifiedTime(entry).toInstant();
                String modStr = DATE_FMT.format(modTime);
                String sizeStr = isDir ? "<DIR>" : formatSize(size);

                sb.append(String.format(" %-6s  %-10s  %s  %s\n",
                        isDir ? "[DIR]" : "[FILE]",
                        sizeStr,
                        modStr,
                        entry.getFileName().toString()));
            }
        } catch (Exception e) {
            return "Erro ao listar diretório: " + e.getMessage();
        }

        return sb.toString();
    }

    private String readFile(String pathStr) {
        Path p = Paths.get(pathStr);
        if (!Files.exists(p)) {
            return "Arquivo não encontrado: " + pathStr;
        }
        if (Files.isDirectory(p)) {
            return listDirectory(pathStr);
        }

        try {
            long size = Files.size(p);
            if (size > 1024 * 1024 * 5) { // 5 MB limit
                return "O arquivo é muito grande para leitura direta (" + formatSize(size) + "). Use o cofre comprimido para processá-lo.";
            }
            return "Conteúdo de [" + p.getFileName() + "]:\n\n" + Files.readString(p);
        } catch (Exception e) {
            return "Erro ao ler arquivo: " + e.getMessage();
        }
    }

    private String writeFile(String pathStr, String content) {
        try {
            Path p = Paths.get(pathStr);
            if (p.getParent() != null) {
                Files.createDirectories(p.getParent());
            }
            Files.writeString(p, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            return "Arquivo gravado com sucesso em: " + p.toAbsolutePath() + " (" + content.length() + " caracteres).";
        } catch (Exception e) {
            return "Erro ao gravar arquivo: " + e.getMessage();
        }
    }

    private String findFiles(String pattern) {
        Path root = Paths.get(System.getProperty("user.home"));
        StringBuilder sb = new StringBuilder("Arquivos encontrados para '" + pattern + "':\n");

        try (Stream<Path> stream = Files.walk(root, 4)) {
            List<String> matches = stream
                    .filter(p -> p.getFileName().toString().toLowerCase().contains(pattern.toLowerCase()))
                    .limit(30)
                    .map(Path::toString)
                    .collect(Collectors.toList());

            if (matches.isEmpty()) {
                return "Nenhum arquivo correspondente encontrado para '" + pattern + "' nos diretórios principais.";
            }

            for (String m : matches) {
                sb.append(" • ").append(m).append("\n");
            }
        } catch (Exception e) {
            return "Erro ao buscar arquivos: " + e.getMessage();
        }

        return sb.toString();
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }
}

