package com.cornelius.ui;

import com.cornelius.brain.CorneliusBrain;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class ChatPanel extends JPanel {
    private final CorneliusBrain brain;
    private final JPanel messagesContainer;
    private final JPanel centerWrapper;
    private final JScrollPane scrollPane;
    private final JTextField inputField;
    private final JButton sendButton;
    private final JLabel statusLabel;
    private final ModernTheme.PulsingDot statusDot;

    private static final int MAX_CHAT_WIDTH = 760;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    public ChatPanel(CorneliusBrain brain) {
        this.brain = brain;
        setLayout(new BorderLayout());
        setBackground(ModernTheme.BG_DARK);

        // Centered scrollable container
        messagesContainer = new JPanel();
        messagesContainer.setLayout(new BoxLayout(messagesContainer, BoxLayout.Y_AXIS));
        messagesContainer.setBackground(ModernTheme.BG_DARK);
        messagesContainer.setBorder(new EmptyBorder(16, 12, 16, 12));

        centerWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        centerWrapper.setBackground(ModernTheme.BG_DARK);
        centerWrapper.add(messagesContainer);

        scrollPane = new JScrollPane(centerWrapper);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        ModernTheme.styleScrollBar(scrollPane);
        add(scrollPane, BorderLayout.CENTER);

        // Dynamically adjust messagesContainer width when window resizes
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                updateContainerBounds();
            }
        });

        // Bottom control area (Centered Antigravity Input Bar)
        JPanel bottomOuter = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        bottomOuter.setBackground(ModernTheme.BG_SURFACE);
        bottomOuter.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, ModernTheme.BORDER_MUTED));

        JPanel bottomPanel = new JPanel(new BorderLayout(0, 8));
        bottomPanel.setBackground(ModernTheme.BG_SURFACE);
        bottomPanel.setBorder(new EmptyBorder(12, 16, 12, 16));
        bottomPanel.setPreferredSize(new Dimension(MAX_CHAT_WIDTH, 125));

        // Input row
        JPanel inputRow = new JPanel(new BorderLayout(8, 0));
        inputRow.setBackground(ModernTheme.BG_SURFACE);

        inputField = ModernTheme.createMinimalTextField("Pergunte ao Cornelius ou digite um comando...");
        inputField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    onSendClicked();
                }
            }
        });

        sendButton = ModernTheme.createMinimalButton("Send", true);
        sendButton.addActionListener(e -> onSendClicked());

        inputRow.add(inputField, BorderLayout.CENTER);
        inputRow.add(sendButton, BorderLayout.EAST);

        // Status row below input
        JPanel statusRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        statusRow.setBackground(ModernTheme.BG_SURFACE);

        statusDot = new ModernTheme.PulsingDot();
        statusLabel = new JLabel("System Ready");
        statusLabel.setFont(ModernTheme.FONT_MONO_SMALL);
        statusLabel.setForeground(ModernTheme.TEXT_MUTED);

        statusRow.add(statusDot);
        statusRow.add(statusLabel);

        // Quick action toolbar (Compact pill buttons)
        JPanel quickActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        quickActions.setBackground(ModernTheme.BG_SURFACE);

        JButton btnWeb = ModernTheme.createMinimalButton("Web Search", false);
        btnWeb.addActionListener(e -> {
            inputField.setText("Pesquise na internet as principais notícias de tecnologia.");
            inputField.requestFocus();
        });

        JButton btnSys = ModernTheme.createMinimalButton("Telemetry", false);
        btnSys.addActionListener(e -> sendMessage("Qual o estado atual do sistema, CPU, RAM e bateria deste notebook?"));

        JButton btnHd = ModernTheme.createMinimalButton("1TB Vault", false);
        btnHd.addActionListener(e -> sendMessage("Qual o status do armazenamento comprimido no meu HD externo?"));

        JButton btnIngest = ModernTheme.createMinimalButton("Ingest", false);
        btnIngest.addActionListener(e -> chooseAndIngestFile());

        JButton btnClear = ModernTheme.createMinimalButton("Clear", false);
        btnClear.addActionListener(e -> clearChat());

        quickActions.add(btnWeb);
        quickActions.add(btnSys);
        quickActions.add(btnHd);
        quickActions.add(btnIngest);
        quickActions.add(btnClear);

        bottomPanel.add(quickActions, BorderLayout.NORTH);

        JPanel southContainer = new JPanel(new BorderLayout(0, 6));
        southContainer.setBackground(ModernTheme.BG_SURFACE);
        southContainer.add(inputRow, BorderLayout.CENTER);
        southContainer.add(statusRow, BorderLayout.SOUTH);

        bottomPanel.add(southContainer, BorderLayout.CENTER);
        bottomOuter.add(bottomPanel);

        add(bottomOuter, BorderLayout.SOUTH);

        // Initial welcome
        addWelcomeMessage();
        updateContainerBounds();
    }

    private void updateContainerBounds() {
        int availWidth = getWidth() > 0 ? getWidth() - 40 : MAX_CHAT_WIDTH;
        int targetWidth = Math.min(MAX_CHAT_WIDTH, Math.max(360, availWidth));
        messagesContainer.setPreferredSize(new Dimension(targetWidth, messagesContainer.getPreferredSize().height));
        messagesContainer.setMaximumSize(new Dimension(targetWidth, Integer.MAX_VALUE));
        messagesContainer.revalidate();
    }

    private void addWelcomeMessage() {
        String welcome = "Saudações, " + brain.getConfig().getUserName() + ". Sou Cornelius, seu mordomo pessoal de IA.\n\n"
                + "Estou ativo localmente neste Pop!_OS e conectado ao seu cofre de armazenamento de 1TB. "
                + "Posso realizar buscas na web em tempo real, monitorar este computador ou gerenciar sua base de conhecimento comprimida.";
        addAssistantBubbleAnimated(welcome, "System", List.of(), 0, false);
    }

    private void onSendClicked() {
        String text = inputField.getText().trim();
        if (text.isEmpty()) return;
        inputField.setText("");
        sendMessage(text);
    }

    public void sendMessage(String userText) {
        addUserBubble(userText);
        setThinkingState(true);

        Thread.ofVirtual().start(() -> {
            try {
                CorneliusBrain.BrainResponse response = brain.processUserMessage(userText);
                SwingUtilities.invokeLater(() -> {
                    setThinkingState(false);
                    addAssistantBubbleAnimated(response.text(), response.providerUsed(), response.toolsExecuted(), response.processingTimeMs(), true);
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    setThinkingState(false);
                    addAssistantBubbleAnimated("Perdão, senhor, ocorreu uma falha na execução: " + e.getMessage(), "Error", List.of(), 0, false);
                });
            }
        });
    }

    private void addUserBubble(String text) {
        int chatW = getChatTargetWidth();
        int maxBubbleW = Math.min(500, (int) (chatW * 0.78));

        JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 2));
        wrapper.setBackground(ModernTheme.BG_DARK);
        wrapper.setMaximumSize(new Dimension(chatW, 600));
        wrapper.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel bubble = new JPanel(new BorderLayout(0, 4));
        bubble.setBackground(ModernTheme.BG_CARD);
        bubble.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ModernTheme.BORDER_LIGHT, 1),
                new EmptyBorder(8, 14, 8, 14)
        ));

        JLabel meta = new JLabel("YOU // " + LocalDateTime.now().format(TIME_FMT));
        meta.setFont(ModernTheme.FONT_MONO_SMALL);
        meta.setForeground(ModernTheme.TEXT_MUTED);

        JTextArea content = new JTextArea(text);
        content.setFont(ModernTheme.FONT_BODY);
        content.setForeground(ModernTheme.TEXT_WHITE);
        content.setBackground(ModernTheme.BG_CARD);
        content.setWrapStyleWord(true);
        content.setLineWrap(true);
        content.setEditable(false);
        content.setColumns(Math.min(38, Math.max(12, text.length())));
        content.setMaximumSize(new Dimension(maxBubbleW, Integer.MAX_VALUE));

        bubble.add(meta, BorderLayout.NORTH);
        bubble.add(content, BorderLayout.CENTER);

        wrapper.add(bubble);
        messagesContainer.add(wrapper);
        messagesContainer.add(Box.createVerticalStrut(10));
        refreshScroll();
    }

    private void addAssistantBubbleAnimated(String fullText, String provider, List<String> tools, long timeMs, boolean animate) {
        int chatW = getChatTargetWidth();
        int maxBubbleW = chatW - 16;

        JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 2));
        wrapper.setBackground(ModernTheme.BG_DARK);
        wrapper.setMaximumSize(new Dimension(chatW, 2000));
        wrapper.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel bubble = new JPanel(new BorderLayout(0, 4));
        bubble.setBackground(ModernTheme.BG_SURFACE);
        bubble.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ModernTheme.BORDER_MUTED, 1),
                new EmptyBorder(10, 14, 10, 14)
        ));
        bubble.setPreferredSize(new Dimension(maxBubbleW, bubble.getPreferredSize().height));
        bubble.setMaximumSize(new Dimension(maxBubbleW, Integer.MAX_VALUE));

        String toolInfo = (tools != null && !tools.isEmpty()) ? " // " + String.join(", ", tools) : "";
        String timeInfo = timeMs > 0 ? " (" + timeMs + "ms)" : "";
        JLabel meta = new JLabel("CORNELIUS // " + provider.toUpperCase() + toolInfo + timeInfo + " // " + LocalDateTime.now().format(TIME_FMT));
        meta.setFont(ModernTheme.FONT_MONO_SMALL);
        meta.setForeground(ModernTheme.TEXT_WHITE);

        JEditorPane content = new JEditorPane();
        content.setContentType("text/html");
        content.setEditable(false);
        content.setBackground(ModernTheme.BG_SURFACE);
        content.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);

        bubble.add(meta, BorderLayout.NORTH);
        bubble.add(content, BorderLayout.CENTER);

        wrapper.add(bubble);
        messagesContainer.add(wrapper);
        messagesContainer.add(Box.createVerticalStrut(10));

        if (!animate) {
            content.setText(wrapHtml(formatToHtml(fullText), maxBubbleW - 30));
            refreshScroll();
            return;
        }

        // Typewriter streaming animation
        final int totalLen = fullText.length();
        final int chunkSize = Math.max(1, totalLen / 30);
        final int[] currentPos = {0};

        Timer typeTimer = new Timer(12, null);
        typeTimer.addActionListener(e -> {
            currentPos[0] = Math.min(totalLen, currentPos[0] + chunkSize);
            String partial = fullText.substring(0, currentPos[0]);
            boolean isDone = currentPos[0] >= totalLen;
            String cursor = isDone ? "" : " <span style='color:#ffffff;'>▌</span>";
            content.setText(wrapHtml(formatToHtml(partial) + cursor, maxBubbleW - 30));
            refreshScroll();
            if (isDone) {
                typeTimer.stop();
            }
        });
        typeTimer.start();
    }

    private int getChatTargetWidth() {
        int availWidth = getWidth() > 0 ? getWidth() - 40 : MAX_CHAT_WIDTH;
        return Math.min(MAX_CHAT_WIDTH, Math.max(360, availWidth));
    }

    private String wrapHtml(String inner, int maxWidth) {
        return "<html><body style='width:" + maxWidth + "px; font-family:SansSerif, sans-serif; font-size:11px; color:#fafafa; margin:0; padding:0; line-height:1.5; word-wrap:break-word;'>"
                + inner + "</body></html>";
    }

    private String formatToHtml(String raw) {
        if (raw == null) return "";
        return raw.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replaceAll("(?m)^### (.*)$", "<h3 style='color:#ffffff; font-size:12px; margin:6px 0 3px 0; border-bottom:1px solid #27272a; padding-bottom:2px;'>$1</h3>")
                .replaceAll("(?m)^## (.*)$", "<h2 style='color:#ffffff; font-size:13px; margin:8px 0 3px 0;'>$1</h2>")
                .replaceAll("(?m)^# (.*)$", "<h1 style='color:#ffffff; font-size:14px; margin:10px 0 4px 0;'>$1</h1>")
                .replaceAll("\\*\\*(.*?)\\*\\*", "<strong style='color:#ffffff;'>$1</strong>")
                .replaceAll("\\*(.*?)\\*", "<em style='color:#a1a1aa;'>$1</em>")
                .replaceAll("`([^`]+)`", "<code style='background:#18181b; border:1px solid #27272a; padding:1px 4px; border-radius:3px; color:#ffffff; font-family:Monospaced;'>$1</code>")
                .replaceAll("(?m)^- (.*)$", "<span style='color:#71717a;'>•</span> $1<br>")
                .replace("\n", "<br>");
    }

    private void chooseAndIngestFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Selecione um Arquivo ou Pasta para o Cofre de 1TB");
        chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        int res = chooser.showOpenDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            File selected = chooser.getSelectedFile();
            sendMessage("Por favor, ingira e comprima no meu HD externo o seguinte arquivo/diretório: " + selected.getAbsolutePath());
        }
    }

    private void clearChat() {
        messagesContainer.removeAll();
        brain.clearHistory();
        addWelcomeMessage();
        refreshScroll();
    }

    private void setThinkingState(boolean thinking) {
        inputField.setEnabled(!thinking);
        sendButton.setEnabled(!thinking);
        if (thinking) {
            statusDot.setDotColor(Color.WHITE);
            statusLabel.setText("Cornelius is processing tools and reasoning...");
            statusLabel.setForeground(ModernTheme.TEXT_WHITE);
        } else {
            statusDot.setDotColor(Color.WHITE);
            statusLabel.setText("System Ready");
            statusLabel.setForeground(ModernTheme.TEXT_MUTED);
            inputField.requestFocus();
        }
    }

    private void refreshScroll() {
        messagesContainer.revalidate();
        messagesContainer.repaint();
        centerWrapper.revalidate();
        centerWrapper.repaint();
        SwingUtilities.invokeLater(() -> {
            JScrollBar v = scrollPane.getVerticalScrollBar();
            v.setValue(v.getMaximum());
        });
    }
}

