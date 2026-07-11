package com.collegefest.gui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.RenderingHints;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingWorker;

import org.mindrot.jbcrypt.BCrypt;

import com.collegefest.db.UserDAO;
import com.collegefest.exceptions.UserNotFoundException;
import com.collegefest.models.User;
import com.collegefest.service.OrderService;

/**
 * Login screen - the app's front door. Modern "card" design: rounded dark
 * card, indigo Sign-in button, icons inside the fields, credentials pill.
 *
 * The important parts are invisible:
 *  1. REGEX validation (Pattern) runs BEFORE any database call - a badly
 *     formatted ID is rejected instantly with an inline red label.
 *  2. SwingWorker keeps the MongoDB lookup + BCrypt check off the Event
 *     Dispatch Thread, so the window never freezes while Atlas answers.
 *  3. BCrypt.checkpw() compares the typed password to the stored HASH -
 *     the real password never exists in the database.
 *  4. Wrong-password and unknown-user produce the SAME message on purpose,
 *     so nobody can probe which user IDs exist.
 *  5. Routing: student -> StudentDashboardFrame, vendor -> VendorDashboard.
 *
 * Swing has no CSS border-radius, so RoundedPanel/RoundedButton (inner
 * classes) paint their own antialiased rounded rectangles.
 */
public class LoginFrame extends JFrame {

    // Usernames are free-form since self-signup exists: 3-20 letters,
    // numbers or underscore (STU001 / VEN001 still match, so the seeded
    // demo accounts keep working). REGEX still gates every login BEFORE
    // any database call. The dashboard is chosen by the user's ROLE
    // stored in MongoDB, never by the username's shape.
    private static final Pattern USERNAME_PATTERN =
            com.collegefest.service.AccountService.USERNAME_PATTERN;

    // FEATURE ROUND: palette unified with the rest of the app — these now
    // point at AppTheme's charcoal/amber values instead of the old indigo.
    private static final Color WINDOW_BG = new Color(0x161616);             // a step deeper than the card
    private static final Color CARD_BG   = AppTheme.SURFACE;                // 0x2A2A2A
    private static final Color FIELD_BG  = AppTheme.SURFACE_LIGHT;          // 0x333333
    private static final Color PILL_EDGE = AppTheme.BORDER;                 // 0x3D3D3D
    static final Color ACCENT           = AppTheme.ACCENT_AMBER;            // 0xFFA726
    private static final Color TEXT_MAIN = AppTheme.TEXT_PRIMARY;           // 0xF5F5F5
    private static final Color TEXT_DIM  = AppTheme.TEXT_SECONDARY;         // 0xAAAAAA
    private static final Color ERROR_RED = new Color(224, 108, 117);

    private final JTextField userIdField = new JTextField();
    private final JPasswordField passwordField = new JPasswordField();
    private final JLabel errorLabel = new JLabel(" ", JLabel.CENTER);
    private final JButton loginButton = new RoundedButton("Sign in");

