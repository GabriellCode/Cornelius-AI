package com.cornelius.system;

import com.cornelius.util.Logger;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class AutostartManager {

    public static boolean isAutostartEnabled() {
        if (SystemProfile.isWindows()) {
            return isWindowsAutostartEnabled();
        } else {
            return isLinuxAutostartEnabled();
        }
    }

    public static boolean enableAutostart() {
        if (SystemProfile.isWindows()) {
            return enableWindowsAutostart();
        } else {
            return enableLinuxAutostart();
        }
    }

    public static boolean disableAutostart() {
        if (SystemProfile.isWindows()) {
            return disableWindowsAutostart();
        } else {
            return disableLinuxAutostart();
        }
    }

    public static String getAutostartDetails() {
        if (SystemProfile.isWindows()) {
            Path startup = getWindowsStartupDir();
            boolean enabled = isWindowsAutostartEnabled();
            return enabled ? "Habilitado no Startup do Windows: " + startup : "Desabilitado";
        } else {
            Path desktop = getLinuxAutostartDesktopPath();
            Path service = getLinuxSystemdServicePath();
            boolean enabled = isLinuxAutostartEnabled();
            if (enabled) {
                return "Habilitado (Systemd: " + Files.exists(service) + ", XDG: " + Files.exists(desktop) + ")";
            }
            return "Desabilitado";
        }
    }

    // --- LINUX IMPLEMENTATION ---

    private static Path getLinuxAutostartDesktopPath() {
        return Paths.get(System.getProperty("user.home"), ".config", "autostart", "cornelius.desktop");
    }

    private static Path getLinuxSystemdServicePath() {
        return Paths.get(System.getProperty("user.home"), ".config", "systemd", "user", "cornelius.service");
    }

    private static boolean isLinuxAutostartEnabled() {
        return Files.exists(getLinuxAutostartDesktopPath()) || Files.exists(getLinuxSystemdServicePath());
    }

    private static boolean enableLinuxAutostart() {
        try {
            Path appDir = Paths.get(System.getProperty("user.dir"));
            Path setupScript = appDir.resolve("setup-autostart.sh");

            if (Files.exists(setupScript)) {
                new ProcessBuilder("bash", setupScript.toAbsolutePath().toString()).inheritIO().start().waitFor();
                return true;
            }

            // Fallback manual desktop creation
            Path desktopPath = getLinuxAutostartDesktopPath();
            Files.createDirectories(desktopPath.getParent());
            String desktopContent = "[Desktop Entry]\n"
                    + "Type=Application\n"
                    + "Version=1.0\n"
                    + "Name=Cornelius AI\n"
                    + "Comment=Autostart do Cornelius AI no Boot\n"
                    + "Exec=" + appDir.resolve("start-discord-background.sh").toAbsolutePath() + "\n"
                    + "Icon=" + appDir.resolve("cornelius.png").toAbsolutePath() + "\n"
                    + "Terminal=false\n"
                    + "Categories=Utility;ArtificialIntelligence;\n"
                    + "X-GNOME-Autostart-enabled=true\n";

            Files.writeString(desktopPath, desktopContent);
            return true;
        } catch (Exception e) {
            Logger.error("Autostart", "Falha ao ativar autostart no Linux: " + e.getMessage());
            return false;
        }
    }

    private static boolean disableLinuxAutostart() {
        try {
            Path appDir = Paths.get(System.getProperty("user.dir"));
            Path disableScript = appDir.resolve("disable-autostart.sh");

            if (Files.exists(disableScript)) {
                new ProcessBuilder("bash", disableScript.toAbsolutePath().toString()).inheritIO().start().waitFor();
                return true;
            }

            Files.deleteIfExists(getLinuxAutostartDesktopPath());
            Files.deleteIfExists(getLinuxSystemdServicePath());
            return true;
        } catch (Exception e) {
            Logger.error("Autostart", "Falha ao desativar autostart no Linux: " + e.getMessage());
            return false;
        }
    }

    // --- WINDOWS IMPLEMENTATION ---

    private static Path getWindowsStartupDir() {
        String appData = System.getenv("APPDATA");
        if (appData == null || appData.isBlank()) {
            appData = System.getProperty("user.home") + "\\AppData\\Roaming";
        }
        return Paths.get(appData, "Microsoft", "Windows", "Start Menu", "Programs", "Startup");
    }

    private static boolean isWindowsAutostartEnabled() {
        Path startupDir = getWindowsStartupDir();
        Path vbs = startupDir.resolve("Cornelius.vbs");
        Path bat = startupDir.resolve("Cornelius.bat");
        return Files.exists(vbs) || Files.exists(bat);
    }

    private static boolean enableWindowsAutostart() {
        try {
            Path startupDir = getWindowsStartupDir();
            Files.createDirectories(startupDir);

            Path currentDir = Paths.get(System.getProperty("user.dir"));
            Path targetVbs = startupDir.resolve("Cornelius.vbs");

            String vbsContent = "Set WshShell = CreateObject(\"WScript.Shell\")\n"
                    + "WshShell.CurrentDirectory = \"" + currentDir.toAbsolutePath().toString().replace("\\", "\\\\") + "\"\n"
                    + "WshShell.Run \"cmd /c \"\"\" & WshShell.CurrentDirectory & \"\\Cornelius.bat\"\"\", 0, False\n"
                    + "Set WshShell = Nothing\n";

            Files.writeString(targetVbs, vbsContent);
            Logger.success("Autostart", "Autostart do Windows configurado com sucesso em: " + targetVbs);
            return true;
        } catch (Exception e) {
            Logger.error("Autostart", "Falha ao configurar autostart no Windows: " + e.getMessage());
            return false;
        }
    }

    private static boolean disableWindowsAutostart() {
        try {
            Path startupDir = getWindowsStartupDir();
            Files.deleteIfExists(startupDir.resolve("Cornelius.vbs"));
            Files.deleteIfExists(startupDir.resolve("Cornelius.bat"));
            Logger.info("Autostart", "Autostart do Windows removido.");
            return true;
        } catch (Exception e) {
            Logger.error("Autostart", "Falha ao desativar autostart no Windows: " + e.getMessage());
            return false;
        }
    }
}

