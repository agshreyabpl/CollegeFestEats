package com.collegefest.gui;

import com.collegefest.models.User;
import javax.swing.*;

public class VendorDashboard extends JFrame {

    private User currentUser;

    public VendorDashboard(User user) {
        this.currentUser = user;
        setTitle("Vendor Dashboard — " + user.getName());
        setSize(700, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Placeholder label — we'll build the real UI next
        JLabel label = new JLabel(
            "Welcome, " + user.getName() + "! Vendor dashboard coming soon.",
            SwingConstants.CENTER
        );
        label.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 16));
        add(label);
    }
}