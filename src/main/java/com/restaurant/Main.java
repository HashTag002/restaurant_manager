package com.restaurant;

import com.restaurant.config.DatabaseConfig;
import com.restaurant.view.MainView;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        // Show DB connection dialog first
        if (!showConnectionDialog(primaryStage)) {
            Platform.exit();
            return;
        }

        // Build main window
        MainView mainView = new MainView();
        Scene scene = new Scene(mainView, 1280, 800);

        primaryStage.setTitle("Restaurant Manager — Gestion");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);
        primaryStage.show();

        primaryStage.setOnCloseRequest(e -> {
            DatabaseConfig.closeConnection();
            Platform.exit();
        });
    }

    private boolean showConnectionDialog(Stage owner) {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Restaurant Manager — Connexion");
        dialog.setHeaderText(null);
        if (owner != null && owner.getScene() != null) {
            dialog.initOwner(owner);
        }

        ButtonType connectBtn = new ButtonType("Se connecter", ButtonBar.ButtonData.OK_DONE);
        ButtonType quitBtn    = new ButtonType("Quitter",      ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(connectBtn, quitBtn);
        dialog.getDialogPane().setMinWidth(440);

        // Header
        Label title = new Label("🍽️  Restaurant Manager");
        title.setFont(Font.font("System", FontWeight.BOLD, 22));
        title.setStyle("-fx-text-fill: #2c3e50;");
        Label subtitle = new Label("Connectez-vous à votre base de données PostgreSQL");
        subtitle.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12;");

        // Form
        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(12);
        grid.setPadding(new Insets(20, 20, 10, 20));

        TextField hostField = new TextField("localhost");
        TextField portField = new TextField("5432");
        TextField dbField   = new TextField("restaurant_db");
        TextField userField = new TextField("postgres");
        PasswordField passField = new PasswordField();
        passField.setPromptText("Mot de passe PostgreSQL");

        hostField.setMaxWidth(Double.MAX_VALUE);
        portField.setMaxWidth(80);
        dbField.setMaxWidth(Double.MAX_VALUE);
        userField.setMaxWidth(Double.MAX_VALUE);
        passField.setMaxWidth(Double.MAX_VALUE);

        // Read env vars if set
        String envUrl = System.getenv("DB_URL");
        if (envUrl != null && envUrl.contains("://")) {
            try {
                String[] parts = envUrl.replace("jdbc:postgresql://", "").split("/");
                String[] hostPort = parts[0].split(":");
                hostField.setText(hostPort[0]);
                if (hostPort.length > 1) portField.setText(hostPort[1]);
                if (parts.length > 1) dbField.setText(parts[1]);
            } catch (Exception ignored) {}
        }
        String envUser = System.getenv("DB_USER");
        if (envUser != null) userField.setText(envUser);

        Label statusLabel = new Label();
        statusLabel.setWrapText(true);

        Button testBtn = new Button("🔌 Tester la connexion");
        testBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill:white; -fx-background-radius:6; -fx-padding:6 16;");
        testBtn.setOnAction(e -> {
            applyCredentials(hostField, portField, dbField, userField, passField);
            if (DatabaseConfig.testConnection()) {
                statusLabel.setText("✅ Connexion réussie !");
                statusLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
            } else {
                statusLabel.setText("❌ Connexion échouée. Vérifiez l'hôte, le port, la base et les identifiants.");
                statusLabel.setStyle("-fx-text-fill: #e74c3c;");
            }
        });

        grid.addRow(0, new Label("Hôte :"),         hostField);
        grid.addRow(1, new Label("Port :"),         portField);
        grid.addRow(2, new Label("Base de données :"), dbField);
        grid.addRow(3, new Label("Utilisateur :"),  userField);
        grid.addRow(4, new Label("Mot de passe :"), passField);
        grid.add(testBtn,      0, 5, 2, 1);
        grid.add(statusLabel,  0, 6, 2, 1);

        VBox content = new VBox(12, title, subtitle, new Separator(), grid);
        content.setPadding(new Insets(20));
        dialog.getDialogPane().setContent(content);

        dialog.setResultConverter(btn -> {
            if (btn == connectBtn) {
                applyCredentials(hostField, portField, dbField, userField, passField);
                return DatabaseConfig.testConnection();
            }
            return false;
        });

        var result = dialog.showAndWait();
        return result.isPresent() && Boolean.TRUE.equals(result.get());
    }

    private void applyCredentials(TextField host, TextField port, TextField db,
                                   TextField user, PasswordField pass) {
        String url = "jdbc:postgresql://" + host.getText().trim() +
                ":" + port.getText().trim() +
                "/" + db.getText().trim();
        DatabaseConfig.setCredentials(url, user.getText().trim(), pass.getText());
    }

    public static void main(String[] args) {
        launch(args);
    }
}
