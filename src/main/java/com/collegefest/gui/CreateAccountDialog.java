package com.collegefest.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingWorker;

import com.collegefest.service.AccountService;

/**
 * The "Create account" flow - two modal dialogs in one file:
 *
 *  1. showChooser(...)  ->  "Are you a student or a vendor?" with two
 *     buttons (matches the mock-up), then opens...
 *  2. new CreateAccountDialog(role)  ->  the signup form. Students get
 *     Username / Display name / Password / Confirm; vendors get Stall
 *     name FIRST (it's what students will see), then the same fields.
 *
 * All DB work (uniqueness check, BCrypt hashing, insert) happens in
 * AccountService on a SwingWorker - the dialog never blocks the EDT.
 * On success the dialog closes and hands the new username back to the
 * LoginFrame so it can be pre-filled, ready to sign in.
 */
public class CreateAccountDialog extends JDialog {

    // Same palette as LoginFrame so the flow feels like one app.
    // FEATURE ROUND: palette unified with AppTheme (charcoal + amber).
    private static final Color BG       = AppTheme.SURFACE;
    private static final Color FIELD_BG = AppTheme.SURFACE_LIGHT;
    private static final Color ACCENT   = AppTheme.ACCENT_AMBER;
    private static final Color TEXT     = AppTheme.TEXT_PRIMARY;
    private static final Color DIM      = AppTheme.TEXT_SECONDARY;
    private static final Color RED      = new Color(224, 108, 117);

    private final String role;
    private final Consumer<String> onCreated;

    private JTextField stallField;      // vendors only
    private JTextField usernameField;
    private JTextField displayNameField; // students only
    private JPasswordField passwordField;
    private JPasswordField confirmField;
    private JLabel errorLabel;
    private JButton createButton;

    /** Step 1: the student-or-vendor chooser (mock-up screen 3). */
    public static void showChooser(Window owner, Consumer<String> onCreated) {
        JDialog chooser = new JDialog(owner, "Create account",
                ModalityType.APPLICATION_MODAL);
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BG);
        panel.setBorder(BorderFactory.createEmptyBorder(28, 40, 28, 40));

        JLabel title = new JLabel("Are you a student or a vendor?");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(TEXT);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(title);
        panel.add(Box.createVerticalStrut(6));