    public LoginFrame() {
        super("CollegeFest Order System - Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(520, 700);
        setLocationRelativeTo(null);
        setResizable(false);
        setIconImage(AppIcon.generate());
        buildUi();
    }

    private void buildUi() {
        JPanel background = new JPanel(new GridBagLayout());
        background.setBackground(WINDOW_BG);

        RoundedPanel card = new RoundedPanel(28, CARD_BG);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(36, 44, 36, 44));

        JLabel tent = new JLabel("\uD83C\uDFAA", JLabel.CENTER);   // 🎪
        tent.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 44));
        tent.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(tent);
        card.add(Box.createVerticalStrut(10));

        JLabel title = new JLabel("CollegeFest Order System", JLabel.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(TEXT_MAIN);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(title);
        card.add(Box.createVerticalStrut(6));

        JLabel subtitle = new JLabel("Sign in to continue", JLabel.CENTER);
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        subtitle.setForeground(TEXT_DIM);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(subtitle);
        card.add(Box.createVerticalStrut(28));

        card.add(fieldLabel("USERNAME"));
        card.add(Box.createVerticalStrut(8));
        card.add(iconField("\uD83D\uDC64", userIdField));   // 👤
        card.add(Box.createVerticalStrut(18));

        card.add(fieldLabel("PASSWORD"));
        card.add(Box.createVerticalStrut(8));
        card.add(iconField("\uD83D\uDD12", passwordField)); // 🔒
        card.add(Box.createVerticalStrut(10));

        errorLabel.setForeground(ERROR_RED);
        errorLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        errorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(errorLabel);
        card.add(Box.createVerticalStrut(10));

        loginButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        loginButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
        loginButton.addActionListener(e -> attemptLogin());   // lambda listener
        card.add(loginButton);
        card.add(Box.createVerticalStrut(12));

        // New: self-service signup (students AND vendors).
        JButton createAccountButton = new RoundedButton("Create account");
        createAccountButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        createAccountButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
        createAccountButton.addActionListener(e ->
                CreateAccountDialog.showChooser(this, newUsername -> {
                    // Back on the login page with the fresh username filled
                    // in - the person only has to type their password.
                    userIdField.setText(newUsername);
                    passwordField.setText("");
                    passwordField.requestFocusInWindow();
                }));
        card.add(createAccountButton);
        card.add(Box.createVerticalStrut(18));

        RoundedPanel pill = new RoundedPanel(18, FIELD_BG);
        pill.setStroke(PILL_EDGE);
        pill.setLayout(new GridBagLayout());
        pill.setBorder(BorderFactory.createEmptyBorder(8, 18, 8, 18));
        JLabel hint = new JLabel("Already have an account? Login with your username.");
        hint.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        hint.setForeground(TEXT_DIM);
        pill.add(hint);
        pill.setMaximumSize(new Dimension(340, 38));
        pill.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(pill);

        card.setPreferredSize(new Dimension(420, 560));
        background.add(card);
        setContentPane(background);

        passwordField.addActionListener(e -> attemptLogin());
    }

    private JPanel fieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 12));
        label.setForeground(TEXT_DIM);
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.add(label, BorderLayout.WEST);
        wrap.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));
        wrap.setAlignmentX(Component.CENTER_ALIGNMENT);
        return wrap;
    }

    private JPanel iconField(String icon, JTextField field) {
        RoundedPanel box = new RoundedPanel(14, FIELD_BG);
        box.setLayout(new BorderLayout(10, 0));
        box.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

        JLabel iconLabel = new JLabel(icon);
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));
        iconLabel.setForeground(ACCENT);
        box.add(iconLabel, BorderLayout.WEST);

        field.setOpaque(false);
        field.setBorder(null);
        field.setForeground(TEXT_MAIN);
        field.setCaretColor(TEXT_MAIN);
        field.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        box.add(field, BorderLayout.CENTER);

        box.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        box.setAlignmentX(Component.CENTER_ALIGNMENT);
        return box;
    }

    /** empty check -> regex check (no DB) -> SwingWorker (DB + BCrypt). */
    private void attemptLogin() {
        String userId = userIdField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (userId.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Please fill in both fields.");
            return;
        }
        if (!USERNAME_PATTERN.matcher(userId).matches()) {
            errorLabel.setText("Username must be 3-20 letters, numbers or _.");
            return;
        }

        errorLabel.setText(" ");
        loginButton.setEnabled(false);
        loginButton.setText("Checking...");

        new SwingWorker<User, Void>() {
            @Override protected User doInBackground() {
                UserDAO userDAO = new UserDAO();
                User user = userDAO.findByUserId(userId);  // throws if unknown
                if (!BCrypt.checkpw(password, user.getPassword())) {
                    throw new UserNotFoundException(userId); // same msg as unknown
                }
                return user;
            }
            @Override protected void done() {
                loginButton.setEnabled(true);
                loginButton.setText("Sign in");
                try {
                    openDashboard(get());
                } catch (Exception e) {
                    Throwable cause = (e.getCause() != null) ? e.getCause() : e;
                    if (cause instanceof UserNotFoundException) {
                        errorLabel.setText("Invalid username or password.");
                    } else {
                        errorLabel.setText("Connection problem - check internet.");
                        cause.printStackTrace();
                    }
                }
            }
        }.execute();
    }

    /** Routes by role. Session goes into OrderService's ConcurrentHashMap. */
    private void openDashboard(User user) {
        OrderService.registerSession(user.getUserId(), user);
        if ("student".equalsIgnoreCase(user.getRole())) {
            new StudentDashboardFrame(user.getUserId(), user.getName())
                    .setVisible(true);
        } else {
            // Person B's dashboard makes itself visible in its constructor.
            // Now gets the stall NAME too, for the header + logout flow.
            new VendorDashboard(user.getUserId(), user.getName(),
                    OrderService.getInstance());
        }
        dispose();
    }

    // ---- rounded-corner helpers (inner classes) ----

    private static class RoundedPanel extends JPanel {
        private final int radius;
        private final Color fill;
        private Color stroke;

        RoundedPanel(int radius, Color fill) {
            this.radius = radius;
            this.fill = fill;
            setOpaque(false);
        }
        void setStroke(Color c) { this.stroke = c; }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            if (stroke != null) {
                g2.setColor(stroke);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static class RoundedButton extends JButton {
        RoundedButton(String text) {
            super(text);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setForeground(AppTheme.BACKGROUND_DARK);   // dark text on amber
            setFont(new Font("Segoe UI", Font.BOLD, 16));
            setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                RenderingHints.VALUE_ANTIALIAS_ON);
            Color base = LoginFrame.ACCENT;
            if (!isEnabled() || getModel().isPressed()) g2.setColor(base.darker());
            else g2.setColor(base);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
