package com.oncologia.agenda;

import com.oncologia.agenda.controller.LoginController;
import com.oncologia.agenda.dao.Database;
import javafx.application.Application;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.InputStream;

public class AgendaApplication extends Application {
    @Override
    public void start(Stage primaryStage) {
        Database.initialize();
        primaryStage.setTitle("Agenda Oncologica - Quimioterapia");
        InputStream iconStream = getClass().getResourceAsStream("/icons/app-icon.png");
        if (iconStream != null) {
            Image icon = new Image(iconStream);
            if (!icon.isError()) {
                primaryStage.getIcons().add(icon);
            }
        }
        new LoginController().show(primaryStage);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
