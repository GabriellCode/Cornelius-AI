package com.cornelius;

import com.cornelius.brain.CorneliusBrain;
import com.cornelius.config.CorneliusConfig;
import com.cornelius.server.LocalApiServer;
import com.cornelius.ui.CorneliusWindow;
import com.cornelius.ui.ModernTheme;
import com.cornelius.util.Logger;
import java.awt.*;
import java.util.Scanner;
import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        Logger.info("Main", "Inicializando Cornelius - Mordomo Pessoal de Inteligência Artificial...");

        CorneliusConfig config = CorneliusConfig.load();
        CorneliusBrain brain = new CorneliusBrain(config);

        // Start Local API Server in background
        LocalApiServer apiServer = new LocalApiServer(brain, config);
        apiServer.start();

        // Start Discord Bot Service in background (for mobile chatting)
        com.cornelius.bot.DiscordBotService discordBot = new com.cornelius.bot.DiscordBotService(brain, config);
        if (config.getDiscordToken() != null && !config.getDiscordToken().isBlank()) {
            discordBot.start();
        }

        // Register shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Logger.info("Main", "Encerrando Cornelius e arquivando memórias no cofre comprimido do HD...");
            brain.archiveConversationSession();
            brain.getKnowledgeBase().saveIndex();
            brain.getMemoryVault().saveMemories();
            discordBot.stop();
            apiServer.stop();
            Logger.success("Main", "Cofre protegido e salvo com sucesso. Até logo!");
        }));

        boolean cliMode = false;
        boolean serverOnly = false;

        for (String arg : args) {
            if ("--cli".equalsIgnoreCase(arg) || "-c".equalsIgnoreCase(arg)) {
                cliMode = true;
            } else if ("--server".equalsIgnoreCase(arg) || "-s".equalsIgnoreCase(arg)) {
                serverOnly = true;
            }
        }

        if (serverOnly) {
            Logger.info("Main", "Cornelius em execução em modo servidor dedicado.");
            try {
                Thread.currentThread().join();
            } catch (InterruptedException ignored) {}
            return;
        }

        if (cliMode || GraphicsEnvironment.isHeadless()) {
            runCliInterface(brain, config);
        } else {
            SwingUtilities.invokeLater(() -> {
                ModernTheme.applyGlobal();
                CorneliusWindow window = new CorneliusWindow(brain);
                window.setVisible(true);
                Logger.success("Main", "Interface Desktop do Cornelius inicializada com sucesso no Pop!_OS.");
            });
        }
    }

    private static void runCliInterface(CorneliusBrain brain, CorneliusConfig config) {
        Logger.butler("Modo Terminal Ativado. Digite sua mensagem para Cornelius (ou 'sair' para encerrar):");
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("\n\u001B[34m[Você]\u001B[0m > ");
            if (!scanner.hasNextLine()) break;
            String input = scanner.nextLine().trim();
            if (input.equalsIgnoreCase("sair") || input.equalsIgnoreCase("exit")) {
                break;
            }
            if (input.isEmpty()) continue;

            CorneliusBrain.BrainResponse resp = brain.processUserMessage(input);
            System.out.printf("\n\u001B[33m[Cornelius - %s]\u001B[0m\n%s\n", resp.providerUsed(), resp.text());
        }
    }
}

