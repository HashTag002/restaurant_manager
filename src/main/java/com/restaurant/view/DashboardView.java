package com.restaurant.view;

import com.restaurant.config.DatabaseConfig;
import com.restaurant.dao.OrderDAO;
import com.restaurant.dao.PaymentDAO;
import com.restaurant.dao.StockDAO;
import com.restaurant.dao.TableDAO;
import com.restaurant.model.Order;
import com.restaurant.model.RestaurantTable;
import com.restaurant.model.Stock;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class DashboardView extends VBox {

    private final PaymentDAO payDAO   = new PaymentDAO();
    private final OrderDAO orderDAO   = new OrderDAO();
    private final TableDAO tableDAO   = new TableDAO();
    private final StockDAO stockDAO   = new StockDAO();

    private Label lblCA     = new Label("—");
    private Label lblOrders = new Label("—");
    private Label lblTables = new Label("—");
    private Label lblAlertes = new Label("—");
    private Label lblTime   = new Label();

    private VBox recentOrdersBox = new VBox(6);
    private VBox alertesBox      = new VBox(6);

    public DashboardView() {
        setSpacing(20);
        setPadding(new Insets(24));
        setStyle("-fx-background-color: #f0f3f7;");

        Label title = new Label("🏠  Tableau de bord");
        title.setFont(Font.font("System", FontWeight.BOLD, 24));
        title.setStyle("-fx-text-fill: #2c3e50;");

        lblTime.setStyle("-fx-text-fill: #95a5a6; -fx-font-size: 12;");
        updateTime();

        HBox header = new HBox(10, title, new Region(), lblTime);
        HBox.setHgrow(header.getChildren().get(1), Priority.ALWAYS);
        header.setAlignment(Pos.CENTER_LEFT);

        getChildren().addAll(header, buildKPIRow(), buildBottomRow());
        VBox.setVgrow(buildBottomRow(), Priority.ALWAYS);
        refresh();
    }

    private HBox buildKPIRow() {
        HBox row = new HBox(16);
        row.getChildren().addAll(
                kpiCard("💰 CA Aujourd'hui",      lblCA,      "#3498db", "#ebf5fb"),
                kpiCard("📋 Commandes actives",   lblOrders,  "#27ae60", "#eafaf1"),
                kpiCard("🪑 Tables occupées",     lblTables,  "#e67e22", "#fef9e7"),
                kpiCard("⚠️ Alertes stocks",      lblAlertes, "#e74c3c", "#fdecea")
        );
        return row;
    }

    private VBox kpiCard(String label, Label valueLabel, String color, String bg) {
        valueLabel.setFont(Font.font("System", FontWeight.BOLD, 28));
        valueLabel.setStyle("-fx-text-fill: " + color + ";");
        Label lbl = new Label(label);
        lbl.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12;");

        // Accent bar
        Region bar = new Region();
        bar.setPrefHeight(4);
        bar.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 4;");

        VBox card = new VBox(8, bar, lbl, valueLabel);
        card.setPadding(new Insets(16, 16, 16, 16));
        card.setStyle("-fx-background-color: " + bg + "; -fx-background-radius: 12; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 8, 0, 0, 2);");
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    private HBox buildBottomRow() {
        // Recent orders
        Label ordTitle = new Label("📋 Commandes récentes");
        ordTitle.setFont(Font.font("System", FontWeight.BOLD, 14));
        ordTitle.setStyle("-fx-text-fill: #2c3e50;");
        ScrollPane ordScroll = new ScrollPane(recentOrdersBox);
        ordScroll.setFitToWidth(true);
        ordScroll.setStyle("-fx-background-color: transparent;");
        VBox ordCard = card(ordTitle, ordScroll);

        // Stock alerts
        Label stockTitle = new Label("⚠️ Alertes de stock");
        stockTitle.setFont(Font.font("System", FontWeight.BOLD, 14));
        stockTitle.setStyle("-fx-text-fill: #2c3e50;");
        ScrollPane stockScroll = new ScrollPane(alertesBox);
        stockScroll.setFitToWidth(true);
        stockScroll.setStyle("-fx-background-color: transparent;");
        VBox stockCard = card(stockTitle, stockScroll);

        // Refresh button
        Button refresh = new Button("🔄 Actualiser");
        refresh.setStyle("-fx-background-color: #3498db; -fx-text-fill:white; -fx-background-radius:6; -fx-padding: 8 18;");
        refresh.setOnAction(e -> refresh());

        VBox right = new VBox(10, stockCard, refresh);
        right.setAlignment(Pos.TOP_CENTER);

        HBox row = new HBox(16, ordCard, right);
        HBox.setHgrow(ordCard, Priority.ALWAYS);
        VBox.setVgrow(row, Priority.ALWAYS);
        return row;
    }

    private VBox card(Label title, javafx.scene.Node content) {
        VBox c = new VBox(10, title, content);
        c.setPadding(new Insets(16));
        c.setStyle("-fx-background-color: white; -fx-background-radius: 12; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 8, 0, 0, 2);");
        VBox.setVgrow(content, Priority.ALWAYS);
        return c;
    }

    private void refresh() {
        updateTime();
        try {
            BigDecimal ca = payDAO.getTotalJour();
            lblCA.setText(String.format("%.0f FCFA", ca));

            List<Order> active = orderDAO.getActive();
            lblOrders.setText(String.valueOf(active.size()));

            List<RestaurantTable> tables = tableDAO.getAll();
            long occ = tables.stream().filter(t -> t.getStatut() == RestaurantTable.Statut.OCCUPEE).count();
            lblTables.setText(occ + " / " + tables.size());

            List<Stock> alertes = stockDAO.getEnAlerte();
            lblAlertes.setText(String.valueOf(alertes.size()));

            // Recent orders
            recentOrdersBox.getChildren().clear();
            for (Order o : active.subList(0, Math.min(active.size(), 8))) {
                HBox row = new HBox(10);
                row.setAlignment(Pos.CENTER_LEFT);
                Label idLabel = new Label("#" + o.getId());
                idLabel.setStyle("-fx-font-weight:bold; -fx-text-fill:#2c3e50;");
                idLabel.setPrefWidth(45);
                Label tableLabel = new Label(o.getTableNumero());
                tableLabel.setPrefWidth(90);
                Label serveur = new Label(o.getServeur() != null ? o.getServeur() : "—");
                serveur.setPrefWidth(90);
                serveur.setStyle("-fx-text-fill: #7f8c8d;");
                Label statutLabel = new Label(o.getStatut().getLabel());
                statutLabel.setStyle(switch (o.getStatut()) {
                    case EN_ATTENTE -> "-fx-text-fill:#e67e22;";
                    case EN_COURS   -> "-fx-text-fill:#3498db;";
                    case SERVIE     -> "-fx-text-fill:#27ae60;";
                    default         -> "-fx-text-fill:#7f8c8d;";
                });
                row.getChildren().addAll(idLabel, tableLabel, serveur, statutLabel);
                row.setPadding(new Insets(6, 0, 6, 0));
                row.setStyle("-fx-border-color: #ecf0f1; -fx-border-width: 0 0 1 0;");
                recentOrdersBox.getChildren().add(row);
            }
            if (active.isEmpty()) {
                recentOrdersBox.getChildren().add(new Label("Aucune commande active."));
            }

            // Stock alerts
            alertesBox.getChildren().clear();
            for (Stock s : alertes.subList(0, Math.min(alertes.size(), 6))) {
                HBox row = new HBox(10);
                row.setAlignment(Pos.CENTER_LEFT);
                Label name = new Label("⚠️ " + s.getNom());
                name.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                name.setPrefWidth(180);
                Label qte = new Label(String.format("%.2f %s (seuil: %.2f)", s.getQuantite(), s.getUnite(), s.getSeuilAlerte()));
                qte.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 11;");
                row.getChildren().addAll(name, qte);
                row.setPadding(new Insets(5, 0, 5, 0));
                alertesBox.getChildren().add(row);
            }
            if (alertes.isEmpty()) {
                Label ok = new Label("✅ Tous les stocks sont OK.");
                ok.setStyle("-fx-text-fill: #27ae60;");
                alertesBox.getChildren().add(ok);
            }

        } catch (SQLException e) {
            lblCA.setText("Erreur DB");
        }
    }

    private void updateTime() {
        lblTime.setText("Dernière MAJ : " +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
    }
}
