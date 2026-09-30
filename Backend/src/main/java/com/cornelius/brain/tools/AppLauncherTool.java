package com.cornelius.brain.tools;

import com.cornelius.system.SystemProfile;
import com.cornelius.util.Logger;

import java.awt.Desktop;
import java.net.URI;

public class AppLauncherTool implements AgentTool {
    @Override
    public String getName() {
        return "AppLauncher";
    }

    @Override
    public String getDescription() {
        return "Abre aplicativos desktop (no Windows ou Linux), abre URLs no navegador, controla volume/mídia e envia notificações no sistema.";
    }

    @Override
    public String execute(String input) throws Exception {
        if (input == null || input.isBlank()) {
            return "Erro: Nenhum aplicativo ou URL fornecido.";
        }

        input = input.trim();
        String lower = input.toLowerCase();
        Logger.info("AppLauncher", "Executando ação no host: " + input);

        // 1. Notification
        if (lower.startsWith("notify:") || lower.startsWith("notificação:")) {
            String msg = input.substring(input.indexOf(':') + 1).trim();
            sendNotification("Cornelius.AI", msg);
            return "Notificação exibida no desktop: \"" + msg + "\"";
        }

        // 2. Volume and Audio control (Linux / Windows)
        if (lower.contains("volume") || lower.contains("mutar") || lower.contains("desmutar") || lower.contains("mudo")) {
            return controlAudio(lower);
        }

        // 3. Clean natural language prefixes like "abra o ", "inicie o ", "execute o ", "abrir "
        String target = input.replaceAll("(?i)^(abra\\s+(o\\s+|a\\s+)?|inicie\\s+(o\\s+|a\\s+)?|abrir\\s+(o\\s+|a\\s+)?|lançar\\s+(o\\s+|a\\s+)?)", "").trim();
        String lowerTarget = target.toLowerCase();

        // 4. Common web destinations
        if (lowerTarget.contains("youtube")) {
            return openUrl("https://youtube.com");
        } else if (lowerTarget.contains("google")) {
            return openUrl("https://google.com");
        } else if (lowerTarget.contains("whatsapp")) {
            return openUrl("https://web.whatsapp.com");
        } else if (lowerTarget.contains("github")) {
            return openUrl("https://github.com");
        } else if (lowerTarget.contains("netflix")) {
            return openUrl("https://netflix.com");
        } else if (lowerTarget.contains("chatgpt")) {
            return openUrl("https://chatgpt.com");
        } else if (lowerTarget.contains("gmail") || lowerTarget.contains("e-mail") || lowerTarget.contains("email")) {
            return openUrl("https://mail.google.com");
        } else if (lowerTarget.startsWith("http://") || lowerTarget.startsWith("https://") || lowerTarget.contains(".com") || lowerTarget.contains(".org") || lowerTarget.contains(".io")) {
            String url = target.startsWith("http") ? target : "https://" + target;
            return openUrl(url);
        }

        // 5. Desktop Application launch
        if (SystemProfile.isWindows()) {
            return launchWindowsApp(lowerTarget);
        } else {
            return launchLinuxApp(lowerTarget);
        }
    }

    private String openUrl(String url) {
        com.cornelius.server.LocalApiServer.openUrlInHost(url);
        return "Página aberta no seu navegador padrão: " + url;
    }

    private String controlAudio(String lower) {
        try {
            if (SystemProfile.isWindows()) {
                return "Controle de áudio no Windows enviado.";
            } else {
                if (lower.contains("mutar") || lower.contains("mudo")) {
                    new ProcessBuilder("pactl", "set-sink-mute", "@DEFAULT_SINK@", "1").start();
                    return "Áudio do sistema silenciado (Mutado).";
                } else if (lower.contains("desmutar")) {
                    new ProcessBuilder("pactl", "set-sink-mute", "@DEFAULT_SINK@", "0").start();
                    return "Áudio do sistema restaurado (Desmutado).";
                } else if (lower.matches(".*\\b\\d{1,3}%?\\b.*")) {
                    String num = lower.replaceAll("[^0-9]", "");
                    int vol = Math.min(150, Math.max(0, Integer.parseInt(num)));
                    new ProcessBuilder("pactl", "set-sink-volume", "@DEFAULT_SINK@", vol + "%").start();
                    return "Volume do sistema ajustado para " + vol + "%.";
                } else if (lower.contains("aumente") || lower.contains("mais alto")) {
                    new ProcessBuilder("pactl", "set-sink-volume", "@DEFAULT_SINK@", "+10%").start();
                    return "Volume do sistema aumentado (+10%).";
                } else if (lower.contains("diminua") || lower.contains("mais baixo")) {
                    new ProcessBuilder("pactl", "set-sink-volume", "@DEFAULT_SINK@", "-10%").start();
                    return "Volume do sistema diminuído (-10%).";
                }
            }
        } catch (Exception e) {
            return "Falha ao ajustar volume: " + e.getMessage();
        }
        return "Comando de áudio processado.";
    }

