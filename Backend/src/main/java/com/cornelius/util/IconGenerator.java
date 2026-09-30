package com.cornelius.util;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class IconGenerator {
    public static void main(String[] args) {
        int size = 256;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Deep Black rounded square
        g.setColor(new Color(9, 9, 11));
        g.fillRoundRect(8, 8, 240, 240, 48, 48);

        // Minimalist 1px Border
        g.setColor(new Color(63, 63, 70));
        g.setStroke(new BasicStroke(4));
        g.drawRoundRect(8, 8, 240, 240, 48, 48);

        // Minimalist Butler Top Hat Silhouette in Pure White
        g.setColor(Color.WHITE);
        // Hat brim
        g.fillRoundRect(42, 162, 172, 16, 8, 8);
        // Hat crown
        g.fillRoundRect(70, 72, 116, 96, 12, 12);

        // Subtle ribbon cutout
        g.setColor(new Color(24, 24, 27));
        g.fillRect(70, 146, 116, 14);

        // Monocle circle / AI lens
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(3));
        g.drawOval(144, 108, 22, 22);

        g.dispose();

        try {
            File out = new File("cornelius.png");
            ImageIO.write(img, "png", out);
            System.out.println("Ícone minimalista P&B gerado com sucesso: " + out.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
