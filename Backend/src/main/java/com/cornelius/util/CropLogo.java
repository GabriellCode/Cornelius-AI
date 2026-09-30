package com.cornelius.util;

import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class CropLogo {
    public static void main(String[] args) {
        try {
            File in = new File("logo.png");
            if (!in.exists()) return;
            BufferedImage img = ImageIO.read(in);

            int minX = img.getWidth(), minY = img.getHeight();
            int maxX = 0, maxY = 0;

            for (int y = 0; y < img.getHeight(); y++) {
                for (int x = 0; x < img.getWidth(); x++) {
                    int rgb = img.getRGB(x, y);
                    int r = (rgb >> 16) & 0xFF;
                    int g = (rgb >> 8) & 0xFF;
                    int b = rgb & 0xFF;
                    if (r > 40 || g > 40 || b > 40) {
                        if (x < minX) minX = x;
                        if (x > maxX) maxX = x;
                        if (y < minY) minY = y;
                        if (y > maxY) maxY = y;
                    }
                }
            }

            if (maxX > minX && maxY > minY) {
                int pad = 12;
                minX = Math.max(0, minX - pad);
                minY = Math.max(0, minY - pad);
                maxX = Math.min(img.getWidth() - 1, maxX + pad);
                maxY = Math.min(img.getHeight() - 1, maxY + pad);

                BufferedImage cropped = img.getSubimage(minX, minY, maxX - minX + 1, maxY - minY + 1);
                ImageIO.write(cropped, "png", new File("logo.png"));
                System.out.println("Logo ajustado perfeitamente: " + cropped.getWidth() + "x" + cropped.getHeight());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