    private String launchWindowsApp(String target) throws Exception {
        if (target.contains("vscode") || target.contains("vs code") || target.contains("código")) {
            new ProcessBuilder("cmd.exe", "/c", "code").start();
            return "Visual Studio Code iniciado com sucesso.";
        } else if (target.contains("terminal") || target.contains("powershell") || target.contains("cmd")) {
            new ProcessBuilder("cmd.exe", "/c", "start", "powershell").start();
            return "Terminal PowerShell aberto.";
        } else if (target.contains("arquivos") || target.contains("explorer") || target.contains("pasta") || target.contains("downloads") || target.contains("documentos")) {
            new ProcessBuilder("explorer.exe", System.getProperty("user.home")).start();
            return "Gerenciador de Arquivos do Windows aberto.";
        } else if (target.contains("notepad") || target.contains("bloco de notas")) {
            new ProcessBuilder("notepad.exe").start();
            return "Bloco de Notas aberto.";
        } else if (target.contains("calculadora") || target.contains("calc")) {
            new ProcessBuilder("calc.exe").start();
            return "Calculadora aberta.";
        } else if (target.contains("spotify")) {
            new ProcessBuilder("cmd.exe", "/c", "start", "spotify:").start();
            return "Spotify aberto.";
        } else {
            new ProcessBuilder("cmd.exe", "/c", "start", target).start();
            return "Aplicativo '" + target + "' iniciado.";
        }
    }

    private String launchLinuxApp(String target) throws Exception {
        String appCmd;
        String desc;
        if (target.contains("vscode") || target.contains("vs code") || target.contains("código")) {
            appCmd = "code &";
            desc = "Visual Studio Code";
        } else if (target.contains("terminal")) {
            appCmd = "(gnome-terminal || io.elementary.terminal || x-terminal-emulator || alacritty || kitty || konsole || xterm) &";
            desc = "Terminal Linux";
        } else if (target.contains("arquivos") || target.contains("files") || target.contains("nautilus") || target.contains("pasta") || target.contains("downloads") || target.contains("documentos")) {
            appCmd = "(nautilus ~ || nemo ~ || thunar ~ || dolphin ~ || xdg-open ~) &";
            desc = "Gerenciador de Arquivos";
        } else if (target.contains("configurações") || target.contains("settings")) {
            appCmd = "gnome-control-center &";
            desc = "Configurações do Sistema";
        } else if (target.contains("calculadora") || target.contains("calc")) {
            appCmd = "(gnome-calculator || kcalc || galculator) &";
            desc = "Calculadora";
        } else if (target.contains("spotify")) {
            appCmd = "(spotify || flatpak run com.spotify.Client || xdg-open https://open.spotify.com) &";
            desc = "Spotify";
        } else {
            appCmd = target + " &";
            desc = target;
        }

        new ProcessBuilder("bash", "-c", appCmd).start();
        return "Aplicativo '" + desc + "' aberto no seu notebook!";
    }

    private void sendNotification(String title, String message) {
        try {
            if (SystemProfile.isWindows()) {
                String psCmd = String.format("[Windows.UI.Notifications.ToastNotificationManager, Windows.UI.Notifications, ContentType = WindowsRuntime] > $null; "
                        + "$template = [Windows.UI.Notifications.ToastNotificationManager]::GetTemplateContent([Windows.UI.Notifications.ToastTemplateType]::ToastText02); "
                        + "$textNodes = $template.GetElementsByTagName('text'); "
                        + "$textNodes.Item(0).AppendChild($template.CreateTextNode('%s')) > $null; "
                        + "$textNodes.Item(1).AppendChild($template.CreateTextNode('%s')) > $null; "
                        + "$toast = [Windows.UI.Notifications.ToastNotification]::new($template); "
                        + "[Windows.UI.Notifications.ToastNotificationManager]::CreateToastNotifier('Cornelius.AI').Show($toast);", title, message);
                new ProcessBuilder("powershell", "-NoProfile", "-NonInteractive", "-Command", psCmd).start();
            } else {
                new ProcessBuilder("notify-send", "-a", "Cornelius.AI", title, message).start();
            }
        } catch (Exception e) {
            Logger.warn("AppLauncher", "Não foi possível disparar notificação: " + e.getMessage());
        }
    }
}
