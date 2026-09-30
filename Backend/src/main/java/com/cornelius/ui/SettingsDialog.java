package com.cornelius.ui;

import com.cornelius.config.CorneliusConfig;
import java.awt.*;
import java.net.URI;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class SettingsDialog extends JDialog {
    private final CorneliusConfig config;
    private final Runnable onSaveCallback;

    private final JTextField txtGeminiKey;
    private final JComboBox<String> cbGeminiModel;
    private final JTextField txtOllamaUrl;
    private final JTextField txtOllamaModel;
    private final JComboBox<String> cbProvider;
    private final JTextField txtDrivePath;
    private final JSlider sliderCompression;
    private final JTextField txtUserName;
    private final JComboBox<String> cbTone;
    private final JTextField txtPort;
    private final JTextField txtDiscordToken;
    private final JTextField txtInstagramUser;
    private final JTextField txtInstagramToken;
    private final JTextField txtInstagramSession;
    private final JCheckBox chkInstagramEnabled;

    public SettingsDialog(Frame owner, CorneliusConfig config, Runnable onSaveCallback) {
        super(owner, "SYSTEM SETTINGS // CORNELIUS", true);
        this.config = config;
        this.onSaveCallback = onSaveCallback;

        setSize(650, 700);
        setLocationRelativeTo(owner);
        getContentPane().setBackground(ModernTheme.BG_DARK);
        setLayout(new BorderLayout());

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(ModernTheme.BG_DARK);
        mainPanel.setBorder(new EmptyBorder(24, 28, 24, 28));

        // 1. Intelligence Engine
        mainPanel.add(createSectionLabel("// ARTIFICIAL INTELLIGENCE ENGINE"));
        mainPanel.add(Box.createVerticalStrut(8));

        cbProvider = new JComboBox<>(new String[]{"AUTO", "GEMINI", "OLLAMA"});
        cbProvider.setSelectedItem(config.getActiveProvider());
        styleComboBox(cbProvider);
        mainPanel.add(createFieldRow("Active Provider Mode:", cbProvider));

        txtGeminiKey = ModernTheme.createMinimalTextField(config.getGeminiApiKey());
        txtGeminiKey.setText(config.getGeminiApiKey());
        mainPanel.add(createFieldRow("Google Gemini API Key:", txtGeminiKey));

        cbGeminiModel = new JComboBox<>(new String[]{"gemini-3.6-flash", "gemini-2.5-flash", "gemini-1.5-flash"});
        cbGeminiModel.setSelectedItem(config.getGeminiModel());
        styleComboBox(cbGeminiModel);
        mainPanel.add(createFieldRow("Gemini Model:", cbGeminiModel));

        txtOllamaUrl = ModernTheme.createMinimalTextField(config.getOllamaUrl());
        txtOllamaUrl.setText(config.getOllamaUrl());
        mainPanel.add(createFieldRow("Ollama Local Endpoint:", txtOllamaUrl));

        txtOllamaModel = ModernTheme.createMinimalTextField(config.getOllamaModel());
        txtOllamaModel.setText(config.getOllamaModel());
        mainPanel.add(createFieldRow("Ollama Model:", txtOllamaModel));

        mainPanel.add(Box.createVerticalStrut(16));

        // 2. 1TB Vault & Compression
        mainPanel.add(createSectionLabel("// 1TB VAULT & COMPRESSION"));
        mainPanel.add(Box.createVerticalStrut(8));

        JPanel driveRow = new JPanel(new BorderLayout(8, 0));
        driveRow.setBackground(ModernTheme.BG_DARK);
        txtDrivePath = ModernTheme.createMinimalTextField(config.getExternalDrivePath());
        txtDrivePath.setText(config.getExternalDrivePath());

        JButton btnBrowse = ModernTheme.createMinimalButton("Browse...", false);
        btnBrowse.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setDialogTitle("Select 1TB External HD Mount Point");
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                txtDrivePath.setText(chooser.getSelectedFile().getAbsolutePath() + "/Cornelius_Vault");
            }
        });

        driveRow.add(txtDrivePath, BorderLayout.CENTER);
        driveRow.add(btnBrowse, BorderLayout.EAST);
        mainPanel.add(createFieldRow("External HD Mount Path:", driveRow));

        sliderCompression = new JSlider(1, 9, config.getCompressionLevel());
        sliderCompression.setBackground(ModernTheme.BG_DARK);
        sliderCompression.setForeground(ModernTheme.TEXT_MUTED);
        sliderCompression.setMajorTickSpacing(1);
        sliderCompression.setPaintTicks(true);
        sliderCompression.setPaintLabels(true);
        mainPanel.add(createFieldRow("Compression Level (1=Fast, 9=Max Space Savings):", sliderCompression));

        mainPanel.add(Box.createVerticalStrut(16));

        // 3. Butler Persona
        mainPanel.add(createSectionLabel("// BUTLER PERSONA & BEHAVIOR"));
        mainPanel.add(Box.createVerticalStrut(8));

        txtUserName = ModernTheme.createMinimalTextField(config.getUserName());
        txtUserName.setText(config.getUserName());
        mainPanel.add(createFieldRow("Address User As:", txtUserName));

        cbTone = new JComboBox<>(new String[]{"FORMAL", "WITTY", "TECHNICAL"});
        cbTone.setSelectedItem(config.getPersonaTone());
        styleComboBox(cbTone);
        mainPanel.add(createFieldRow("Response Tone:", cbTone));

        txtPort = ModernTheme.createMinimalTextField(String.valueOf(config.getServerPort()));
        txtPort.setText(String.valueOf(config.getServerPort()));
        mainPanel.add(createFieldRow("Local REST Server Port:", txtPort));

        mainPanel.add(Box.createVerticalStrut(16));

        // 4. Remote & Mobile (Discord Bot)
        mainPanel.add(createSectionLabel("// REMOTE ACCESS // DISCORD BOT (CHAT PELO CELULAR)"));
        mainPanel.add(Box.createVerticalStrut(8));

        txtDiscordToken = ModernTheme.createMinimalTextField(config.getDiscordToken());
        txtDiscordToken.setText(config.getDiscordToken());
        mainPanel.add(createFieldRow("Discord Bot Token:", txtDiscordToken));

        mainPanel.add(Box.createVerticalStrut(16));

        // 5. Instagram Integration
        mainPanel.add(createSectionLabel("// REDE SOCIAL // INSTAGRAM DO CORNELIUS"));
        mainPanel.add(Box.createVerticalStrut(8));

        txtInstagramUser = ModernTheme.createMinimalTextField(config.getInstagramUsername());
        txtInstagramUser.setText(config.getInstagramUsername());
        mainPanel.add(createFieldRow("Usuário / @ do Instagram:", txtInstagramUser));

        txtInstagramToken = ModernTheme.createMinimalTextField(config.getInstagramAccessToken());
        txtInstagramToken.setText(config.getInstagramAccessToken());
        mainPanel.add(createFieldRow("Meta Graph API Access Token (Opcional):", txtInstagramToken));

        txtInstagramSession = ModernTheme.createMinimalTextField(config.getInstagramSessionId());
        txtInstagramSession.setText(config.getInstagramSessionId());
        mainPanel.add(createFieldRow("Cookie de Sessão (sessionid - Opcional):", txtInstagramSession));

        chkInstagramEnabled = new JCheckBox("Habilitar Ações Autônomas no Instagram", config.isInstagramEnabled());
        chkInstagramEnabled.setBackground(ModernTheme.BG_DARK);
        chkInstagramEnabled.setForeground(ModernTheme.TEXT_WHITE);
        chkInstagramEnabled.setFont(ModernTheme.FONT_BODY);
        mainPanel.add(chkInstagramEnabled);

        mainPanel.add(Box.createVerticalStrut(8));

        JButton btnTestInsta = ModernTheme.createMinimalButton("📸 Abrir / Testar Instagram", false);
        btnTestInsta.addActionListener(e -> {
            String u = txtInstagramUser.getText().trim().replace("@", "");
            String url = u.isEmpty() ? "https://www.instagram.com/" : "https://www.instagram.com/" + u + "/";
            try {
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(URI.create(url));
                } else {
                    new ProcessBuilder("xdg-open", url).start();
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Acesse: " + url, "Instagram", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        mainPanel.add(btnTestInsta);

        JScrollPane scrollPane = new JScrollPane(mainPanel);
        scrollPane.setBorder(null);
        ModernTheme.styleScrollBar(scrollPane);
        add(scrollPane, BorderLayout.CENTER);

        // Bottom Actions
        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 16));
        bottomBar.setBackground(ModernTheme.BG_SURFACE);
        bottomBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, ModernTheme.BORDER_MUTED));

        JButton btnCancel = ModernTheme.createMinimalButton("Cancel", false);
        btnCancel.addActionListener(e -> dispose());

        JButton btnSave = ModernTheme.createMinimalButton("Save Settings", true);
        btnSave.addActionListener(e -> saveAndClose());

        bottomBar.add(btnCancel);
        bottomBar.add(btnSave);
        add(bottomBar, BorderLayout.SOUTH);
    }

    private JLabel createSectionLabel(String title) {
        JLabel lbl = new JLabel(title);
        lbl.setFont(ModernTheme.FONT_MONO_SMALL);
        lbl.setForeground(ModernTheme.TEXT_WHITE);
        return lbl;
    }

    private JPanel createFieldRow(String labelText, JComponent component) {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setBackground(ModernTheme.BG_DARK);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 65));

        JLabel label = new JLabel(labelText);
        label.setFont(ModernTheme.FONT_SMALL);
        label.setForeground(ModernTheme.TEXT_MUTED);

        panel.add(label, BorderLayout.NORTH);
        panel.add(component, BorderLayout.CENTER);
        panel.setBorder(new EmptyBorder(4, 0, 4, 0));
        return panel;
    }

    private void styleComboBox(JComboBox<?> box) {
        box.setBackground(ModernTheme.BG_INPUT);
        box.setForeground(ModernTheme.TEXT_WHITE);
        box.setFont(ModernTheme.FONT_BODY);
        box.setBorder(BorderFactory.createLineBorder(ModernTheme.BORDER_MUTED, 1));
    }

    private void saveAndClose() {
        config.setActiveProvider((String) cbProvider.getSelectedItem());
        config.setGeminiApiKey(txtGeminiKey.getText().trim());
        config.setGeminiModel((String) cbGeminiModel.getSelectedItem());
        config.setOllamaUrl(txtOllamaUrl.getText().trim());
        config.setOllamaModel(txtOllamaModel.getText().trim());
        config.setExternalDrivePath(txtDrivePath.getText().trim());
        config.setCompressionLevel(sliderCompression.getValue());
        config.setUserName(txtUserName.getText().trim());
        config.setPersonaTone((String) cbTone.getSelectedItem());
        config.setDiscordToken(txtDiscordToken.getText().trim());
        config.setInstagramUsername(txtInstagramUser.getText().trim());
        config.setInstagramAccessToken(txtInstagramToken.getText().trim());
        config.setInstagramSessionId(txtInstagramSession.getText().trim());
        config.setInstagramEnabled(chkInstagramEnabled.isSelected());

        try {
            config.setServerPort(Integer.parseInt(txtPort.getText().trim()));
        } catch (NumberFormatException ignored) {}

        config.save();

        if (onSaveCallback != null) {
            onSaveCallback.run();
        }

        dispose();
    }
}
