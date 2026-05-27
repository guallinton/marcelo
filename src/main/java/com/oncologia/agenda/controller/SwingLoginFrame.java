package com.oncologia.agenda.controller;

import com.oncologia.agenda.model.User;
import com.oncologia.agenda.service.AuthService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

public class SwingLoginFrame extends JFrame {
    private final AuthService authService = new AuthService();
    private final JTextField username = new JTextField("enfermera", 20);
    private final JPasswordField password = new JPasswordField("1234", 20);

    public SwingLoginFrame() {
        super("Agenda Oncologica - Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(460, 330));
        setLocationRelativeTo(null);

        JLabel title = new JLabel("Agenda Oncologica", SwingConstants.CENTER);
        title.putClientProperty("FlatLaf.styleClass", "h1");
        JLabel subtitle = new JLabel("Gestion de turnos de quimioterapia", SwingConstants.CENTER);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(24, 42, 24, 42));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        form.add(title, gbc);
        gbc.gridy++;
        form.add(subtitle, gbc);
        gbc.gridwidth = 1;
        gbc.gridy++;
        form.add(new JLabel("Usuario"), gbc);
        gbc.gridx = 1;
        username.setToolTipText("Usuarios: enfermera o drlopez");
        form.add(username, gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        form.add(new JLabel("Contrasena"), gbc);
        gbc.gridx = 1;
        password.setToolTipText("Clave precargada: 1234");
        form.add(password, gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        JButton login = new JButton("Ingresar");
        login.addActionListener(e -> doLogin());
        getRootPane().setDefaultButton(login);
        form.add(login, gbc);

        add(form, BorderLayout.CENTER);
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
