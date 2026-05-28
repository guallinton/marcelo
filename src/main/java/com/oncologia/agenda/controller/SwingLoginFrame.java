package com.oncologia.agenda.controller;

import com.oncologia.agenda.AppInfo;
import com.oncologia.agenda.model.User;
import com.oncologia.agenda.service.AuthService;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

public class SwingLoginFrame extends JFrame {
    private final AuthService authService = new AuthService();
    private final JTextField username = new JTextField("enfermera", 20);
    private final JPasswordField password = new JPasswordField("1234", 20);

    public SwingLoginFrame() {
        super(AppInfo.displayName() + " - Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(520, 420));
        setLocationRelativeTo(null);

        JPanel shell = new JPanel(new BorderLayout());
        shell.setBorder(new EmptyBorder(28, 34, 28, 34));
        shell.setBackground(new Color(245, 248, 252));

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(218, 226, 238)),
                new EmptyBorder(24, 32, 24, 32)
        ));

        JLabel title = new JLabel(AppInfo.NAME, SwingConstants.CENTER);
        title.setAlignmentX(CENTER_ALIGNMENT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 24f));
        JLabel subtitle = new JLabel("Agenda simple para quimioterapia", SwingConstants.CENTER);
        subtitle.setAlignmentX(CENTER_ALIGNMENT);
        subtitle.setForeground(new Color(90, 103, 120));
        JLabel version = new JLabel("Version " + AppInfo.VERSION + " - " + AppInfo.RELEASE_NAME, SwingConstants.CENTER);
        version.setAlignmentX(CENTER_ALIGNMENT);
        version.setForeground(new Color(100, 116, 139));

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        form.setBorder(BorderFactory.createEmptyBorder(18, 0, 8, 0));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        form.add(new JLabel("Usuario"), gbc);
        gbc.gridx = 1;
        username.setToolTipText("Usuarios: enfermera o drlopez");
        username.putClientProperty("JTextField.placeholderText", "Usuario");
        form.add(username, gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        form.add(new JLabel("Contrasena"), gbc);
        gbc.gridx = 1;
        password.setToolTipText("Clave precargada: 1234");
        password.putClientProperty("JTextField.placeholderText", "Contrasena");
        form.add(password, gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        JButton login = new JButton("Ingresar");
        login.putClientProperty("JButton.buttonType", "roundRect");
        login.setFont(login.getFont().deriveFont(Font.BOLD));
        login.addActionListener(e -> doLogin());
        getRootPane().setDefaultButton(login);
        form.add(login, gbc);

        card.add(title);
        card.add(Box.createVerticalStrut(6));
        card.add(subtitle);
        card.add(Box.createVerticalStrut(2));
        card.add(version);
        card.add(form);
        shell.add(card, BorderLayout.CENTER);
        add(shell, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(null);
    }

    private void doLogin() {
        String pass = new String(password.getPassword());
        java.util.Optional<User> user = authService.login(username.getText(), pass);
        if (user.isPresent()) {
            dispose();
            new SwingMainFrame(user.get()).setVisible(true);
            return;
        }
        JOptionPane.showMessageDialog(this, "Usuario o contrasena invalidos.", "Atencion", JOptionPane.ERROR_MESSAGE);
    }
}
