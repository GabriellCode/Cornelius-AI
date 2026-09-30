package com.cornelius.ui;

import com.cornelius.brain.CorneliusBrain;
import com.cornelius.config.CorneliusConfig;
import com.cornelius.storage.ExternalDriveManager;
import com.cornelius.storage.MemoryVault;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class CorneliusWindow extends JFrame {
    private final CorneliusBrain brain;
    private final CorneliusConfig config;

    private final CardLayout cardLayout;
    private final JPanel contentCards;

    private final ChatPanel chatPanel;
    private final StoragePanel storagePanel;
    private final JPanel memoryPanel;
    private final SystemDashboardPanel systemDashboardPanel;

    private final JLabel lblNetStatus;
    private final JLabel lblDriveStatus;
    private final JLabel lblAiStatus;

    private JButton btnTabChat;
    private JButton btnTabDashboard;
    private JButton btnTabStorage;
    private JButton btnTabMemories;

    private JPanel liveUpdateBanner;
    private JLabel lblLiveUpdateText;
    private com.cornelius.system.LiveUpdateService liveUpdateService;

    public CorneliusWindow(CorneliusBrain brain) {
        this.brain = brain;
        this.config = brain.getConfig();

        setTitle("Cornelius.AI — Autonomous Personal Butler");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1120, 800);
        setMinimumSize(new Dimension(920, 680));
        setLocationRelativeTo(null);
        getContentPane().setBackground(ModernTheme.BG_DARK);
        setLayout(new BorderLayout());

        // Set Window Icon
        try {
            File iconFile = new File("cornelius.png");
            if (iconFile.exists()) {
                setIconImage(new ImageIcon(iconFile.getAbsolutePath()).getImage());
            }
        } catch (Exception ignored) {}

        // 1. Sleek Minimalist Header Bar
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(ModernTheme.BG_VOID);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, ModernTheme.BORDER_MUTED),
                new EmptyBorder(14, 24, 14, 24)
        ));

        // Brand + Official Logo Image + Pulsing Live Dot
        JPanel brandPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        brandPanel.setBackground(ModernTheme.BG_VOID);

        ModernTheme.PulsingDot liveDot = new ModernTheme.PulsingDot();
        liveDot.setDotColor(Color.WHITE);

        JLabel logoLabel = null;
        try {
            File logoFile = new File("logo.png");
            if (logoFile.exists()) {
                ImageIcon original = new ImageIcon(logoFile.getAbsolutePath());
                Image scaled = original.getImage().getScaledInstance(-1, 28, Image.SCALE_SMOOTH);
                logoLabel = new JLabel(new ImageIcon(scaled));
            }
        } catch (Exception ignored) {}

        JPanel brandText = new JPanel(new GridLayout(2, 1, 0, 2));
        brandText.setBackground(ModernTheme.BG_VOID);

        JLabel title = new JLabel("Cornelius.AI");
        title.setFont(new Font("SansSerif", Font.BOLD, 17));
        title.setForeground(ModernTheme.TEXT_WHITE);

        JLabel sub = new JLabel("AUTONOMOUS PERSONAL BUTLER // POP!_OS");
        sub.setFont(ModernTheme.FONT_MONO_SMALL);
        sub.setForeground(ModernTheme.TEXT_DIM);

        brandText.add(title);
        brandText.add(sub);
        brandPanel.add(liveDot);
        if (logoLabel != null) {
            brandPanel.add(logoLabel);
        } else {
            brandPanel.add(brandText);
        }

        // Status Badges & Settings
        JPanel rightHeader = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightHeader.setBackground(ModernTheme.BG_VOID);

        lblNetStatus = createMonochromePill("NET: OK");
        lblDriveStatus = createMonochromePill("1TB VAULT: ACTIVE");
        lblAiStatus = createMonochromePill("MODEL: " + config.getActiveProvider());

        JButton btnOllama = ModernTheme.createMinimalButton("⚡ Start Ollama", false);
        btnOllama.addActionListener(e -> startOrCheckOllama());

        JButton btnSettings = ModernTheme.createMinimalButton("Settings", false);
        btnSettings.addActionListener(e -> openSettings());

        rightHeader.add(lblNetStatus);
        rightHeader.add(lblDriveStatus);
        rightHeader.add(lblAiStatus);
        rightHeader.add(btnOllama);
        rightHeader.add(btnSettings);

        header.add(brandPanel, BorderLayout.WEST);
        header.add(rightHeader, BorderLayout.EAST);

        // 3. Main Center Content (CardLayout)
        cardLayout = new CardLayout();
        contentCards = new JPanel(cardLayout);
        contentCards.setBackground(ModernTheme.BG_DARK);

        chatPanel = new ChatPanel(brain);
        systemDashboardPanel = new SystemDashboardPanel(brain);
        storagePanel = new StoragePanel(brain);
        memoryPanel = createMemoryPanel();

        contentCards.add(chatPanel, "CHAT");
        contentCards.add(systemDashboardPanel, "DASHBOARD");
        contentCards.add(storagePanel, "STORAGE");
        contentCards.add(memoryPanel, "MEMORIES");

        // 2. Custom Sleek Tab Navigation Bar (Antigravity Style)
        JPanel navBar = new JPanel(new BorderLayout());
        navBar.setBackground(ModernTheme.BG_SURFACE);
        navBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, ModernTheme.BORDER_MUTED));

        JPanel navTabs = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 8));
        navTabs.setBackground(ModernTheme.BG_SURFACE);
        navTabs.setBorder(new EmptyBorder(0, 18, 0, 18));

        btnTabChat = createNavTabButton("Chat Butler", true);
        btnTabDashboard = createNavTabButton("🤖 Discord & Sistema", false);
        btnTabStorage = createNavTabButton("1TB Storage & Vault", false);
        btnTabMemories = createNavTabButton("Long-Term Memory", false);

        btnTabChat.addActionListener(e -> selectTab("CHAT", btnTabChat));
        btnTabDashboard.addActionListener(e -> {
            systemDashboardPanel.refreshAllData();
            selectTab("DASHBOARD", btnTabDashboard);
        });
        btnTabStorage.addActionListener(e -> {
            storagePanel.refreshMetrics();
            selectTab("STORAGE", btnTabStorage);
        });
        btnTabMemories.addActionListener(e -> {
            refreshMemoryList();
            selectTab("MEMORIES", btnTabMemories);
        });

        navTabs.add(btnTabChat);
        navTabs.add(btnTabDashboard);
        navTabs.add(btnTabStorage);
        navTabs.add(btnTabMemories);

        navBar.add(navTabs, BorderLayout.WEST);

        // Live Update Real-Time Notification Banner
        liveUpdateBanner = new JPanel(new BorderLayout(14, 0));
        liveUpdateBanner.setBackground(new Color(20, 26, 38));
        liveUpdateBanner.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(50, 120, 255)),
                new EmptyBorder(8, 24, 8, 24)
        ));
        liveUpdateBanner.setVisible(false);

        lblLiveUpdateText = new JLabel("⚡ Alteração detectada em tempo real.");
        lblLiveUpdateText.setFont(ModernTheme.FONT_SUBTITLE);
        lblLiveUpdateText.setForeground(new Color(130, 200, 255));

        JPanel bannerActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        bannerActions.setBackground(new Color(20, 26, 38));

        JButton btnReload = ModernTheme.createMinimalButton("🔄 Sincronizar e Atualizar", true);
        btnReload.addActionListener(e -> hotReloadSystem());

        JButton btnDismiss = ModernTheme.createMinimalButton("✕", false);
        btnDismiss.addActionListener(e -> liveUpdateBanner.setVisible(false));

        bannerActions.add(btnReload);
        bannerActions.add(btnDismiss);

        liveUpdateBanner.add(lblLiveUpdateText, BorderLayout.CENTER);
        liveUpdateBanner.add(bannerActions, BorderLayout.EAST);

        // Header Container
        JPanel topContainer = new JPanel(new BorderLayout());
        topContainer.add(header, BorderLayout.NORTH);
        topContainer.add(liveUpdateBanner, BorderLayout.CENTER);
        topContainer.add(navBar, BorderLayout.SOUTH);
        add(topContainer, BorderLayout.NORTH);

        add(contentCards, BorderLayout.CENTER);

        // 4. Minimalist Footer Bar
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(ModernTheme.BG_VOID);
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, ModernTheme.BORDER_MUTED),
                new EmptyBorder(8, 24, 8, 24)
        ));

        JLabel lblOs = new JLabel(com.cornelius.system.SystemProfile.getOsType() + " // " +
                com.cornelius.system.SystemProfile.getPowerTier() + " // Java 21 LTS");
        lblOs.setFont(ModernTheme.FONT_MONO_SMALL);
        lblOs.setForeground(ModernTheme.TEXT_DIM);

        JLabel lblApi = new JLabel("Local REST Server // http://localhost:" + config.getServerPort());
        lblApi.setFont(ModernTheme.FONT_MONO_SMALL);
        lblApi.setForeground(ModernTheme.TEXT_DIM);

        footer.add(lblOs, BorderLayout.WEST);
        footer.add(lblApi, BorderLayout.EAST);

        add(footer, BorderLayout.SOUTH);

        // Monitor background status
        startStatusMonitor();

        // Start Live Update File Watcher
        this.liveUpdateService = new com.cornelius.system.LiveUpdateService(brain, config);
        this.liveUpdateService.addListener(event -> {
            SwingUtilities.invokeLater(() -> {
                String text = String.format("⚡ Alteração em tempo real: '%s' [%s]. Clique para sincronizar!",
                        event.filename(), event.changeType());
                lblLiveUpdateText.setText(text);
                liveUpdateBanner.setVisible(true);
                liveUpdateBanner.revalidate();
                liveUpdateBanner.repaint();
            });
        });
        this.liveUpdateService.start();
    }

    private void hotReloadSystem() {
        try {
            config.detectDefaultDrivePath();
            brain.getKnowledgeBase().loadIndex();
            brain.getMemoryVault().loadMemories();
            storagePanel.refreshMetrics();
            lblAiStatus.setText("MODEL: " + config.getActiveProvider());
            lblDriveStatus.setText(brain.getDriveManager().isMountedExternal() ? "1TB HD: MOUNTED" : "VAULT: LOCAL");
            lblLiveUpdateText.setText("✅ Sistema e dados atualizados com sucesso em tempo real!");
            Timer timer = new Timer(2500, e -> liveUpdateBanner.setVisible(false));
            timer.setRepeats(false);
            timer.start();
        } catch (Exception ex) {
            lblLiveUpdateText.setText("⚠️ Erro ao recarregar: " + ex.getMessage());
        }
    }

    private JButton createNavTabButton(String text, boolean active) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean isActive = (this == btnTabChat && "CHAT".equals(currentTab))
                        || (this == btnTabDashboard && "DASHBOARD".equals(currentTab))
                        || (this == btnTabStorage && "STORAGE".equals(currentTab))
                        || (this == btnTabMemories && "MEMORIES".equals(currentTab));

                if (isActive) {
                    g2.setColor(ModernTheme.BG_CARD);
                    g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 6, 6));
                    g2.setColor(ModernTheme.BORDER_LIGHT);
                    g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 6, 6));
                    g2.setColor(ModernTheme.TEXT_WHITE);
                } else {
                    g2.setColor(ModernTheme.TEXT_MUTED);
                }

                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.setFont(getFont());
                g2.drawString(getText(), x, y);
                g2.dispose();
            }
        };

        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setBorder(new EmptyBorder(6, 14, 6, 14));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private String currentTab = "CHAT";

    private void selectTab(String name, JButton activeBtn) {
        this.currentTab = name;
        cardLayout.show(contentCards, name);
        btnTabChat.repaint();
        btnTabDashboard.repaint();
        btnTabStorage.repaint();
        btnTabMemories.repaint();
    }

    private JLabel createMonochromePill(String text) {
        JLabel pill = new JLabel(text);
        pill.setFont(ModernTheme.FONT_MONO_SMALL);
        pill.setForeground(ModernTheme.TEXT_MUTED);
        pill.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ModernTheme.BORDER_MUTED, 1),
                new EmptyBorder(4, 10, 4, 10)
        ));
        return pill;
    }

    private JPanel createMemoryPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setBackground(ModernTheme.BG_DARK);
        panel.setBorder(new EmptyBorder(24, 28, 24, 28));

        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(ModernTheme.BG_DARK);

        JLabel title = new JLabel("LONG-TERM MEMORY VAULT");
        title.setFont(ModernTheme.FONT_TITLE);
        title.setForeground(ModernTheme.TEXT_WHITE);

        JButton btnAddMem = ModernTheme.createMinimalButton("+ Add Memory", true);
        btnAddMem.addActionListener(e -> addNewMemoryDialog());

        top.add(title, BorderLayout.WEST);
        top.add(btnAddMem, BorderLayout.EAST);

        DefaultListModel<String> memListModel = new DefaultListModel<>();
        JList<String> memList = new JList<>(memListModel);
        memList.setBackground(ModernTheme.BG_CARD);
        memList.setForeground(ModernTheme.TEXT_WHITE);
        memList.setFont(ModernTheme.FONT_MONO_SMALL);
        memList.setBorder(new EmptyBorder(8, 12, 8, 12));

        JScrollPane scroll = new JScrollPane(memList);
        ModernTheme.styleScrollBar(scroll);

        panel.add(top, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        panel.putClientProperty("model", memListModel);

        return panel;
    }

    @SuppressWarnings("unchecked")
    private void refreshMemoryList() {
        DefaultListModel<String> model = (DefaultListModel<String>) memoryPanel.getClientProperty("model");
        if (model != null) {
            model.clear();
            for (MemoryVault.MemoryEntry m : brain.getMemoryVault().getAllMemories()) {
                model.addElement(String.format("// [%s] %s", m.category(), m.text()));
            }
        }
    }

    private void addNewMemoryDialog() {
        String text = JOptionPane.showInputDialog(this, "Digite o fato ou instrução para o Cornelius memorizar:", "Add to Long-Term Memory Vault", JOptionPane.PLAIN_MESSAGE);
        if (text != null && !text.isBlank()) {
            brain.getMemoryVault().addMemory("PREFERENCE", text.trim(), 5);
            refreshMemoryList();
        }
    }

    private void openSettings() {
        SettingsDialog dialog = new SettingsDialog(this, config, () -> {
            brain.getDriveManager().initVault();
            storagePanel.refreshMetrics();
            lblAiStatus.setText("MODEL: " + config.getActiveProvider());
        });
        dialog.setVisible(true);
    }

    private void startOrCheckOllama() {
        Thread.ofVirtual().start(() -> {
            String url = config.getOllamaUrl();
            String model = config.getOllamaModel();
            boolean running = com.cornelius.brain.OllamaManager.isRunning(url);

            if (!running) {
                if (!com.cornelius.brain.OllamaManager.isInstalled()) {
                    SwingUtilities.invokeLater(() -> {
                        int opt = JOptionPane.showConfirmDialog(this,
                                "O Ollama não foi encontrado instalado no sistema.\n\nDeseja copiar o comando de instalação oficial para o seu terminal?\n(curl -fsSL https://ollama.com/install.sh | sh)",
                                "Instalar Ollama",
                                JOptionPane.YES_NO_OPTION);
                        if (opt == JOptionPane.YES_OPTION) {
                            try {
                                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                                        new java.awt.datatransfer.StringSelection("curl -fsSL https://ollama.com/install.sh | sh"), null);
                                JOptionPane.showMessageDialog(this, "Comando copiado para a área de transferência!\nAbra o terminal e pressione Ctrl+Shift+V para instalar.");
                            } catch (Exception ignored) {}
                        }
                    });
                    return;
                }

                SwingUtilities.invokeLater(() -> lblAiStatus.setText("OLLAMA: STARTING..."));

                try {
                    com.cornelius.brain.OllamaManager.startDaemon(url, msg -> {
                        SwingUtilities.invokeLater(() -> lblAiStatus.setText("OLLAMA: " + msg));
                    });
                    running = true;
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> {
                        lblAiStatus.setText("MODEL: " + config.getActiveProvider());
                        JOptionPane.showMessageDialog(this,
                                "Não foi possível iniciar o daemon do Ollama: " + ex.getMessage(),
                                "Falha ao Iniciar Ollama",
                                JOptionPane.ERROR_MESSAGE);
                    });
                    return;
                }
            }

            // Check if model is downloaded
            boolean hasModel = com.cornelius.brain.OllamaManager.hasModel(url, model);
            if (!hasModel) {
                SwingUtilities.invokeLater(() -> {
                    int opt = JOptionPane.showConfirmDialog(this,
                            "O serviço do Ollama está rodando, mas o modelo '" + model + "' ainda não foi baixado.\n\nDeseja iniciar o download agora em segundo plano?",
                            "Download do Modelo Local",
                            JOptionPane.YES_NO_OPTION);
                    if (opt == JOptionPane.YES_OPTION) {
                        Thread.ofVirtual().start(() -> {
                            try {
                                SwingUtilities.invokeLater(() -> lblAiStatus.setText("PULLING " + model.toUpperCase() + "..."));
                                com.cornelius.brain.OllamaManager.pullModel(url, model, msg -> {
                                    SwingUtilities.invokeLater(() -> lblAiStatus.setText(msg));
                                });
                                config.setActiveProvider("OLLAMA");
                                config.save();
                                SwingUtilities.invokeLater(() -> {
                                    lblAiStatus.setText("MODEL: OLLAMA");
                                    JOptionPane.showMessageDialog(this, "Modelo '" + model + "' baixado e pronto! Cornelius agora está em modo IA 100% Local.");
                                });
                            } catch (Exception e) {
                                SwingUtilities.invokeLater(() -> {
                                    lblAiStatus.setText("MODEL: " + config.getActiveProvider());
                                    JOptionPane.showMessageDialog(this, "Falha no download do modelo: " + e.getMessage() + "\n(Você pode rodar 'ollama pull " + model + "' no terminal).");
                                });
                            }
                        });
                    }
                });
            } else {
                config.setActiveProvider("OLLAMA");
                config.save();
                SwingUtilities.invokeLater(() -> {
                    lblAiStatus.setText("MODEL: OLLAMA");
                    JOptionPane.showMessageDialog(this,
                            "Ollama conectado com sucesso com o modelo local '" + model + "'!",
                            "Ollama Pronto",
                            JOptionPane.INFORMATION_MESSAGE);
                });
            }
        });
    }

    private void startStatusMonitor() {
        Thread.ofVirtual().start(() -> {
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
            while (true) {
                boolean netOk = false;
                try {
                    var req = HttpRequest.newBuilder(URI.create("https://1.1.1.1")).GET().build();
                    var resp = client.send(req, HttpResponse.BodyHandlers.discarding());
                    netOk = resp.statusCode() == 200 || resp.statusCode() == 301;
                } catch (Exception ignored) {}

                ExternalDriveManager.DriveMetrics m = brain.getDriveManager().getMetrics();
                boolean isExternal = m.isExternal();

                final boolean finalNet = netOk;
                SwingUtilities.invokeLater(() -> {
                    lblNetStatus.setText(finalNet ? "NET: ONLINE" : "NET: OFFLINE");
                    lblNetStatus.setForeground(finalNet ? ModernTheme.TEXT_WHITE : ModernTheme.TEXT_DIM);

                    lblDriveStatus.setText(isExternal ? "1TB HD: MOUNTED" : "VAULT: LOCAL");
                    lblDriveStatus.setForeground(isExternal ? ModernTheme.TEXT_WHITE : ModernTheme.TEXT_MUTED);
                });

                try {
                    Thread.sleep(8000);
                } catch (InterruptedException ignored) {
                    break;
                }
            }
        });
    }
}
