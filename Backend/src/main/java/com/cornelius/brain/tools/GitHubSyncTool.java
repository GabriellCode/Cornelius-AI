package com.cornelius.brain.tools;

import com.cornelius.config.CorneliusConfig;
import com.cornelius.system.GitHubSyncEngine;
import com.cornelius.util.Logger;

/**
 * Ferramenta nativa do Cornelius para sincronizar o projeto com o GitHub via Java 21.
 */
public class GitHubSyncTool implements AgentTool {
    private final CorneliusConfig config;

    public GitHubSyncTool(CorneliusConfig config) {
        this.config = config;
    }

    @Override
    public String getName() {
        return "GitHubSync";
    }

    @Override
    public String getDescription() {
        return "Sincroniza e atualiza todo o repositório do projeto Cornelius no GitHub em Java nativo.";
    }

    @Override
    public String execute(String input) {
        String msg = (input != null && !input.isBlank()) ? input.trim() : "auto-sync via Cornelius AI";
        Logger.info("Tool:GitHubSync", "Executando sincronização GitHub: " + msg);
        GitHubSyncEngine.SyncResult result = GitHubSyncEngine.sync(config, msg);
        if (result.success()) {
            return "✅ Repositório GitHub sincronizado com sucesso!\n" +
                   "Commit: " + result.commitSha() + "\n" +
                   "Arquivos enviados: " + result.filesUploaded() + " de " + result.totalFiles() + "\n" +
                   "Tempo: " + result.elapsedMs() + "ms\n" +
                   "URL: https://github.com/" + config.getGithubRepo();
        } else {
            return "❌ Falha na sincronização GitHub: " + result.message();
        }
    }
}
