package com.restaurant.view;

import com.restaurant.dao.TableDAO;
import com.restaurant.model.RestaurantTable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.sql.SQLException;
import java.util.List;

public class TableView extends VBox {

    private final TableDAO dao = new TableDAO();
    private FlowPane tableGrid;

    public TableView() {
        setSpacing(10);
        setPadding(new Insets(20));
        setStyle("-fx-background-color: #f8f9fa;");

        Label title = new Label("🪑  Gestion des Tables");
        title.setFont(Font.font("System", FontWeight.BOLD, 22));
        title.setStyle("-fx-text-fill: #2c3e50;");

        getChildren().addAll(title, buildToolbar(), buildLegend(), buildGrid());
        VBox.setVgrow(tableGrid, Priority.ALWAYS);
        loadData();
    }

    private HBox buildToolbar() {
        Button btnAdd    = btn("➕ Ajouter une table", "#27ae60");
        Button btnRefresh = btn("🔄 Actualiser", "#95a5a6");

        btnAdd.setOnAction(e -> showTableDialog(null));
        btnRefresh.setOnAction(e -> loadData());

        HBox bar = new HBox(10, btnAdd, btnRefresh);
        bar.setAlignment(Pos.CENTER_LEFT);
        return bar;
    }

    private HBox buildLegend() {
        HBox leg = new HBox(20);
        leg.setAlignment(Pos.CENTER_LEFT);
        leg.getChildren().addAll(
                legendItem("🟢", "Libre",    "#27ae60"),
                legendItem("🔴", "Occupée",  "#e74c3c"),
                legendItem("🟡", "Réservée", "#f39c12")
        );
        return leg;
    }

    private HBox legendItem(String emoji, String label, String color) {
        Label l = new Label(emoji + " " + label);
        l.setStyle("-fx-text-fill:" + color + "; -fx-font-weight:bold;");
        return new HBox(l);
    }

    private ScrollPane buildGrid() {
        tableGrid = new FlowPane();
        tableGrid.setHgap(15);
        tableGrid.setVgap(15);
        tableGrid.setPadding(new Insets(10));

        ScrollPane sp = new ScrollPane(tableGrid);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color: transparent;");
        VBox.setVgrow(sp, Priority.ALWAYS);
        return sp;
    }

    private void loadData() {
        tableGrid.getChildren().clear();
        try {
            List<RestaurantTable> tables = dao.getAll();
            for (RestaurantTable t : tables) {
                tableGrid.getChildren().add(buildTableCard(t));
            }
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, "Erreur : " + e.getMessage()).showAndWait();
        }
    }

    private VBox buildTableCard(RestaurantTable t) {
        String color = switch (t.getStatut()) {
            case LIBRE    -> "#27ae60";
            case OCCUPEE  -> "#e74c3c";
            case RESERVEE -> "#f39c12";
        };

        VBox card = new VBox(8);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(16));
        card.setPrefSize(130, 130);
        card.setStyle("""
                -fx-background-color: %s;
                -fx-background-radius: 12;
                -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.18), 6, 0, 0, 2);
                -fx-cursor: hand;
                """.formatted(color));

        Text num    = new Text("Table " + t.getNumero());
        num.setFont(Font.font("System", FontWeight.BOLD, 16));
        num.setFill(Color.WHITE);

        Text cap    = new Text(t.getCapacite() + " personnes");
        cap.setFont(Font.font(11));
        cap.setFill(Color.WHITE);

        Text zone   = new Text(t.getZone());
        zone.setFont(Font.font(10));
        zone.setFill(Color.web("#ffffff99"));

        Text statut = new Text(t.getStatut().name());
        statut.setFont(Font.font("System", FontWeight.BOLD, 11));
        statut.setFill(Color.WHITE);

        card.getChildren().addAll(num, cap, zone, statut);

        // Context menu
        ContextMenu ctx = new ContextMenu();
        MenuItem editItem   = new MenuItem("✏️ Modifier");
        MenuItem libreItem  = new MenuItem("🟢 Marquer Libre");
        MenuItem occupeItem = new MenuItem("🔴 Marquer Occupée");
        MenuItem resvItem   = new MenuItem("🟡 Marquer Réservée");
        MenuItem delItem    = new MenuItem("🗑️ Supprimer");

        editItem.setOnAction(e -> showTableDialog(t));
        libreItem.setOnAction(e  -> updateStatut(t, RestaurantTable.Statut.LIBRE));
        occupeItem.setOnAction(e -> updateStatut(t, RestaurantTable.Statut.OCCUPEE));
        resvItem.setOnAction(e   -> updateStatut(t, RestaurantTable.Statut.RESERVEE));
        delItem.setOnAction(e    -> deleteTable(t));

        ctx.getItems().addAll(editItem, new SeparatorMenuItem(), libreItem, occupeItem, resvItem, new SeparatorMenuItem(), delItem);
        card.setOnContextMenuRequested(e -> ctx.show(card, e.getScreenX(), e.getScreenY()));
        card.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) showTableDialog(t);
        });

        return card;
    }

    private void showTableDialog(RestaurantTable existing) {
        Dialog<RestaurantTable> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Nouvelle table" : "Modifier la table");
        ButtonType saveBtn = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField numField = new TextField();
        TextField capField = new TextField();
        TextField zoneField = new TextField();
        ComboBox<RestaurantTable.Statut> statutBox = new ComboBox<>();
        statutBox.getItems().addAll(RestaurantTable.Statut.values());

        if (existing != null) {
            numField.setText(String.valueOf(existing.getNumero()));
            capField.setText(String.valueOf(existing.getCapacite()));
            zoneField.setText(existing.getZone());
            statutBox.setValue(existing.getStatut());
        } else {
            statutBox.setValue(RestaurantTable.Statut.LIBRE);
            zoneField.setText("Salle");
        }

        grid.addRow(0, new Label("Numéro :"), numField);
        grid.addRow(1, new Label("Capacité :"), capField);
        grid.addRow(2, new Label("Zone :"), zoneField);
        grid.addRow(3, new Label("Statut :"), statutBox);
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == saveBtn) {
                RestaurantTable t = existing != null ? existing : new RestaurantTable();
                try {
                    t.setNumero(Integer.parseInt(numField.getText().trim()));
                    t.setCapacite(Integer.parseInt(capField.getText().trim()));
                } catch (NumberFormatException ex) {
                    new Alert(Alert.AlertType.ERROR, "Numéro et capacité doivent être des entiers.").showAndWait();
                    return null;
                }
                t.setZone(zoneField.getText().trim());
                t.setStatut(statutBox.getValue());
                return t;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(t -> {
            if (t != null) {
                try { dao.save(t); loadData(); }
                catch (SQLException ex) { new Alert(Alert.AlertType.ERROR, ex.getMessage()).showAndWait(); }
            }
        });
    }

    private void updateStatut(RestaurantTable t, RestaurantTable.Statut statut) {
        try { dao.updateStatut(t.getId(), statut); loadData(); }
        catch (SQLException ex) { new Alert(Alert.AlertType.ERROR, ex.getMessage()).showAndWait(); }
    }

    private void deleteTable(RestaurantTable t) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer la table " + t.getNumero() + " ?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.YES) {
                try { dao.delete(t.getId()); loadData(); }
                catch (SQLException ex) { new Alert(Alert.AlertType.ERROR, ex.getMessage()).showAndWait(); }
            }
        });
    }

    private Button btn(String text, String color) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color:" + color + "; -fx-text-fill:white; -fx-background-radius:5; -fx-padding:6 14;");
        return b;
    }
}
