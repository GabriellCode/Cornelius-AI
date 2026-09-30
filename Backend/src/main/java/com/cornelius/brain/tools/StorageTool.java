package com.cornelius.brain.tools;

import com.cornelius.storage.DocumentIngester;
import com.cornelius.storage.ExternalDriveManager;
import com.cornelius.storage.KnowledgeBase;
import com.cornelius.storage.MemoryVault;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class StorageTool implements AgentTool {
    private final ExternalDriveManager driveManager;
    private final KnowledgeBase knowledgeBase;
    private final MemoryVault memoryVault;
    private final DocumentIngester ingester;

    public StorageTool(ExternalDriveManager driveManager, KnowledgeBase knowledgeBase, MemoryVault memoryVault, DocumentIngester ingester) {
        this.driveManager = driveManager;
        this.knowledgeBase = knowledgeBase;
        this.memoryVault = memoryVault;
        this.ingester = ingester;
    }

    @Override
    public String getName() {
        return "StorageManager";
    }

    @Override
    public String getDescription() {
        return "Gerencia o armazenamento comprimido do HD externo de 1TB, ingere novos arquivos e consulta o cofre de memórias.";
    }

    @Override
    public String execute(String input) throws Exception {
        if (input == null || input.isBlank() || "status".equalsIgnoreCase(input.trim())) {
            ExternalDriveManager.DriveMetrics m = driveManager.getMetrics();
            return String.format("""
                    ### 🗄️ Status do Cofre de Armazenamento do HD Externo:
                    - **Caminho:** `%s`
                    - **Espaço Total:** %.2f GB (Capacidade aproximada de 1TB)
                    - **Espaço Livre:** %.2f GB
                    - **Espaço Utilizado:** %.2f GB (%.1f%%)
                    - **Dispositivo:** %s
                    - **Total de Arquivos Compactados:** %d arquivos
                    - **Tamanho Original Estimado:** %.2f MB
                    - **Tamanho Comprimido em Disco:** %.2f MB
                    - **Economia de Espaço:** %.2f%%
                    - **Blocos de Conhecimento Ativos no Índice:** %d
                    """,
                    m.path(),
                    m.totalBytes() / 1e9,
                    m.freeBytes() / 1e9,
                    m.usedBytes() / 1e9,
                    m.usedPercent(),
                    m.isExternal() ? "HD Externo Montado" : "Diretório Local do Cofre",
                    m.totalArchivedFiles(),
                    m.totalRawBytesSaved() / 1e6,
                    m.totalCompressedBytes() / 1e6,
                    m.overallSavingsRatio(),
                    knowledgeBase.getChunkCount()
            );
        }

        String lower = input.trim();
        if (lower.startsWith("ingest ") || lower.startsWith("ingerir ")) {
            String pathStr = input.substring(input.indexOf(' ') + 1).trim();
            Path p = Paths.get(pathStr);
            DocumentIngester.IngestionSummary summary = ingester.ingestPath(p);
            return String.format("### 📥 Ingestão Concluída com Sucesso\n- Arquivos processados e comprimidos: %d\n- Economia de espaço: %.2f%%\n- Itens adicionados: %s",
                    summary.filesProcessed(), summary.savingsRatio(), String.join(", ", summary.ingestedTitles()));
        }

        if (lower.startsWith("search ") || lower.startsWith("buscar ")) {
            String q = input.substring(input.indexOf(' ') + 1).trim();
            List<KnowledgeBase.SearchResult> results = knowledgeBase.search(q, 3);
            if (results.isEmpty()) {
                return "Nenhum documento relevante encontrado no HD externo para \"" + q + "\".";
            }
            StringBuilder sb = new StringBuilder("### 🔍 Resultados no Conhecimento do HD:\n\n");
            for (KnowledgeBase.SearchResult r : results) {
                sb.append("- **").append(r.chunk().sourceName()).append("** (Relevância: ")
                        .append(String.format("%.2f", r.score())).append(")\n  > ")
                        .append(r.highlight()).append("\n\n");
            }
            return sb.toString();
        }

        return "Comando de armazenamento não reconhecido. Use: status, ingest <caminho>, ou search <termo>.";
    }
}

