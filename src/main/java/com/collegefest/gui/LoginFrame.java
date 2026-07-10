package com.collegefest.gui;

import com.collegefest.db.UserDAO;
import com.collegefest.models.User;
import org.mindrot.jbcrypt.BCrypt;

import javax.swing.*;
import java.awt.*;

public class LoginFrame extends JFrame {

    private JTextField userIdField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JLabel statusLabel;
    private UserDAO userDAO;

    public LoginFrame() {
        userDAO = new UserDAO();
        setupUI();
    }

    private void setupUI() {
        setTitle("CollegeFest — Login");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // centers the window on screen
        setResizable(false);

        // Main panel with padding
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        panel.setBackground(new Color(30, 30, 45));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Title label
        JLabel titleLabel = new JLabel("CollegeFest Order System", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        titleLabel.setForeground(new Color(255, 200, 50));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        panel.add(titleLabel, gbc);

        // User ID label and field
        gbc.gridwidth = 1;
        gbc.gridx = 0; gbc.gridy = 1;
        JLabel userLabel = new JLabel("User ID:");
        userLabel.setForeground(Color.WHITE);
        panel.add(userLabel, gbc);

        gbc.gridx = 1; gbc.gridy = 1;
        userIdField = new JTextField(15);
        panel.add(userIdField, gbc);

        // Password label and field
        gbc.gridx = 0; gbc.gridy = 2;
        JLabel passLabel = new JLabel("Password:");
        passLabel.setForeground(Color.WHITE);
        panel.add(passLabel, gbc);

        gbc.gridx = 1; gbc.gridy = 2;
        passwordField = new JPasswordField(15);
        panel.add(passwordField, gbc);

        // Login button
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        loginButton = new JButton("Login");
        loginButton.setBackground(new Color(70, 130, 180));
        loginButton.setForeground(Color.WHITE);
        loginButton.setFont(new Font("Arial", Font.BOLD, 14));
        loginButton.setFocusPainted(false);
        panel.add(loginButton, gbc);

        // Status label for error messages
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        statusLabel = new JLabel("", SwingConstants.CENTER);
        statusLabel.setForeground(new Color(255, 80, 80));
        panel.add(statusLabel, gbc);

        add(panel);

        // Login button action — lambda expression (required Java feature)
        loginButton.addActionListener(e -> handleLogin());

        // Also allow pressing Enter to login
        passwordField.addActionListener(e -> handleLogin());
    }

    private void handleLogin() {
        String userId = userIdField.getText().trim();
        String password = new String(passwordField.getPassword());

        // Regex validation — format must be STU001 or VEN001 style
        java.util.regex.Pattern studentPattern = java.util.regex.Pattern.compile("^STU\\d{3}$");
        java.util.regex.Pattern vendorPattern  = java.util.regex.Pattern.compile("^VEN\\d{3}$");

        if (userId.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Please enter both User ID and password.");
            return;
        }

        if (!studentPattern.matcher(userId).matches() && 
            !vendorPattern.matcher(userId).matches()) {
            statusLabel.setText("Invalid ID format. Use STU001 or VEN001.");
            return;
        }

        loginButton.setEnabled(false);
        statusLabel.setText("Logging in...");

        SwingWorker<User, Void> worker = new SwingWorker<>() {
            @Override
            protected User doInBackground() {
                return userDAO.findByUserId(userId);
            }

            @Override
            protected void done() {
                try {
                    User user = get();
                    if (!BCrypt.checkpw(password, user.getPassword())) {
                        statusLabel.setText("Incorrect password.");
                        loginButton.setEnabled(true);
                        return;
                    }
                    statusLabel.setText("Login successful!");
                    dispose();
                    if ("vendor".equals(user.getRole())) {
                        new VendorDashboard(user).setVisible(true);
                    } else {
                        new StudentDashboard(user).setVisible(true);
                    }
                } catch (java.util.concurrent.ExecutionException ex) {
                    // Catches UserNotFoundException thrown from DAO
                    statusLabel.setText(ex.getCause().getMessage());
                    loginButton.setEnabled(true);
                } catch (Exception ex) {
                    statusLabel.setText("Error: " + ex.getMessage());
                    loginButton.setEnabled(true);
                }
            }
        };
        worker.execute();
    }

    public static void main(String[] args) {
        // Launch on the Swing event thread (correct way to start Swing apps)
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}