package com.oncologia.agenda;

import com.formdev.flatlaf.FlatLightLaf;
import com.oncologia.agenda.controller.SwingLoginFrame;
import com.oncologia.agenda.dao.Database;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class AgendaApplication {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    UIManager.setLookAndFeel(new FlatLightLaf());
                } catch (Exception ignored) {
                    // Swing falls back to the platform look and feel if FlatLaf cannot initialize.
                }
                Database.initialize();
                new SwingLoginFrame().setVisible(true);
            }
        });
    }
}
