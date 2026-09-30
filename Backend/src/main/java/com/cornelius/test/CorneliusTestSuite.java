package com.cornelius.test;

import com.cornelius.brain.tools.SystemMetricsTool;
import com.cornelius.config.CorneliusConfig;
import com.cornelius.storage.CompressionEngine;
import com.cornelius.storage.ExternalDriveManager;
import com.cornelius.storage.KnowledgeBase;
import com.cornelius.storage.MemoryVault;
import com.cornelius.util.JsonParser;
import com.cornelius.util.Logger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class CorneliusTestSuite {

    public static void main(String[] args) {
        System.out.println("=== EXECUTANDO BATERIA DE TESTES DO CORNELIUS ===");

        int passed = 0;
        int total = 0;

        total++;
        if (testJsonParser()) passed++;

        total++;
        if (testCompressionEngine()) passed++;

        total++;
        if (testExternalDriveAndVault()) passed++;

        total++;
        if (testKnowledgeBaseRAG()) passed++;

        total++;
        if (testSystemMetricsTool()) passed++;

        total++;
        if (testTerminalTool()) passed++;

        total++;
        if (testFileSystemTool()) passed++;

        total++;
        if (testSystemProfile()) passed++;

        total++;
        if (testAutonomousActionEngine()) passed++;

        total++;
        if (testInstagramTool()) passed++;

        System.out.println("==================================================");
        System.out.printf("RESULTADO FINAL: %d/%d TESTES APROVADOS (%s)\n",
                passed, total, (passed == total ? "SUCESSO TOTAL" : "FALHAS DETECTADAS"));
        System.out.println("==================================================");

        if (passed != total) {
            System.exit(1);
        }
    }

    private static boolean testJsonParser() {
        try {
            String json = "{\"name\":\"Cornelius\",\"version\":1.0,\"active\":true,\"tools\":[\"web\",\"hd\"]}";
            Map<String, Object> map = JsonParser.parseObject(json);
            assert "Cornelius".equals(map.get("name"));
            assert Boolean.TRUE.equals(map.get("active"));
            assert map.get("tools") instanceof List;
            String generated = JsonParser.toJson(map);
            assert generated.contains("\"Cornelius\"");
            Logger.success("Test-Json", "JsonParser aprovado!");
            return true;
        } catch (Throwable t) {
            Logger.error("Test-Json", "Falha no JsonParser: " + t.getMessage());
            return false;
        }
    }

    private static boolean testCompressionEngine() {
        try {
            String sample = "Cornelius é o mordomo de inteligência artificial de alta performance para Linux Pop!_OS. "
                    .repeat(100);

            CompressionEngine.CompressionResult res = CompressionEngine.compressString(sample, 9);
            assert res.compressedBytes() < res.originalBytes() : "Comprimido deve ser menor que o original";
            assert res.compressionRatioPercent() > 50.0 : "Economia esperada acima de 50%";

            String decompressed = CompressionEngine.decompressString(res.compressedData());
            assert sample.equals(decompressed) : "Dados descomprimidos devem ser idênticos";

            // Test file archive format
            Path tempSrc = Files.createTempFile("test_src_", ".txt");
            Files.writeString(tempSrc, sample);
            Path tempArch = Files.createTempFile("test_arch_", ".czip");

            CompressionEngine.compressFileToArchive(tempSrc, tempArch, 9);
            byte[] decompBytes = CompressionEngine.decompressArchive(tempArch);
            assert sample.equals(new String(decompBytes, StandardCharsets.UTF_8));

            Files.deleteIfExists(tempSrc);
            Files.deleteIfExists(tempArch);

            Logger.success("Test-Compression", String.format("CompressionEngine aprovado (Economia: %.2f%%)", res.compressionRatioPercent()));
            return true;
        } catch (Throwable t) {
            Logger.error("Test-Compression", "Falha no CompressionEngine: " + t.getMessage());
            return false;
        }
    }

    private static boolean testExternalDriveAndVault() {
        try {
            CorneliusConfig config = new CorneliusConfig();
            Path tempVault = Files.createTempDirectory("cornelius_test_vault_");
            config.setExternalDrivePath(tempVault.toAbsolutePath().toString());

            ExternalDriveManager manager = new ExternalDriveManager(config);
            ExternalDriveManager.DriveMetrics metrics = manager.getMetrics();
            assert metrics.totalBytes() > 0;

            MemoryVault vault = new MemoryVault(manager, config);
            vault.addMemory("FACT", "Teste de memorização do mordomo", 5);
            assert vault.getFormattedMemoriesForPrompt().contains("Teste de memorização do mordomo");

            Logger.success("Test-Storage", "ExternalDriveManager e MemoryVault aprovados!");
            return true;
        } catch (Throwable t) {
            Logger.error("Test-Storage", "Falha no Storage: " + t.getMessage());
            return false;
        }
    }

    private static boolean testKnowledgeBaseRAG() {
        try {
            CorneliusConfig config = new CorneliusConfig();
            Path tempVault = Files.createTempDirectory("cornelius_test_kb_");
            config.setExternalDrivePath(tempVault.toAbsolutePath().toString());

            ExternalDriveManager manager = new ExternalDriveManager(config);
            KnowledgeBase kb = new KnowledgeBase(manager, config);

            kb.addDocument("Manual Java", "/tmp/java.txt", "Java 21 traz Virtual Threads, Pattern Matching e alta performance concorrente para aplicações Linux.", List.of("java", "doc"));
            kb.addDocument("Manual Linux", "/tmp/linux.txt", "Pop!_OS é uma distribuição Linux baseada no Ubuntu desenvolvida pela System76 focada em desenvolvedores.", List.of("linux", "os"));

            List<KnowledgeBase.SearchResult> results = kb.search("Virtual Threads", 1);
            assert !results.isEmpty() : "Deve encontrar resultado para Virtual Threads";
            assert results.get(0).chunk().sourceName().contains("Manual Java") : "Deve corresponder ao manual Java";

            Logger.success("Test-RAG", "KnowledgeBase BM25 RAG aprovado!");
            return true;
        } catch (Throwable t) {
            Logger.error("Test-RAG", "Falha no KnowledgeBase RAG: " + t.getMessage());
            return false;
        }
    }

    private static boolean testSystemMetricsTool() {
        try {
            SystemMetricsTool tool = new SystemMetricsTool();
            String output = tool.execute("");
            assert output.contains("Telemetria do Sistema");
            assert output.contains("Processador");
            assert output.contains("Armazenamento Raiz");
            Logger.success("Test-SystemMetrics", "SystemMetricsTool aprovado!");
            return true;
        } catch (Throwable t) {
            Logger.error("Test-SystemMetrics", "Falha no SystemMetricsTool: " + t.getMessage());
            return false;
        }
    }

    private static boolean testTerminalTool() {
        try {
            com.cornelius.brain.tools.TerminalTool tool = new com.cornelius.brain.tools.TerminalTool();
            String output = tool.execute("echo 'Cornelius System Access OK'");
            assert output.contains("Cornelius System Access OK") : "Deve conter a saída do comando echo";
            Logger.success("Test-Terminal", "TerminalTool aprovado!");
            return true;
        } catch (Throwable t) {
            Logger.error("Test-Terminal", "Falha no TerminalTool: " + t.getMessage());
            return false;
        }
    }

    private static boolean testFileSystemTool() {
        try {
            com.cornelius.brain.tools.FileSystemTool tool = new com.cornelius.brain.tools.FileSystemTool();
            Path temp = Files.createTempFile("cornelius_fs_test_", ".txt");
            tool.execute("write " + temp.toAbsolutePath() + " ::: Teste de escrita do mordomo");
            String read = tool.execute("read " + temp.toAbsolutePath());
            assert read.contains("Teste de escrita do mordomo");
            Files.deleteIfExists(temp);
            Logger.success("Test-FileSystem", "FileSystemTool aprovado!");
            return true;
        } catch (Throwable t) {
            Logger.error("Test-FileSystem", "Falha no FileSystemTool: " + t.getMessage());
            return false;
        }
    }

    private static boolean testSystemProfile() {
        try {
            com.cornelius.system.SystemProfile.OSType os = com.cornelius.system.SystemProfile.getOsType();
            assert os != null : "OS Type deve ser detectado";
            assert com.cornelius.system.SystemProfile.getCpuCores() > 0 : "CPU Cores deve ser > 0";
            assert com.cornelius.system.SystemProfile.getTotalRamBytes() > 0 : "RAM total deve ser > 0";
            assert com.cornelius.system.SystemProfile.getPowerTier() != null : "Power Tier deve ser classificado";
            String summary = com.cornelius.system.SystemProfile.getSystemSummary();
            assert summary.contains("Sistema Operacional") : "Resumo deve conter sistema operacional";
            assert summary.contains("Perfil de Potência") : "Resumo deve conter perfil de potência";
            Logger.success("Test-SystemProfile", "SystemProfile aprovado (" + os + " | " + com.cornelius.system.SystemProfile.getPowerTier() + ")!");
            return true;
        } catch (Throwable t) {
            Logger.error("Test-SystemProfile", "Falha no SystemProfile: " + t.getMessage());
            return false;
        }
    }

    private static boolean testAutonomousActionEngine() {
        try {
            CorneliusConfig config = new CorneliusConfig();
            com.cornelius.brain.CorneliusBrain brain = new com.cornelius.brain.CorneliusBrain(config);

            // Test 1: Direct Natural Action Trigger
            com.cornelius.brain.CorneliusBrain.BrainResponse resp1 = brain.processUserMessage("qual a bateria e memoria do sistema?");
            assert resp1.toolsExecuted().contains("SystemMetrics") : "Deve conter SystemMetrics";

            // Test 2: Terminal execution
            com.cornelius.brain.CorneliusBrain.BrainResponse resp2 = brain.processUserMessage("execute o comando echo 'Cornelius Total Access OK'");
            assert resp2.toolsExecuted().stream().anyMatch(t -> t.contains("Terminal")) : "Deve conter Terminal";

            Logger.success("Test-AutonomousAction", "Motor de Ações Autônomas e Acesso Total Aprovados!");
            return true;
        } catch (Throwable t) {
            Logger.error("Test-AutonomousAction", "Falha no Motor de Ações: " + t.getMessage());
            return false;
        }
    }

    private static boolean testInstagramTool() {
        try {
            CorneliusConfig config = new CorneliusConfig();
            config.setInstagramUsername("cornelius.ai");
            com.cornelius.brain.tools.InstagramTool tool = new com.cornelius.brain.tools.InstagramTool(config);

            String status = tool.execute("status");
            assert status.contains("INSTAGRAM") : "Status deve conter INSTAGRAM";
            assert status.contains("cornelius.ai") : "Status deve conter o username";

            Logger.success("Test-Instagram", "InstagramTool aprovado!");
            return true;
        } catch (Throwable t) {
            Logger.error("Test-Instagram", "Falha no InstagramTool: " + t.getMessage());
            return false;
        }
    }
}

