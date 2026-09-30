package com.cornelius.brain.tools;

import com.cornelius.system.SystemProfile;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class SystemMetricsTool implements AgentTool {

    @Override
    public String getName() {
        return "SystemMetrics";
    }

    @Override
    public String getDescription() {
        return "Obtém telemetria em tempo real do sistema (CPU, Memória, Disco, Bateria e Perfil de Potência do Aparelho).";
    }

    @Override
    public String execute(String input) {
        StringBuilder sb = new StringBuilder("### 💻 Telemetria do Sistema (" + (SystemProfile.isWindows() ? "Windows" : "Linux Pop!_OS") + ")\n\n");

        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();

        sb.append("**Sistema Operacional:** ").append(System.getProperty("os.name"))
                .append(" ").append(System.getProperty("os.version"))
                .append(" (").append(System.getProperty("os.arch")).append(")\n");

        // CPU & Cores
        sb.append("**Processador:** ").append(SystemProfile.getCpuCores()).append(" núcleos de CPU\n");
        sb.append("**Perfil de Potência do Aparelho:** `").append(SystemProfile.getPowerTier().name()).append("` — ").append(SystemProfile.getPowerTierDescription()).append("\n");

        double load = osBean.getSystemLoadAverage();
        if (load >= 0) {
            sb.append(String.format("**Carga Média da CPU:** %.2f\n", load));
        }

        // Memory
        if (SystemProfile.isLinux()) {
            String memInfo = readLinuxMemInfo();
            if (!memInfo.isBlank()) {
                sb.append("**Memória RAM do Sistema:** ").append(memInfo).append("\n");
            }
        } else {
            sb.append(String.format("**Memória RAM Total:** %.1f GB\n", SystemProfile.getTotalRamGb()));
        }

        // Battery Status for notebook
        String battery = readBatteryStatus();
        if (!battery.isBlank()) {
            sb.append("**Bateria do Notebook:** ").append(battery).append("\n");
        }

        // Disk overview
        File[] roots = File.listRoots();
        if (roots != null && roots.length > 0) {
            for (File root : roots) {
                long total = root.getTotalSpace();
                long free = root.getUsableSpace();
                if (total > 0) {
                    long used = total - free;
                    double percent = ((double) used / total) * 100.0;
                    sb.append(String.format("**Disco (%s):** %.1f GB livres de %.1f GB (%.1f%% em uso)\n",
                            root.getAbsolutePath(), free / 1e9, total / 1e9, percent));
                }
            }
        }

        return sb.toString();
    }

    private String readLinuxMemInfo() {
        try {
            Path path = Paths.get("/proc/meminfo");
            if (Files.exists(path)) {
                List<String> lines = Files.readAllLines(path);
                long totalKb = 0;
                long availKb = 0;
                for (String line : lines) {
                    if (line.startsWith("MemTotal:")) {
                        totalKb = parseKb(line);
                    } else if (line.startsWith("MemAvailable:")) {
                        availKb = parseKb(line);
                    }
                }
                if (totalKb > 0) {
                    long usedKb = totalKb - availKb;
                    double usedGb = usedKb / (1024.0 * 1024.0);
                    double totalGb = totalKb / (1024.0 * 1024.0);
                    double percent = ((double) usedKb / totalKb) * 100.0;
                    return String.format("%.1f GB / %.1f GB em uso (%.1f%%)", usedGb, totalGb, percent);
                }
            }
        } catch (Exception ignored) {}
        return "";
    }

    private long parseKb(String line) {
        String[] parts = line.split("\\s+");
        if (parts.length >= 2) {
            try {
                return Long.parseLong(parts[1]);
            } catch (NumberFormatException ignored) {}
        }
        return 0;
    }

    private String readBatteryStatus() {
        try {
            if (SystemProfile.isLinux()) {
                Path bat0 = Paths.get("/sys/class/power_supply/BAT0/capacity");
                Path status0 = Paths.get("/sys/class/power_supply/BAT0/status");
                if (Files.exists(bat0)) {
                    String cap = Files.readString(bat0).trim();
                    String st = Files.exists(status0) ? Files.readString(status0).trim() : "Desconhecido";
                    return cap + "% (" + st + ")";
                }
                Path bat1 = Paths.get("/sys/class/power_supply/BAT1/capacity");
                Path status1 = Paths.get("/sys/class/power_supply/BAT1/status");
                if (Files.exists(bat1)) {
                    String cap = Files.readString(bat1).trim();
                    String st = Files.exists(status1) ? Files.readString(status1).trim() : "Desconhecido";
                    return cap + "% (" + st + ")";
                }
            }
        } catch (Exception ignored) {}
        return "";
    }
}
