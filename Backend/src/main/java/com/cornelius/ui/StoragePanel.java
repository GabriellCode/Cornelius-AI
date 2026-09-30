package com.cornelius.ui;

import com.cornelius.brain.CorneliusBrain;
import com.cornelius.storage.DocumentIngester;
import com.cornelius.storage.ExternalDriveManager;
import com.cornelius.storage.KnowledgeBase;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class StoragePanel extends JPanel {
    private final CorneliusBrain brain;

    private final JLabel lblDrivePath;
    private final JLabel lblSpace;
    private final JProgressBar progressSpace;
    private final JLabel lblArchivedCount;
    private final JLabel lblRawSize;
    private final JLabel lblCompSize;
    private final JLabel lblSavingsRatio;
    private final DefaultListModel<String> listModel;
    private final JList<String> docList;

    public StoragePanel(CorneliusBrain brain) {
        this.brain = brain;
        setLayout(new BorderLayout(0, 20));
        setBackground(ModernTheme.BG_DARK);
        setBorder(new EmptyBorder(24, 28, 24, 28));

        // Top Header
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(ModernTheme.BG_DARK);

        JLabel title = new JLabel("1TB VAULT & COMPRESSION ENGINE");
        title.setFont(ModernTheme.FONT_TITLE);
        title.setForeground(ModernTheme.TEXT_WHITE);

        JButton btnRefresh = ModernTheme.createMinimalButton("Refresh", false);
        btnRefresh.addActionListener(e -> refreshMetrics());

        topPanel.add(title, BorderLayout.WEST);
        topPanel.add(btnRefresh, BorderLayout.EAST);

        // Center Cards Grid
        JPanel centerPanel = new JPanel(new GridLayout(2, 2, 16, 16));
        centerPanel.setBackground(ModernTheme.BG_DARK);

        // Card 1: Drive Capacity
        JPanel card1 = createCard("DRIVE CAPACITY");
        lblDrivePath = new JLabel("Vault: Loading...");
        lblDrivePath.setFont(ModernTheme.FONT_MONO_SMALL);
        lblDrivePath.setForeground(ModernTheme.TEXT_MUTED);

        lblSpace = new JLabel("0.0 GB / 0.0 GB (0.0%)");
        lblSpace.setFont(ModernTheme.FONT_SUBTITLE);
        lblSpace.setForeground(ModernTheme.TEXT_WHITE);

        progressSpace = new JProgressBar(0, 100);
        progressSpace.setForeground(Color.WHITE);
        progressSpace.setBackground(ModernTheme.BORDER_MUTED);
        progressSpace.setBorderPainted(false);
        progressSpace.setPreferredSize(new Dimension(0, 8));

        card1.add(lblDrivePath);
        card1.add(Box.createVerticalStrut(8));
        card1.add(lblSpace);
        card1.add(Box.createVerticalStrut(10));
        card1.add(progressSpace);

        // Card 2: Compression Metrics
        JPanel card2 = createCard("COMPRESSION EFFICIENCY");
        lblSavingsRatio = new JLabel("Space Savings: 0.0%");
        lblSavingsRatio.setFont(ModernTheme.FONT_SUBTITLE);
        lblSavingsRatio.setForeground(ModernTheme.TEXT_WHITE);

        lblRawSize = new JLabel("Raw Estimated Volume: 0 MB");
        lblRawSize.setFont(ModernTheme.FONT_BODY);
        lblRawSize.setForeground(ModernTheme.TEXT_MUTED);

        lblCompSize = new JLabel("Physical Vault Size: 0 MB");
        lblCompSize.setFont(ModernTheme.FONT_BODY);
        lblCompSize.setForeground(ModernTheme.TEXT_MUTED);

        card2.add(lblSavingsRatio);
        card2.add(Box.createVerticalStrut(8));
        card2.add(lblRawSize);
        card2.add(lblCompSize);

        // Card 3: Ingestion Action
        JPanel card3 = createCard("FEED KNOWLEDGE BASE");
        JLabel ingestDesc = new JLabel("Ingest local files and directories into 1TB compressed vault:");
        ingestDesc.setFont(ModernTheme.FONT_BODY);
        ingestDesc.setForeground(ModernTheme.TEXT_MUTED);

        JButton btnIngest = ModernTheme.createMinimalButton("Ingest & Compress Files", true);
        btnIngest.addActionListener(e -> selectAndIngest());

        card3.add(ingestDesc);
        card3.add(Box.createVerticalStrut(12));
        card3.add(btnIngest);

        // Card 4: Archive Stats
        JPanel card4 = createCard("VAULT METRICS");
        lblArchivedCount = new JLabel("Total Archived Files: 0");
        lblArchivedCount.setFont(ModernTheme.FONT_SUBTITLE);
        lblArchivedCount.setForeground(ModernTheme.TEXT_WHITE);

        JLabel compAlgo = new JLabel("Algorithm: DEFLATE Level " + brain.getConfig().getCompressionLevel() + " (SHA-256 Checksum)");
        compAlgo.setFont(ModernTheme.FONT_BODY);
        compAlgo.setForeground(ModernTheme.TEXT_MUTED);

        card4.add(lblArchivedCount);
        card4.add(Box.createVerticalStrut(8));
        card4.add(compAlgo);

        centerPanel.add(card1);
        centerPanel.add(card2);
        centerPanel.add(card3);
        centerPanel.add(card4);

        // Bottom: Indexed Documents List
        JPanel bottomSection = new JPanel(new BorderLayout(0, 10));
        bottomSection.setBackground(ModernTheme.BG_DARK);

        JLabel lblDocs = new JLabel("ACTIVE KNOWLEDGE CHUNKS (BM25 INDEX)");
        lblDocs.setFont(ModernTheme.FONT_SUBTITLE);
        lblDocs.setForeground(ModernTheme.TEXT_WHITE);

        listModel = new DefaultListModel<>();
        docList = new JList<>(listModel);
        docList.setBackground(ModernTheme.BG_CARD);
        docList.setForeground(ModernTheme.TEXT_WHITE);
        docList.setFont(ModernTheme.FONT_MONO_SMALL);
        docList.setSelectionBackground(ModernTheme.BORDER_LIGHT);
        docList.setBorder(new EmptyBorder(8, 12, 8, 12));

        JScrollPane scrollDocs = new JScrollPane(docList);
        scrollDocs.setPreferredSize(new Dimension(0, 180));
        ModernTheme.styleScrollBar(scrollDocs);

        bottomSection.add(lblDocs, BorderLayout.NORTH);
        bottomSection.add(scrollDocs, BorderLayout.CENTER);

        JPanel contentContainer = new JPanel(new BorderLayout(0, 16));
        contentContainer.setBackground(ModernTheme.BG_DARK);
        contentContainer.add(centerPanel, BorderLayout.NORTH);
        contentContainer.add(bottomSection, BorderLayout.CENTER);

        add(topPanel, BorderLayout.NORTH);
        add(contentContainer, BorderLayout.CENTER);

        refreshMetrics();
    }

    private JPanel createCard(String title) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(ModernTheme.BG_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ModernTheme.BORDER_MUTED, 1),
                new EmptyBorder(16, 18, 16, 18)
        ));

        JLabel lbl = new JLabel(title);
        lbl.setFont(ModernTheme.FONT_MONO_SMALL);
        lbl.setForeground(ModernTheme.TEXT_MUTED);

        card.add(lbl);
        card.add(Box.createVerticalStrut(10));
        return card;
    }

    public void refreshMetrics() {
        ExternalDriveManager.DriveMetrics m = brain.getDriveManager().getMetrics();

        lblDrivePath.setText("Path: " + m.path() + (m.isExternal() ? " [1TB External Mount]" : " [Local Vault]"));
        lblSpace.setText(String.format("%.1f GB used / %.1f GB total (%.1f%%)",
                m.usedBytes() / 1e9, m.totalBytes() / 1e9, m.usedPercent()));
        progressSpace.setValue((int) m.usedPercent());

        lblSavingsRatio.setText(String.format("Space Savings: %.1f%%", m.overallSavingsRatio()));
        lblRawSize.setText(String.format("Raw Estimated: %.2f MB", m.totalRawBytesSaved() / 1e6));
        lblCompSize.setText(String.format("Stored on Disk: %.2f MB", m.totalCompressedBytes() / 1e6));
        lblArchivedCount.setText(String.format("Total Compressed Files: %d", m.totalArchivedFiles()));

        // Update list of documents
        listModel.clear();
        List<KnowledgeBase.DocumentChunk> chunks = brain.getKnowledgeBase().getAllChunks();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        for (KnowledgeBase.DocumentChunk c : chunks) {
            String dateStr = sdf.format(new Date(c.timestamp()));
            listModel.addElement(String.format("// %s | words: %d | date: %s", c.sourceName(), c.wordCount(), dateStr));
        }
        if (chunks.isEmpty()) {
            listModel.addElement("// No documents indexed yet. Use 'Ingest & Compress Files' to feed Cornelius.");
        }
    }

    private void selectAndIngest() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select File or Folder to Ingest");
        chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        int res = chooser.showOpenDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            Thread.ofVirtual().start(() -> {
                try {
                    DocumentIngester.IngestionSummary sum = brain.getIngester().ingestPath(file.toPath());
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(this,
                                String.format("Ingestion complete.\n\n- Files processed: %d\n- Space saved: %.2f%%\n- Data indexed and ready.",
                                        sum.filesProcessed(), sum.savingsRatio()),
                                "Ingestion Complete",
                                JOptionPane.INFORMATION_MESSAGE);
                        refreshMetrics();
                    });
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(this,
                                "Failed to ingest files: " + ex.getMessage(),
                                "Ingestion Error",
                                JOptionPane.ERROR_MESSAGE);
                    });
                }
            });
        }
    }
}