        JLabel sub = new JLabel("Choose your account type to continue");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        sub.setForeground(DIM);
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(sub);
        panel.add(Box.createVerticalStrut(20));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 0));
        buttons.setOpaque(false);
        JButton studentBtn = accentButton("\uD83D\uDC64  Student");
        JButton vendorBtn  = accentButton("\uD83C\uDFEA  Vendor");
        studentBtn.addActionListener(e -> {
            chooser.dispose();
            new CreateAccountDialog(owner, AccountService.ROLE_STUDENT, onCreated)
                    .setVisible(true);
        });
        vendorBtn.addActionListener(e -> {
            chooser.dispose();
            new CreateAccountDialog(owner, AccountService.ROLE_VENDOR, onCreated)
                    .setVisible(true);
        });
        buttons.add(studentBtn);
        buttons.add(vendorBtn);
        panel.add(buttons);

        chooser.setContentPane(panel);
        chooser.pack();
        chooser.setLocationRelativeTo(owner);
        chooser.setVisible(true);
    }

    /** Step 2: the actual signup form for the chosen role. */
    public CreateAccountDialog(Window owner, String role, Consumer<String> onCreated) {
        super(owner, AccountService.ROLE_VENDOR.equals(role)
                ? "Create Vendor Account" : "Create Student Account",
                ModalityType.APPLICATION_MODAL);
        this.role = role;
        this.onCreated = onCreated;
        buildUi();
        pack();
        setLocationRelativeTo(owner);
    }

    private void buildUi() {
        boolean vendor = AccountService.ROLE_VENDOR.equals(role);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BG);
        panel.setBorder(BorderFactory.createEmptyBorder(24, 44, 24, 44));

        JLabel title = new JLabel(vendor ? "Vendor Account" : "Student Account");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(TEXT);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(title);
        panel.add(Box.createVerticalStrut(4));

        JLabel sub = new JLabel(vendor
                ? "Your stall name is shown to students when ordering."
                : "You will use this username to log in.");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        sub.setForeground(DIM);
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(sub);
        panel.add(Box.createVerticalStrut(18));

        if (vendor) {
            stallField = addField(panel, "Stall name (visible to students)", false);
        }
        usernameField = addField(panel, "Username (3-20 letters / numbers / _)", false);
        if (!vendor) {
            displayNameField = addField(panel, "Display name", false);
        }
        passwordField = (JPasswordField) addField(panel, "Password (min 6 characters)", true);
        confirmField  = (JPasswordField) addField(panel, "Confirm password", true);

        errorLabel = new JLabel(" ", JLabel.CENTER);
        errorLabel.setForeground(RED);
        errorLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        errorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(errorLabel);
        panel.add(Box.createVerticalStrut(8));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 0));
        buttons.setOpaque(false);
        createButton = accentButton("Create account");
        JButton cancel = accentButton("Cancel");
        createButton.addActionListener(e -> submit());   // lambda listeners
        cancel.addActionListener(e -> dispose());
        buttons.add(createButton);
        buttons.add(cancel);
        panel.add(buttons);

        setContentPane(panel);
    }

    /** Instant checks on the EDT, then AccountService on a SwingWorker. */
    private void submit() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        String confirm  = new String(confirmField.getPassword());
        boolean vendor  = AccountService.ROLE_VENDOR.equals(role);
        String name = vendor ? stallField.getText().trim()
                             : displayNameField.getText().trim();

        if (!password.equals(confirm)) {
            errorLabel.setText("Passwords do not match.");
            return;
        }

        errorLabel.setText(" ");
        createButton.setEnabled(false);
        createButton.setText("Creating...");

        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() {
                // Validation + uniqueness + BCrypt + MongoDB insert.
                AccountService.createAccount(role, username, name, password);
                return null;
            }
            @Override protected void done() {
                createButton.setEnabled(true);
                createButton.setText("Create account");
                try {
                    get();
                    JOptionPane.showMessageDialog(CreateAccountDialog.this,
                            (vendor ? "Stall '" + name + "' is ready!"
                                    : "Account created!")
                                    + "\nYou can now sign in as '" + username + "'.",
                            "Welcome to CollegeFest",
                            JOptionPane.INFORMATION_MESSAGE);
                    dispose();                    // back to the login page
                    onCreated.accept(username);   // pre-fill the username
                } catch (Exception ex) {
                    Throwable c = ex.getCause() != null ? ex.getCause() : ex;
                    if (c instanceof IllegalArgumentException) {
                        errorLabel.setText(c.getMessage());
                    } else {
                        errorLabel.setText("Connection problem - check internet.");
                    }
                }
            }
        }.execute();
    }

    // ---- small styling helpers ----

    private JTextField addField(JPanel panel, String label, boolean password) {
        JLabel l = new JLabel(label);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        l.setForeground(DIM);
        l.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(l);
        panel.add(Box.createVerticalStrut(6));

        JTextField f = password ? new JPasswordField() : new JTextField();
        f.setBackground(FIELD_BG);
        f.setForeground(TEXT);
        f.setCaretColor(TEXT);
        f.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        f.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        f.setMaximumSize(new Dimension(380, 42));
        f.setPreferredSize(new Dimension(380, 42));
        f.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(f);
        panel.add(Box.createVerticalStrut(14));
        return f;
    }

    private static JButton accentButton(String text) {
        JButton b = new JButton(text);
        b.setBackground(ACCENT);
        b.setForeground(AppTheme.BACKGROUND_DARK);   // dark text on amber
        b.setFont(new Font("Segoe UI Emoji", Font.BOLD, 15));
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(10, 26, 10, 26));
        b.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        return b;
    }
}
