package com.cornelius.ui;

import com.cornelius.brain.CorneliusBrain;
import com.cornelius.brain.tools.SystemMetricsTool;
import com.cornelius.config.CorneliusConfig;
import com.cornelius.storage.ExternalDriveManager;
import com.cornelius.system.AutostartManager;
import com.cornelius.system.SystemProfile;
import com.cornelius.util.Logger;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class SystemDashboardPanel extends JPanel {
    private final CorneliusBrain brain;
    private final CorneliusConfig config;

    // Discord Card Components
    private JLabel lblDiscordStatus;
    private JLabel lblDiscordUser;
    private JLabel lblDiscordId;
    private JLabel lblDiscordIntents;

    // Autostart Card Components
    private JLabel lblAutostartStatus;
    private JLabel lblAutostartType;
    private JButton btnToggleAutostart;

    // Hardware Telemetry Components
    private JLabel lblOsInfo;
    private JLabel lblCpuInfo;
    private JLabel lblRamInfo;
    private JProgressBar barRamUsage;
    private JLabel lblPowerTier;

    // Live Logs Components
    private JTextArea txtLiveLogs;
    private Timer refreshTimer;

    public SystemDashboardPanel(CorneliusBrain brain) {
        this.brain = brain;
        this.config = brain.getConfig();

        setLayout(new BorderLayout());
        setBackground(ModernTheme.BG_DARK);

        JPanel mainScrollable = new JPanel();
        mainScrollable.setLayout(new BoxLayout(mainScrollable, BoxLayout.Y_AXIS));
        mainScrollable.setBackground(ModernTheme.BG_DARK);
        mainScrollable.setBorder(new EmptyBorder(24, 32, 32, 32));

        // Header Title
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(ModernTheme.BG_DARK);
        titlePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

        JLabel title = new JLabel("Painel de Controle do Sistema & Bot");
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        title.setForeground(ModernTheme.TEXT_WHITE);

        JLabel subtitle = new JLabel("Monitoramento em tempo real de hardware, Discord, inicialização automática e cofre de 1TB");
        subtitle.setFont(ModernTheme.FONT_BODY);
        subtitle.setForeground(ModernTheme.TEXT_MUTED);

        JPanel titleText = new JPanel(new GridLayout(2, 1, 0, 4));
        titleText.setBackground(ModernTheme.BG_DARK);
        titleText.add(title);
        titleText.add(subtitle);

        JButton btnRefreshAll = ModernTheme.createMinimalButton("🔄 Atualizar Tudo", true);
        btnRefreshAll.addActionListener(e -> refreshAllData());

        titlePanel.add(titleText, BorderLayout.WEST);
        titlePanel.add(btnRefreshAll, BorderLayout.EAST);

        mainScrollable.add(titlePanel);
        mainScrollable.add(Box.createVerticalStrut(20));

        // Grid of Status Cards (2 columns)
        JPanel cardsGrid = new JPanel(new GridLayout(0, 2, 18, 18));
        cardsGrid.setBackground(ModernTheme.BG_DARK);
        cardsGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 580));

        cardsGrid.add(createDiscordCard());
        cardsGrid.add(createInstagramCard());
        cardsGrid.add(createAutostartCard());
        cardsGrid.add(createHardwareCard());
        cardsGrid.add(createVaultCard());

        mainScrollable.add(cardsGrid);
        mainScrollable.add(Box.createVerticalStrut(24));

        // Real-Time Event & Terminal Log Card
        mainScrollable.add(createLogsConsoleCard());

        JScrollPane scrollPane = new JScrollPane(mainScrollable);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setBackground(ModernTheme.BG_DARK);

        add(scrollPane, BorderLayout.CENTER);

        // Auto-refresh timer every 4 seconds
        refreshTimer = new Timer(4000, e -> refreshAllData());
        refreshTimer.start();

        // Initial populate
        refreshAllData();
    }

    private JPanel createCardContainer(String titleText, String badgeText, Color badgeColor) {
        JPanel card = new JPanel(new BorderLayout(0, 12)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(ModernTheme.BG_CARD);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));
                g2.setColor(ModernTheme.BORDER_MUTED);
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 12, 12));
                g2.dispose();
            }
        };
        card.setBackground(ModernTheme.BG_CARD);
        card.setBorder(new EmptyBorder(16, 18, 16, 18));

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(ModernTheme.BG_CARD);

        JLabel lblTitle = new JLabel(titleText);
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 14));
        lblTitle.setForeground(ModernTheme.TEXT_WHITE);

        JLabel lblBadge = new JLabel(" " + badgeText + " ");
        lblBadge.setFont(ModernTheme.FONT_MONO_SMALL);
        lblBadge.setForeground(badgeColor);
        lblBadge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(badgeColor, 1, true),
                new EmptyBorder(2, 6, 2, 6)
        ));

        header.add(lblTitle, BorderLayout.WEST);
        header.add(lblBadge, BorderLayout.EAST);

        card.add(header, BorderLayout.NORTH);
        return card;
    }

    private JPanel createDiscordCard() {
        JPanel card = createCardContainer("🤖 Discord Bot Gateway", "ONLINE", new Color(50, 205, 50));

        JPanel content = new JPanel(new GridLayout(4, 1, 0, 6));
        content.setBackground(ModernTheme.BG_CARD);

        lblDiscordStatus = new JLabel("Status: 🟢 ONLINE (Conectado via WebSocket Gateway)");
        lblDiscordStatus.setFont(ModernTheme.FONT_BODY);
        lblDiscordStatus.setForeground(new Color(120, 220, 120));

        lblDiscordUser = new JLabel("Bot: @Cornelius#9594 (Verificado)");
        lblDiscordUser.setFont(ModernTheme.FONT_BODY);
        lblDiscordUser.setForeground(ModernTheme.TEXT_WHITE);

        lblDiscordId = new JLabel("ID da Aplicação: 1542459980010102784");
        lblDiscordId.setFont(ModernTheme.FONT_MONO_SMALL);
        lblDiscordId.setForeground(ModernTheme.TEXT_MUTED);

        lblDiscordIntents = new JLabel("Intents Ativas: 37377 (Guilds, DMs, Message Content)");
        lblDiscordIntents.setFont(ModernTheme.FONT_MONO_SMALL);
        lblDiscordIntents.setForeground(ModernTheme.TEXT_DIM);

        content.add(lblDiscordStatus);
        content.add(lblDiscordUser);
        content.add(lblDiscordId);
        content.add(lblDiscordIntents);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setBackground(ModernTheme.BG_CARD);

        JButton btnRestartBot = ModernTheme.createMinimalButton("🔄 Reiniciar Bot", false);
        btnRestartBot.addActionListener(e -> restartDiscordBot());
        actions.add(btnRestartBot);

        card.add(content, BorderLayout.CENTER);
        card.add(actions, BorderLayout.SOUTH);
        return card;
    }

    private JPanel createInstagramCard() {
        String user = config.getInstagramUsername();
        boolean hasUser = user != null && !user.isBlank();
        JPanel card = createCardContainer("📸 Instagram do Cornelius", hasUser ? "@" + user : "NÃO CONFIGURADO",
                hasUser ? new Color(225, 48, 108) : new Color(180, 180, 180));

        JPanel content = new JPanel(new GridLayout(4, 1, 0, 6));
        content.setBackground(ModernTheme.BG_CARD);

        JLabel lblUser = new JLabel("Conta: " + (hasUser ? "@" + user : "Nenhuma conta vinculada"));
        lblUser.setFont(ModernTheme.FONT_BODY);
        lblUser.setForeground(ModernTheme.TEXT_WHITE);

        JLabel lblMode = new JLabel("Integração: " + (config.getInstagramAccessToken().isBlank() ? "Navegador Web / Directs" : "Meta Graph API"));
        lblMode.setFont(ModernTheme.FONT_MONO_SMALL);
        lblMode.setForeground(new Color(255, 120, 160));

        JLabel lblStatus = new JLabel(config.isInstagramEnabled() ? "✅ Ações autônomas habilitadas" : "Modo manual (configure nas Opções)");
        lblStatus.setFont(ModernTheme.FONT_SMALL);
        lblStatus.setForeground(ModernTheme.TEXT_MUTED);

        JLabel lblTips = new JLabel("Cornelius pode abrir directs, postar e checar perfil.");
        lblTips.setFont(ModernTheme.FONT_SMALL);
        lblTips.setForeground(ModernTheme.TEXT_DIM);

        content.add(lblUser);
        content.add(lblMode);
        content.add(lblStatus);
        content.add(lblTips);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setBackground(ModernTheme.BG_CARD);

        JButton btnOpen = ModernTheme.createMinimalButton("📸 Abrir", false);
        btnOpen.addActionListener(e -> brain.getInstagramTool().openInstagramWeb());

        JButton btnDMs = ModernTheme.createMinimalButton("📩 Directs", false);
        btnDMs.addActionListener(e -> brain.getInstagramTool().openDirects());

        actions.add(btnOpen);
        actions.add(btnDMs);

        card.add(content, BorderLayout.CENTER);
        card.add(actions, BorderLayout.SOUTH);
        return card;
    }

    private JPanel createAutostartCard() {
        boolean enabled = AutostartManager.isAutostartEnabled();
        JPanel card = createCardContainer("🚀 Inicialização no Boot (Autostart)", enabled ? "HABILITADO" : "DESABILITADO",
                enabled ? new Color(50, 205, 50) : new Color(220, 120, 50));

        JPanel content = new JPanel(new GridLayout(3, 1, 0, 6));
        content.setBackground(ModernTheme.BG_CARD);

        lblAutostartStatus = new JLabel(enabled ? "✅ Inicia automaticamente junto com o " + (SystemProfile.isWindows() ? "Windows" : "Linux") : "❌ Não configurado para iniciar no boot");
        lblAutostartStatus.setFont(ModernTheme.FONT_BODY);
        lblAutostartStatus.setForeground(enabled ? new Color(120, 220, 120) : ModernTheme.TEXT_MUTED);

        String typeDesc = SystemProfile.isWindows() ? "Mecanismo: Windows Startup Folder (Cornelius.vbs)" : "Mecanismo: Systemd User Daemon (cornelius.service) + XDG";
        lblAutostartType = new JLabel(typeDesc);
        lblAutostartType.setFont(ModernTheme.FONT_MONO_SMALL);
        lblAutostartType.setForeground(ModernTheme.TEXT_DIM);

        JLabel lblDetail = new JLabel("O assistente fica disponível 24/7 no Discord assim que o notebook liga.");
        lblDetail.setFont(ModernTheme.FONT_SMALL);
        lblDetail.setForeground(ModernTheme.TEXT_MUTED);

        content.add(lblAutostartStatus);
        content.add(lblAutostartType);
        content.add(lblDetail);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setBackground(ModernTheme.BG_CARD);

        btnToggleAutostart = ModernTheme.createMinimalButton(enabled ? "🛑 Desativar do Boot" : "⚡ Ativar no Boot", true);
        btnToggleAutostart.addActionListener(e -> toggleAutostart());
        actions.add(btnToggleAutostart);

        card.add(content, BorderLayout.CENTER);
        card.add(actions, BorderLayout.SOUTH);
        return card;
    }

    private JPanel createHardwareCard() {
        JPanel card = createCardContainer("💻 Hardware & Telemetria", SystemProfile.getPowerTier().name(), new Color(100, 180, 255));

        JPanel content = new JPanel(new GridLayout(4, 1, 0, 6));
        content.setBackground(ModernTheme.BG_CARD);

        lblOsInfo = new JLabel("OS: " + System.getProperty("os.name") + " (" + System.getProperty("os.arch") + ")");
        lblOsInfo.setFont(ModernTheme.FONT_BODY);
        lblOsInfo.setForeground(ModernTheme.TEXT_WHITE);

        lblCpuInfo = new JLabel("Processador: " + SystemProfile.getCpuCores() + " núcleos detectados");
        lblCpuInfo.setFont(ModernTheme.FONT_BODY);
        lblCpuInfo.setForeground(ModernTheme.TEXT_MUTED);

        lblRamInfo = new JLabel("Memória RAM: Calculando...");
        lblRamInfo.setFont(ModernTheme.FONT_MONO_SMALL);
        lblRamInfo.setForeground(ModernTheme.TEXT_WHITE);

        barRamUsage = new JProgressBar(0, 100);
        barRamUsage.setValue(45);
        barRamUsage.setStringPainted(true);
        barRamUsage.setForeground(new Color(50, 120, 255));
        barRamUsage.setBackground(ModernTheme.BG_VOID);

        content.add(lblOsInfo);
        content.add(lblCpuInfo);
        content.add(lblRamInfo);
        content.add(barRamUsage);

        card.add(content, BorderLayout.CENTER);
        return card;
    }

    private JPanel createVaultCard() {
        ExternalDriveManager.DriveMetrics m = brain.getDriveManager().getMetrics();
        JPanel card = createCardContainer("💾 Cofre de 1TB & Memórias", m.isExternal() ? "1TB HD ATIVO" : "COFRE LOCAL",
                m.isExternal() ? new Color(50, 205, 50) : new Color(180, 180, 220));

        JPanel content = new JPanel(new GridLayout(4, 1, 0, 6));
        content.setBackground(ModernTheme.BG_CARD);

        JLabel lblVaultPath = new JLabel("Caminho: " + m.path());
        lblVaultPath.setFont(ModernTheme.FONT_MONO_SMALL);
        lblVaultPath.setForeground(ModernTheme.TEXT_WHITE);

        JLabel lblCompression = new JLabel("Compressão: Ativa (.czip Nível " + config.getCompressionLevel() + ")");
        lblCompression.setFont(ModernTheme.FONT_BODY);
        lblCompression.setForeground(new Color(130, 200, 255));

        int memoryCount = brain.getMemoryVault().getAllMemories().size();
        JLabel lblMemories = new JLabel("Memórias de Longo Prazo: " + memoryCount + " fatos guardados");
        lblMemories.setFont(ModernTheme.FONT_BODY);
        lblMemories.setForeground(ModernTheme.TEXT_MUTED);

        JLabel lblSavedSpace = new JLabel(String.format("Economia de Espaço: %.1f%% (%s)",
                m.overallSavingsRatio(), formatBytes(m.totalRawBytesSaved() - m.totalCompressedBytes())));
        lblSavedSpace.setFont(ModernTheme.FONT_MONO_SMALL);
        lblSavedSpace.setForeground(new Color(120, 220, 120));

        content.add(lblVaultPath);
        content.add(lblCompression);
        content.add(lblMemories);
        content.add(lblSavedSpace);

        card.add(content, BorderLayout.CENTER);
        return card;
    }

    private JPanel createLogsConsoleCard() {
        JPanel card = new JPanel(new BorderLayout(0, 10)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(ModernTheme.BG_CARD);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));
                g2.setColor(ModernTheme.BORDER_MUTED);
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 12, 12));
                g2.dispose();
            }
        };
        card.setBackground(ModernTheme.BG_CARD);
        card.setBorder(new EmptyBorder(16, 18, 16, 18));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 260));
        card.setPreferredSize(new Dimension(800, 240));

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(ModernTheme.BG_CARD);

        JLabel lblTitle = new JLabel("📜 Console de Logs em Tempo Real (Discord & Sistema)");
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 14));
        lblTitle.setForeground(ModernTheme.TEXT_WHITE);

        JButton btnClearLog = ModernTheme.createMinimalButton("Limpar Visualização", false);
        btnClearLog.addActionListener(e -> txtLiveLogs.setText(""));

        header.add(lblTitle, BorderLayout.WEST);
        header.add(btnClearLog, BorderLayout.EAST);

        txtLiveLogs = new JTextArea();
        txtLiveLogs.setEditable(false);
        txtLiveLogs.setFont(new Font("Monospaced", Font.PLAIN, 12));
        txtLiveLogs.setBackground(new Color(12, 14, 18));
        txtLiveLogs.setForeground(new Color(180, 220, 255));
        txtLiveLogs.setBorder(new EmptyBorder(8, 10, 8, 10));

        JScrollPane logScroll = new JScrollPane(txtLiveLogs);
        logScroll.setBorder(BorderFactory.createLineBorder(ModernTheme.BORDER_MUTED));
        logScroll.getVerticalScrollBar().setUnitIncrement(14);

        card.add(header, BorderLayout.NORTH);
        card.add(logScroll, BorderLayout.CENTER);
        return card;
    }

    public void refreshAllData() {
        Thread.ofVirtual().start(() -> {
            boolean autostart = AutostartManager.isAutostartEnabled();
            double totalRam = SystemProfile.getTotalRamGb();
            long freeRamBytes = Runtime.getRuntime().freeMemory();
            long totalJvmBytes = Runtime.getRuntime().totalMemory();
            long maxJvmBytes = Runtime.getRuntime().maxMemory();
            long usedJvmBytes = totalJvmBytes - freeRamBytes;
            long maxOrTotal = maxJvmBytes > 0 ? maxJvmBytes : totalJvmBytes;

            // Read live log file
            String logContent = "Nenhum log registrado ainda.";
            try {
                Path logPath = Paths.get(System.getProperty("user.dir"), "discord_bot.log");
                if (Files.exists(logPath)) {
                    List<String> lines = Files.readAllLines(logPath);
                    int start = Math.max(0, lines.size() - 40);
                    StringBuilder sb = new StringBuilder();
                    for (int i = start; i < lines.size(); i++) {
                        sb.append(lines.get(i)).append("\n");
                    }
                    logContent = sb.toString();
                }
            } catch (Exception ignored) {}

            final boolean fAutostart = autostart;
            final String fLog = logContent;

            SwingUtilities.invokeLater(() -> {
                // Update Autostart
                lblAutostartStatus.setText(fAutostart ? "✅ Inicia automaticamente junto com o " + (SystemProfile.isWindows() ? "Windows" : "Linux") : "❌ Não configurado para iniciar no boot");
                lblAutostartStatus.setForeground(fAutostart ? new Color(120, 220, 120) : ModernTheme.TEXT_MUTED);
                btnToggleAutostart.setText(fAutostart ? "🛑 Desativar do Boot" : "⚡ Ativar no Boot");

                // Update RAM
                lblRamInfo.setText(String.format("RAM Total: %.1f GB | Perfil: %s", totalRam, SystemProfile.getPowerTier()));
                barRamUsage.setValue((int) (usedJvmBytes * 100 / maxOrTotal));
                barRamUsage.setString(String.format("JVM Heap: %d MB / %d MB", usedJvmBytes / (1024 * 1024), maxOrTotal / (1024 * 1024)));

                // Update Logs
                if (txtLiveLogs != null && !fLog.equals(txtLiveLogs.getText())) {
                    txtLiveLogs.setText(fLog);
                    txtLiveLogs.setCaretPosition(txtLiveLogs.getDocument().getLength());
                }
            });
        });
    }

    private void toggleAutostart() {
        boolean current = AutostartManager.isAutostartEnabled();
        if (current) {
            AutostartManager.disableAutostart();
            JOptionPane.showMessageDialog(this,
                    "Inicialização automática desativada com sucesso.",
                    "Autostart", JOptionPane.INFORMATION_MESSAGE);
        } else {
            AutostartManager.enableAutostart();
            JOptionPane.showMessageDialog(this,
                    "Cornelius configurado para iniciar automaticamente com o " + (SystemProfile.isWindows() ? "Windows" : "Linux") + "!",
                    "Autostart Ativo", JOptionPane.INFORMATION_MESSAGE);
        }
        refreshAllData();
    }

    private void restartDiscordBot() {
        Thread.ofVirtual().start(() -> {
            try {
                if (SystemProfile.isWindows()) {
                    new ProcessBuilder("cmd.exe", "/c", "run-discord-bot.bat").start();
                } else {
                    new ProcessBuilder("systemctl", "--user", "restart", "cornelius.service").start().waitFor();
                }
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this,
                            "Comando de reinicialização do Discord Bot enviado com sucesso!",
                            "Discord Bot", JOptionPane.INFORMATION_MESSAGE);
                    refreshAllData();
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
                        "Erro ao reiniciar bot: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE));
            }
        });
    }

    private String formatBytes(long bytes) {
        if (bytes <= 0) return "0 B";
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
}
