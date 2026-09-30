package com.cornelius.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

public class ModernTheme {
    // Antigravity Minimalist Black & White Palette
    public static final Color BG_VOID = new Color(0, 0, 0);          // Pure Black #000000
    public static final Color BG_DARK = new Color(9, 9, 11);         // #09090b
    public static final Color BG_SURFACE = new Color(18, 18, 20);     // #121214
    public static final Color BG_CARD = new Color(24, 24, 27);        // #18181b
    public static final Color BG_CARD_HOVER = new Color(34, 34, 38);
    public static final Color BG_INPUT = new Color(15, 15, 17);
    
    public static final Color BORDER_MUTED = new Color(39, 39, 42);   // #27272a
    public static final Color BORDER_LIGHT = new Color(63, 63, 70);   // #3f3f46
    public static final Color BORDER_ACTIVE = new Color(255, 255, 255);

    public static final Color TEXT_WHITE = new Color(250, 250, 250);  // #fafafa
    public static final Color TEXT_MUTED = new Color(161, 161, 170);  // #a1a1aa
    public static final Color TEXT_DIM = new Color(113, 113, 122);    // #71717a

    public static final Font FONT_BRAND = new Font("SansSerif", Font.BOLD, 15);
    public static final Font FONT_TITLE = new Font("SansSerif", Font.BOLD, 17);
    public static final Font FONT_SUBTITLE = new Font("SansSerif", Font.BOLD, 13);
    public static final Font FONT_BODY = new Font("SansSerif", Font.PLAIN, 13);
    public static final Font FONT_CODE = new Font("Monospaced", Font.PLAIN, 12);
    public static final Font FONT_SMALL = new Font("SansSerif", Font.PLAIN, 11);
    public static final Font FONT_MONO_SMALL = new Font("Monospaced", Font.PLAIN, 11);

    public static void applyGlobal() {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) {}

        UIManager.put("Panel.background", BG_DARK);
        UIManager.put("OptionPane.background", BG_DARK);
        UIManager.put("OptionPane.messageForeground", TEXT_WHITE);
        UIManager.put("Label.foreground", TEXT_WHITE);
        UIManager.put("Label.font", FONT_BODY);
        UIManager.put("Button.font", FONT_BODY);
        UIManager.put("TextField.font", FONT_BODY);
        UIManager.put("TextArea.font", FONT_BODY);
    }

    public static JButton createMinimalButton(String text, boolean primary) {
        JButton btn = new JButton(text) {
            private boolean hover = false;
            {
                addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) {
                        hover = true;
                        repaint();
                    }
                    @Override
                    public void mouseExited(MouseEvent e) {
                        hover = false;
                        repaint();
                    }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                if (primary) {
                    g2.setColor(hover ? new Color(220, 220, 220) : Color.WHITE);
                    g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));
                    g2.setColor(Color.BLACK);
                } else {
                    g2.setColor(hover ? BG_CARD_HOVER : BG_SURFACE);
                    g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));
                    g2.setColor(hover ? BORDER_LIGHT : BORDER_MUTED);
                    g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 8, 8));
                    g2.setColor(hover ? TEXT_WHITE : TEXT_MUTED);
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
        btn.setBorder(new EmptyBorder(8, 16, 8, 16));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    public static JTextField createMinimalTextField(String placeholder) {
        JTextField tf = new JTextField() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_INPUT);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));
                
                g2.setColor(isFocusOwner() ? BORDER_ACTIVE : BORDER_MUTED);
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 8, 8));
                g2.dispose();

                super.paintComponent(g);
            }
        };

        tf.setOpaque(false);
        tf.setBackground(new Color(0, 0, 0, 0));
        tf.setForeground(TEXT_WHITE);
        tf.setCaretColor(TEXT_WHITE);
        tf.setFont(new Font("SansSerif", Font.PLAIN, 13));
        tf.setBorder(new EmptyBorder(10, 14, 10, 14));
        return tf;
    }

    public static void styleScrollBar(JScrollPane scrollPane) {
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(BG_DARK);
        scrollPane.setBackground(BG_DARK);
        scrollPane.getVerticalScrollBar().setBackground(BG_DARK);
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(6, 0));
        scrollPane.getVerticalScrollBar().setUI(new BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                this.thumbColor = BORDER_MUTED;
                this.trackColor = BG_DARK;
            }

            @Override
            protected JButton createDecreaseButton(int orientation) {
                return createZeroButton();
            }

            @Override
            protected JButton createIncreaseButton(int orientation) {
                return createZeroButton();
            }

            private JButton createZeroButton() {
                JButton b = new JButton();
                b.setPreferredSize(new Dimension(0, 0));
                return b;
            }

            @Override
            protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
                if (thumbBounds.isEmpty() || !scrollbar.isEnabled()) return;
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isThumbRollover() ? BORDER_LIGHT : BORDER_MUTED);
                g2.fillRoundRect(thumbBounds.x, thumbBounds.y, thumbBounds.width, thumbBounds.height, 4, 4);
                g2.dispose();
            }
        });
    }

    // Animated Pulsing Indicator
    public static class PulsingDot extends JComponent {
        private float alpha = 1.0f;
        private boolean fading = true;
        private Color dotColor = Color.WHITE;

        public PulsingDot() {
            setPreferredSize(new Dimension(14, 14));
            Timer timer = new Timer(50, e -> {
                if (fading) {
                    alpha -= 0.05f;
                    if (alpha <= 0.3f) {
                        alpha = 0.3f;
                        fading = false;
                    }
                } else {
                    alpha += 0.05f;
                    if (alpha >= 1.0f) {
                        alpha = 1.0f;
                        fading = true;
                    }
                }
                repaint();
            });
            timer.start();
        }

        public void setDotColor(Color c) {
            this.dotColor = c;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Outer glow halo
            int w = getWidth();
            int h = getHeight();
            g2.setColor(new Color(dotColor.getRed(), dotColor.getGreen(), dotColor.getBlue(), (int) (alpha * 60)));
            g2.fillOval(1, 1, w - 2, h - 2);

            // Inner core dot
            g2.setColor(new Color(dotColor.getRed(), dotColor.getGreen(), dotColor.getBlue(), (int) (alpha * 255)));
            g2.fillOval(4, 4, w - 8, h - 8);

            g2.dispose();
        }
    }
}
