package uppa.group2.ui;

import uppa.group2.NetworkUtils;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Dialogue de connexion affiché au démarrage.
 * Permet de choisir son pseudonyme et son port d'écoute.
 */
public class LoginDialog extends JDialog {

    private static final Color COLOR_BG = new Color(30, 30, 46);
    private static final Color COLOR_PANEL = new Color(49, 50, 68);
    private static final Color COLOR_ACCENT = new Color(137, 180, 250);
    private static final Color COLOR_TEXT = new Color(205, 214, 244);
    private static final Color COLOR_MUTED = new Color(108, 112, 134);

    private JTextField usernameField;
    private JTextField portField;
    private boolean confirmed = false;

    public LoginDialog() {
        super((Frame) null, "P2P Chat — Connexion", true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);

        initUI();
        pack();
        setLocationRelativeTo(null);
    }

    private void initUI() {
        JPanel main = new JPanel(new BorderLayout(0, 0));
        main.setBackground(COLOR_BG);

        // Header
        JPanel header = new JPanel(new FlowLayout(FlowLayout.CENTER));
        header.setBackground(COLOR_PANEL);
        header.setBorder(new EmptyBorder(20, 30, 20, 30));

        JLabel title = new JLabel("💬 P2P Chat");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));
        title.setForeground(COLOR_ACCENT);

        JLabel subtitle = new JLabel("Système de messagerie pair-à-pair");
        subtitle.setFont(subtitle.getFont().deriveFont(12f));
        subtitle.setForeground(COLOR_MUTED);

        JPanel headerContent = new JPanel();
        headerContent.setLayout(new BoxLayout(headerContent, BoxLayout.Y_AXIS));
        headerContent.setOpaque(false);
        headerContent.add(title);
        headerContent.add(Box.createVerticalStrut(4));
        headerContent.add(subtitle);

        header.add(headerContent);
        main.add(header, BorderLayout.NORTH);

        // Formulaire
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(COLOR_BG);
        form.setBorder(new EmptyBorder(24, 40, 16, 40));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 4, 6, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Pseudonyme
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 1;
        JLabel userLabel = styledLabel("Pseudonyme:");
        form.add(userLabel, gbc);

        gbc.gridx = 1;
        usernameField = styledTextField("", 16);
        usernameField.setToolTipText("Votre nom d'utilisateur visible par les autres");
        form.add(usernameField, gbc);

        // Port d'écoute
        gbc.gridx = 0; gbc.gridy = 1;
        JLabel portLabel = styledLabel("Port d'écoute:");
        form.add(portLabel, gbc);

        gbc.gridx = 1;
        portField = styledTextField("5000", 16);
        portField.setToolTipText("Port TCP sur lequel ce nœud écoute (1025-65534)");
        form.add(portField, gbc);

        // Info IP locale
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        String localIp = NetworkUtils.getLocalHost();
        JLabel ipInfo = new JLabel("<html><center>Votre adresse: <b>" + localIp + "</b><br>" +
                "<font color='#6c7086'>Communiquez cette adresse aux autres utilisateurs</font></center></html>");
        ipInfo.setForeground(COLOR_TEXT);
        ipInfo.setHorizontalAlignment(SwingConstants.CENTER);
        ipInfo.setBorder(new EmptyBorder(4, 0, 4, 0));
        form.add(ipInfo, gbc);

        main.add(form, BorderLayout.CENTER);

        // Boutons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 12));
        btnPanel.setBackground(COLOR_BG);

        JButton cancelBtn = new JButton("Annuler");
        cancelBtn.setForeground(COLOR_MUTED);
        cancelBtn.setBackground(COLOR_PANEL);
        cancelBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_MUTED, 1),
                new EmptyBorder(7, 18, 7, 18)));
        cancelBtn.setFocusPainted(false);
        cancelBtn.addActionListener(e -> {
            confirmed = false;
            dispose();
            System.exit(0);
        });

        JButton connectBtn = new JButton("Se connecter");
        connectBtn.setForeground(COLOR_ACCENT);
        connectBtn.setBackground(COLOR_PANEL);
        connectBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_ACCENT, 1),
                new EmptyBorder(7, 18, 7, 18)));
        connectBtn.setFocusPainted(false);
        connectBtn.setFont(connectBtn.getFont().deriveFont(Font.BOLD));
        connectBtn.addActionListener(e -> tryConfirm());
        connectBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        btnPanel.add(cancelBtn);
        btnPanel.add(connectBtn);

        main.add(btnPanel, BorderLayout.SOUTH);

        // Appuyer Entrée dans n'importe quel champ
        KeyAdapter enterHandler = new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) tryConfirm();
            }
        };
        usernameField.addKeyListener(enterHandler);
        portField.addKeyListener(enterHandler);

        setContentPane(main);
    }

    private void tryConfirm() {
        String username = usernameField.getText().trim();
        String portStr = portField.getText().trim();

        if (username.isEmpty()) {
            showError("Le pseudonyme ne peut pas être vide.");
            usernameField.requestFocus();
            return;
        }
        if (username.contains("@") || username.contains(";") || username.contains(":")) {
            showError("Le pseudonyme ne doit pas contenir les caractères @, ; ou :.");
            usernameField.requestFocus();
            return;
        }
        if (username.length() > 20) {
            showError("Le pseudonyme ne doit pas dépasser 20 caractères.");
            return;
        }

        int port;
        try {
            port = Integer.parseInt(portStr);
            if (!NetworkUtils.isValidPort(port)) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            showError("Le port doit être un entier entre 1025 et 65534.");
            portField.requestFocus();
            return;
        }

        confirmed = true;
        dispose();
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Erreur de saisie", JOptionPane.WARNING_MESSAGE);
    }

    private JLabel styledLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(COLOR_TEXT);
        return label;
    }

    private JTextField styledTextField(String text, int cols) {
        JTextField field = new JTextField(text, cols);
        field.setBackground(new Color(69, 71, 90));
        field.setForeground(COLOR_TEXT);
        field.setCaretColor(COLOR_TEXT);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_ACCENT, 1),
                new EmptyBorder(5, 8, 5, 8)));
        return field;
    }

    public boolean isConfirmed() { return confirmed; }
    public String getUsername() { return usernameField.getText().trim(); }
    public int getPort() { return Integer.parseInt(portField.getText().trim()); }
}