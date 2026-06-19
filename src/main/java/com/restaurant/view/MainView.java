package com.restaurant.view;

import com.restaurant.config.DatabaseConfig;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class MainView extends BorderPane {

    private TabPane tabPane;

    public MainView() {
        setStyle("-fx-background-color: #f0f3f7;");
        buildTopBar();
        buildTabs();
    }

    private void buildTopBar() {
        Label appName = new Label("🍽️  Restaurant Manager");
        appName.setFont(Font.font("System", FontWeight.BOLD, 18));
        appName.setStyle("-fx-text-fill: white;");

        Label version = new Label("v1.0.0");
        version.setStyle("-fx-text-fill: rgba(255,255,255,0.6); -fx-font-size: 11;");

        Button btnDB = new Button("⚙️ Configuration BD");
        btnDB.setStyle("-fx-background-color: rgba(255,255,255,0.2); -fx-text-fill: white; " +
                "-fx-background-radius: 6; -fx-padding: 5 12; -fx-cursor: hand;");
        btnDB.setOnAction(e -> showDBConfig());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox topBar = new HBox(12, appName, version, spacer, btnDB);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(12, 20, 12, 20));
        topBar.setStyle("-fx-background-color: #2c3e50;");

        setTop(topBar);
    }

    private void buildTabs() {
        tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.setStyle("-fx-background-color: #f0f3f7;");

        Tab tabDash    = makeTab("🏠 Tableau de bord", new DashboardView());
        Tab tabMenu    = makeTab("🍽️ Menu",             new MenuView());
        Tab tabTables  = makeTab("🪑 Tables",           new TableView());
        Tab tabOrders  = makeTab("📋 Commandes",        new OrderView());
        Tab tabPay     = makeTab("💳 Paiements",        new PaymentView());
        Tab tabStock   = makeTab("📦 Stocks",           new StockView());
        Tab tabSales   = makeTab("📊 Rapports",         new SalesView());

        tabPane.getTabs().addAll(tabDash, tabMenu, tabTables, tabOrders, tabPay, tabStock, tabSales);
        setCenter(tabPane);
    }

    private Tab makeTab(String label, javafx.scene.Node content) {
        Tab tab = new Tab(label, content);
        return tab;
    }

    private void showDBConfig() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Configuration de la base de données");
        dialog.setHeaderText("Paramètres de connexion PostgreSQL");
        ButtonType connectBtn = new ButtonType("Connecter", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(connectBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField urlField  = new TextField("jdbc:postgresql://localhost:5432/restaurant_db");
        TextField userField = new TextField("postgres");
        PasswordField passField = new PasswordField();
        passField.setText("postgres");

        urlField.setMinWidth(300);

        grid.addRow(0, new Label("URL JDBC :"),   urlField);
        grid.addRow(1, new Label("Utilisateur :"), userField);
        grid.addRow(2, new Label("Mot de passe :"), passField);

        Label statusLabel = new Label();
        statusLabel.setStyle("-fx-text-fill: #27ae60;");
        grid.add(statusLabel, 0, 3, 2, 1);

        Button testBtn = new Button("🔌 Tester la connexion");
        testBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill:white; -fx-background-radius:5;");
        testBtn.setOnAction(e -> {
            DatabaseConfig.setCredentials(urlField.getText(), userField.getText(), passField.getText());
            if (DatabaseConfig.testConnection()) {
                statusLabel.setText("✅ Connexion réussie !");
                statusLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
            } else {
                statusLabel.setText("❌ Connexion échouée. Vérifiez les paramètres.");
                statusLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
            }
        });
        grid.add(testBtn, 0, 4, 2, 1);
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == connectBtn) {
                DatabaseConfig.setCredentials(urlField.getText(), userField.getText(), passField.getText());
            }
            return null;
        });

        dialog.showAndWait();
    }
}
