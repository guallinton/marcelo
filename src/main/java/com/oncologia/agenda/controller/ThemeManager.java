package com.oncologia.agenda.controller;

import javafx.scene.Scene;

public class ThemeManager {
    private boolean darkMode;

    public void apply(Scene scene) {
        scene.getStylesheets().clear();
        String stylesheet = darkMode ? "/css/dark.css" : "/css/light.css";
        scene.getStylesheets().add(getClass().getResource(stylesheet).toExternalForm());
    }

    public void toggle(Scene scene) {
        darkMode = !darkMode;
        apply(scene);
    }

    public boolean isDarkMode() {
        return darkMode;
    }
}
