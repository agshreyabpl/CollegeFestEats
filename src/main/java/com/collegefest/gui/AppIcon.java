package main.java.com.collegefest.gui;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Draws a small "CF" monogram icon at runtime, so every JFrame can set a
 * consistent taskbar/title-bar icon without needing an image file bundled
 * into the project (and without touching src/main/resources yet).
 *
 * Uses the same dark + amber palette as AppTheme.
 */
public class AppIcon {

    public static Image generate() {
        int size = 64;
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g.setColor(AppTheme.BACKGROUND_DARK);
        g.fillRoundRect(0, 0, size, size, 16, 16);

        g.setColor(AppTheme.ACCENT_AMBER);
        g.setFont(new Font("Segoe UI", Font.BOLD, 26));
        FontMetrics fm = g.getFontMetrics();
        String text = "CF";
        int textWidth = fm.stringWidth(text);
        int baseline = (size + fm.getAscent()) / 2 - 4;
        g.drawString(text, (size - textWidth) / 2, baseline);

        g.dispose();
        return image;
    }
}
