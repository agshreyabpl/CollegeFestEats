package com.collegefest;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import com.collegefest.gui.AppTheme;
import com.collegefest.gui.LoginFrame;

/**
 * Application entry point. Run THIS file to start CollegeFest Eats.
 *
 * Two jobs:
 *  1. Install a GLOBAL uncaught-exception handler: any crash on ANY thread
 *     is appended to error-log.txt (try-with-resources, append mode) and
 *     shown to the user once as a friendly dialog - never a silent death.
 *  2. Show a small splash for ~1.5 s, then open the LoginFrame. All UI is
 *     created on the Event Dispatch Thread via invokeLater (Swing's rule).
 */
public class Main {

    public static void main(String[] args) {

        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            logToFile(thread, throwable);
            SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(null,
                            "Something went wrong:\n" + throwable.getMessage()
                                    + "\n\nDetails were saved to error-log.txt",
                            "Unexpected error", JOptionPane.ERROR_MESSAGE));
        });

        SwingUtilities.invokeLater(Main::showSplashThenLogin);
    }

    private static void showSplashThenLogin() {
        javax.swing.JWindow splash = new javax.swing.JWindow();
        javax.swing.JPanel panel = new javax.swing.JPanel(new java.awt.BorderLayout(10, 10));
        panel.setBackground(new java.awt.Color(0x161616));   // charcoal, matches login
        panel.setBorder(javax.swing.BorderFactory.createEmptyBorder(30, 40, 30, 40));

        javax.swing.JLabel label = new javax.swing.JLabel(
                "CollegeFest Eats — Loading...", javax.swing.JLabel.CENTER);
        label.setForeground(AppTheme.ACCENT_AMBER);          // amber, matches the app
        label.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 20));
        javax.swing.JProgressBar bar = new javax.swing.JProgressBar();
        bar.setIndeterminate(true);

        panel.add(label, java.awt.BorderLayout.CENTER);
        panel.add(bar, java.awt.BorderLayout.SOUTH);
        splash.setContentPane(panel);
        splash.pack();
        splash.setLocationRelativeTo(null);
        splash.setVisible(true);

        // One-shot Swing Timer: its callback runs ON the EDT - safe for UI.
        javax.swing.Timer timer = new javax.swing.Timer(1500, e -> {
            splash.dispose();
            // Dark-theme JOptionPane/tooltips everywhere, BEFORE any frame
            // exists (AppTheme's own note asks for exactly this call site).
            AppTheme.applyGlobalDefaults();
            new LoginFrame().setVisible(true);
        });
        timer.setRepeats(false);
        timer.start();
    }

    private static void logToFile(Thread thread, Throwable throwable) {
        try (PrintWriter out = new PrintWriter(
                new BufferedWriter(new FileWriter("error-log.txt", true)))) {
            out.println("==== " + LocalDateTime.now()
                    + "  (thread: " + thread.getName() + ") ====");
            throwable.printStackTrace(out);
            out.println();
        } catch (IOException io) {
            io.printStackTrace();
        }
    }
}
