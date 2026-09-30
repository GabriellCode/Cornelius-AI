package com.cornelius.system;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class SystemProfile {

    public enum OSType {
        WINDOWS,
        LINUX,
        MACOS,
        UNKNOWN
    }

    public enum PowerTier {
        HIGH_PERFORMANCE, // >= 8 cores and >= 16 GB RAM
        BALANCED,         // 4-7 cores or 8-15 GB RAM
        LOW_POWER         // <= 3 cores or <= 6 GB RAM
    }

    private static final OSType OS_TYPE;
    private static final int CPU_CORES;
    private static final long TOTAL_RAM_BYTES;
    private static final PowerTier POWER_TIER;

    static {
        String osName = System.getProperty("os.name", "").toLowerCase();
        if (osName.contains("win")) {
            OS_TYPE = OSType.WINDOWS;
        } else if (osName.contains("mac") || osName.contains("darwin")) {
            OS_TYPE = OSType.MACOS;
        } else if (osName.contains("nix") || osName.contains("nux") || osName.contains("aix")) {
            OS_TYPE = OSType.LINUX;
        } else {
            OS_TYPE = OSType.UNKNOWN;
        }

        CPU_CORES = Runtime.getRuntime().availableProcessors();
        TOTAL_RAM_BYTES = detectTotalRam();

        double totalRamGb = TOTAL_RAM_BYTES / (1024.0 * 1024.0 * 1024.0);

        if (CPU_CORES >= 8 && totalRamGb >= 14.0) {
            POWER_TIER = PowerTier.HIGH_PERFORMANCE;
        } else if (CPU_CORES >= 4 && totalRamGb >= 6.0) {
            POWER_TIER = PowerTier.BALANCED;
        } else {
            POWER_TIER = PowerTier.LOW_POWER;
        }
    }

    public static OSType getOsType() {
        return OS_TYPE;
    }

    public static boolean isWindows() {
        return OS_TYPE == OSType.WINDOWS;
    }

    public static boolean isLinux() {
        return OS_TYPE == OSType.LINUX;
    }

    public static boolean isMac() {
        return OS_TYPE == OSType.MACOS;
    }

    public static int getCpuCores() {
        return CPU_CORES;
    }

    public static long getTotalRamBytes() {
        return TOTAL_RAM_BYTES;
    }

    public static double getTotalRamGb() {
        return TOTAL_RAM_BYTES / (1024.0 * 1024.0 * 1024.0);
    }

    public static PowerTier getPowerTier() {
        return POWER_TIER;
    }

    public static int getRecommendedCompressionLevel() {
        return switch (POWER_TIER) {
            case HIGH_PERFORMANCE -> 9;
            case BALANCED -> 7;
            case LOW_POWER -> 4;
        };
    }

    public static int getRecommendedConcurrency() {
        return switch (POWER_TIER) {
            case HIGH_PERFORMANCE -> Math.max(4, CPU_CORES);
            case BALANCED -> Math.max(2, CPU_CORES / 2);
            case LOW_POWER -> 2;
        };
    }

    public static String getPowerTierDescription() {
        return switch (POWER_TIER) {
            case HIGH_PERFORMANCE -> "ALTO DESEMPENHO (Execução pesada, multithreading avançado e compressão máxima habilitada)";
            case BALANCED -> "EQUILIBRADO (Bom balanço entre economia de bateria, responsividade e CPU)";
            case LOW_POWER -> "ECONOMIA DE ENERGIA / LEVE (Modo otimizado para preservar bateria e uso de memória)";
        };
    }

    public static String getSystemSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("- Sistema Operacional: %s %s (%s)\n",
                System.getProperty("os.name"),
                System.getProperty("os.version"),
                System.getProperty("os.arch")));
        sb.append(String.format("- Hardware: %d Núcleos de CPU | %.1f GB de Memória RAM Total\n",
                CPU_CORES, getTotalRamGb()));
        sb.append(String.format("- Perfil de Potência do Aparelho: %s\n", getPowerTierDescription()));
        sb.append(String.format("- Shell do Sistema: %s\n", isWindows() ? "PowerShell / CMD (Windows)" : "/bin/bash (Linux/Unix)"));
        sb.append(String.format("- Pasta do Usuário: %s\n", System.getProperty("user.home")));
        return sb.toString();
    }

    private static long detectTotalRam() {
        try {
            // Try Linux /proc/meminfo
            if (isLinux()) {
                Path meminfo = Paths.get("/proc/meminfo");
                if (Files.exists(meminfo)) {
                    List<String> lines = Files.readAllLines(meminfo);
                    for (String l : lines) {
                        if (l.startsWith("MemTotal:")) {
                            String[] parts = l.split("\\s+");
                            if (parts.length >= 2) {
                                return Long.parseLong(parts[1]) * 1024L;
                            }
                        }
                    }
                }
            }

            // Reflection on com.sun.management.OperatingSystemMXBean for Windows / Mac / Linux
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            try {
                var method = osBean.getClass().getMethod("getTotalMemorySize");
                Object val = method.invoke(osBean);
                if (val instanceof Number n && n.longValue() > 0) {
                    return n.longValue();
                }
            } catch (Exception ignored) {}

            try {
                var method = osBean.getClass().getMethod("getTotalPhysicalMemorySize");
                Object val = method.invoke(osBean);
                if (val instanceof Number n && n.longValue() > 0) {
                    return n.longValue();
                }
            } catch (Exception ignored) {}

        } catch (Exception ignored) {}

        // Fallback default: JVM max memory * 4 or 8GB estimate
        long jvmMax = Runtime.getRuntime().maxMemory();
        return Math.max(jvmMax * 4, 8L * 1024 * 1024 * 1024);
    }
}

