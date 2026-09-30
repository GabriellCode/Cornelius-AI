package com.cornelius.brain.tools;

import com.cornelius.system.SystemProfile;
import com.cornelius.util.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

public class TerminalTool implements AgentTool {
    private static final int TIMEOUT_SECONDS = 25;

    @Override
    public String getName() {
        return "Terminal";
    }

    @Override
    public String getDescription() {
        if (SystemProfile.isWindows()) {
            return "Executa comandos nativos no Windows via PowerShell / CMD (gerenciamento de processos, arquivos, rede, etc.).";
        }
        return "Executa comandos shell/bash nativos no Linux Pop!_OS (gerenciamento de processos, pacotes, rede, arquivos, etc.).";
    }

    @Override
    public String execute(String command) throws Exception {
        if (command == null || command.isBlank()) {
            return "Erro: Nenhum comando informado para o terminal.";
        }

        command = command.trim();
        Logger.info("TerminalTool", "Executando comando no sistema (" + (SystemProfile.isWindows() ? "Windows PowerShell" : "Linux Bash") + "): " + command);

        ProcessBuilder pb;
        if (SystemProfile.isWindows()) {
            pb = new ProcessBuilder("powershell.exe", "-NoProfile", "-NonInteractive", "-Command", command);
        } else {
            pb = new ProcessBuilder("bash", "-c", command);
        }

        pb.directory(new File(System.getProperty("user.home")));
        pb.redirectErrorStream(true);

        Process process = pb.start();
        StringBuilder output = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            int lineCount = 0;
            while ((line = reader.readLine()) != null) {
                if (lineCount < 200) { // Limit output to prevent overflow
                    output.append(line).append("\n");
                }
                lineCount++;
            }
            if (lineCount >= 200) {
                output.append("\n... [Saída truncada em 200 linhas]");
            }
        }

        boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            return "Comando interrompido por tempo limite (" + TIMEOUT_SECONDS + "s).\nSaída parcial:\n" + output;
        }

        int exitCode = process.exitValue();
        if (output.isEmpty()) {
            return "Comando executado com sucesso (código de saída " + exitCode + ", sem saída de texto).";
        }

        return output.toString().trim();
    }
}
