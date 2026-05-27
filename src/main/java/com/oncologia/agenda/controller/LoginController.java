package com.oncologia.agenda.controller;

import com.oncologia.agenda.model.User;
import com.oncologia.agenda.service.AuthService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

public class LoginController {
    private final AuthService authService = new AuthService();
    private final ThemeManager themeManager = new ThemeManager();

    public void show(Stage stage) {
        TextField username = new TextField("enfermera");
        username.setPromptText("Usuario");
        username.setTooltip(new Tooltip("Usuarios demo: enfermera / drlopez"));

        PasswordField password = new PasswordField();
        password.setPromptText("Contrasena");
        password.setText("1234");
        password.setTooltip(new Tooltip("Clave precargada: 1234"));

        Label error = new Label();
        error.getStyleClass().add("danger");

        Button login = new Button("Ingresar", new FontIcon(FontAwesomeSolid.SIGN_IN_ALT));
        login.setMaxWidth(Double.MAX_VALUE);
        login.setDefaultButton(true);
        login.setOnAction(event -> authenticate(stage, username.getText(), password.getText(), error));

        Label title = new Label("Agenda Oncologica");
        title.getStyleClass().add("title");
        Label subtitle = new Label("Gestion de turnos de quimioterapia");
        subtitle.getStyleClass().add("muted");

        VBox card = new VBox(12, new FontIcon(FontAwesomeSolid.CALENDAR_CHECK), title, subtitle, username, password, login, error);
        card.getStyleClass().add("login-card");
        card.setMaxWidth(380);
        card.setAlignment(Pos.CENTER);

        BorderPane root = new BorderPane(card);
        root.getStyleClass().add("app-shell");
        BorderPane.setMargin(card, new Insets(40));

        Scene scene = new Scene(root, 760, 520);
        themeManager.apply(scene);
        stage.setScene(scene);
        stage.centerOnScreen();
        stage.show();
    }

    private void authenticate(Stage stage, String username, String password, Label error) {
        authService.login(username, password).ifPresentOrElse(
                user -> openMain(stage, user),
                () -> error.setText("Usuario o contrasena invalidos.")
        );
    }

    private void openMain(Stage stage, User user) {
        new MainController(user, themeManager).show(stage);
    }
}
