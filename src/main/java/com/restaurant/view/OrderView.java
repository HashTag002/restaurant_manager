package com.restaurant.view;

import com.restaurant.dao.MenuItemDAO;
import com.restaurant.dao.OrderDAO;
import com.restaurant.dao.TableDAO;
import com.restaurant.model.*;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class OrderView extends VBox {

    private final OrderDAO orderDAO = new OrderDAO();
    private final MenuItemDAO menuDAO = new MenuItemDAO();
    private final TableDAO tableDAO = new TableDAO();

    private final javafx.scene.control.TableView<Order> orderTable = new javafx.scene.control.TableView<>();
    private final ObservableList<Order> orders = FXCollections.observableArrayList();
    private final ComboBox<String> filterStatut = new ComboBox<>();

    public OrderView() {
        setSpacing(10);
        setPadding(new Insets(20));
        setStyle("-fx-background-color: #f8f9fa;");

        Label title = new Label("📋  Gestion des Commandes");
        title.setFont(Font.font("System", FontWeight.BOLD, 22));
        title.setStyle("-fx-text-fill: #2c3e50;");

        buildTable();
        getChildren().addAll(title, buildToolbar(), orderTable);
        VBox.setVgrow(orderTable, Priority.ALWAYS);
        loadData();
    }

    private HBox buildToolbar() {
        filterStatut.getItems().addAll("Toutes", "EN_ATTENTE", "EN_COURS", "SERVIE", "PAYEE", "ANNULEE");
        filterStatut.setValue("Toutes");
        filterStatut.setOnAction(e -> loadData());

        Button btnNew    = btn("➕ Nouvelle commande", "#27ae60");
        Button btnEdit   = btn("✏️ Modifier",          "#2980b9");
        Button btnStatus = btn("🔁 Changer statut",   "#8e44ad");
        Button btnDel    = btn("🗑️ Supprimer",        "#e74c3c");
        Button btnRefresh = btn("🔄 Actualiser",      "#95a5a6");

        btnNew.setOnAction(e -> showOrderDialog(null));
        btnEdit.setOnAction(e -> {
            Order sel = orderTable.getSelectionModel().getSelectedItem();
            if (sel != null) showOrderDialog(sel);
            else alert("Sélectionnez une commande.");
        });
        btnStatus.setOnAction(e -> changeStatus());
        btnDel.setOnAction(e -> deleteSelected());
        btnRefresh.setOnAction(e -> loadData());

        HBox bar = new HBox(10, new Label("Filtre:"), filterStatut, btnNew, btnEdit, btnStatus, btnDel, new Region(), btnRefresh);
        HBox.setHgrow(bar.getChildren().get(6), Priority.ALWAYS);
        bar.setAlignment(Pos.CENTER_LEFT);
        return bar;
    }

    private void buildTable() {
        TableColumn<Order, String> colId     = col("N°", 60);
        colId.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getId())));

        TableColumn<Order, String> colTable  = col("Table", 100);
        colTable.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTableNumero()));

        TableColumn<Order, String> colServeur = col("Serveur", 120);
        colServeur.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getServeur()));

        TableColumn<Order, String> colStatut = col("Statut", 130);
        colStatut.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatut().getLabel()));

        TableColumn<Order, String> colDate   = col("Date/Heure", 150);
        colDate.setCellValueFactory(d -> {
            var dt = d.getValue().getCreatedAt();
            return new SimpleStringProperty(dt != null ? dt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "");
        });

        TableColumn<Order, String> colNote   = col("Note", 200);
        colNote.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNote()));

        orderTable.setEditable(false);
        orderTable.getColumns().addAll(colId, colTable, colServeur, colStatut, colDate, colNote);
        orderTable.setItems(orders);
        orderTable.setColumnResizePolicy(javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY);
        orderTable.setRowFactory(tv -> {
            var row = new TableRow<Order>();
            row.itemProperty().addListener((obs, old, o) -> {
                if (o == null) { row.setStyle(""); return; }
                row.setStyle(switch (o.getStatut()) {
                    case EN_ATTENTE -> "-fx-background-color: #fff9e6;";
                    case EN_COURS   -> "-fx-background-color: #e8f4fd;";
                    case SERVIE     -> "-fx-background-color: #eafaf1;";
                    case PAYEE      -> "-fx-background-color: #f0f0f0;";
                    case ANNULEE    -> "-fx-background-color: #fdecea;";
                });
            });
            return row;
        });
    }

    private void loadData() {
        try {
            List<Order> all = orderDAO.getAll();
            String filter = filterStatut.getValue();
            if (!"Toutes".equals(filter)) {
                all.removeIf(o -> !o.getStatut().name().equals(filter));
            }
            orders.setAll(all);
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, "Erreur : " + e.getMessage()).showAndWait();
        }
    }

    private void showOrderDialog(Order existing) {
        Dialog<Order> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Nouvelle commande" : "Modifier la commande");
        dialog.getDialogPane().setPrefSize(700, 550);
        ButtonType saveBtn = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        // Left: order info + item selector
        ComboBox<RestaurantTable> tableBox = new ComboBox<>();
        TextField serveurField = new TextField();
        TextArea noteArea = new TextArea();
        noteArea.setPrefRowCount(2);

        try {
            tableBox.setItems(FXCollections.observableArrayList(tableDAO.getAll()));
        } catch (SQLException e) { error(e.getMessage()); }

        // Items in current order
        ObservableList<OrderItem> orderItems = FXCollections.observableArrayList();
        javafx.scene.control.TableView<OrderItem> itemTable = new javafx.scene.control.TableView<>(orderItems);

        TableColumn<OrderItem, String> cNom = new TableColumn<>("Article");
        cNom.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getMenuItemNom()));
        cNom.setPrefWidth(200);
        TableColumn<OrderItem, String> cQty = new TableColumn<>("Qté");
        cQty.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getQuantite())));
        cQty.setPrefWidth(50);
        TableColumn<OrderItem, String> cPrix = new TableColumn<>("Prix");
        cPrix.setCellValueFactory(d -> new SimpleStringProperty(String.format("%.0f FCFA", d.getValue().getPrixUnitaire())));
        cPrix.setPrefWidth(80);
        TableColumn<OrderItem, String> cTotal = new TableColumn<>("S-total");
        cTotal.setCellValueFactory(d -> new SimpleStringProperty(String.format("%.0f FCFA", d.getValue().getSousTotal())));
        cTotal.setPrefWidth(80);
        itemTable.getColumns().addAll(cNom, cQty, cPrix, cTotal);
        itemTable.setPrefHeight(200);

        // Menu item selector
        ComboBox<com.restaurant.model.MenuItem> menuCombo = new ComboBox<>();
        try { menuCombo.setItems(FXCollections.observableArrayList(menuDAO.getAvailableMenuItems())); }
        catch (SQLException e) { error(e.getMessage()); }
        Spinner<Integer> qtySpinner = new Spinner<>(1, 99, 1);
        qtySpinner.setPrefWidth(70);
        Button btnAddItem = new Button("Ajouter");
        btnAddItem.setStyle("-fx-background-color:#27ae60; -fx-text-fill:white; -fx-background-radius:5;");
        Button btnRemItem = new Button("Retirer");
        btnRemItem.setStyle("-fx-background-color:#e74c3c; -fx-text-fill:white; -fx-background-radius:5;");

        btnAddItem.setOnAction(e -> {
            com.restaurant.model.MenuItem sel = menuCombo.getValue();
            if (sel == null) return;
            // Check if already present
            for (OrderItem oi : orderItems) {
                if (oi.getMenuItemId() == sel.getId()) {
                    oi.setQuantite(oi.getQuantite() + qtySpinner.getValue());
                    itemTable.refresh();
                    return;
                }
            }
            orderItems.add(new OrderItem(sel.getId(), sel.getNom(), qtySpinner.getValue(), sel.getPrix()));
        });
        btnRemItem.setOnAction(e -> {
            OrderItem sel = itemTable.getSelectionModel().getSelectedItem();
            if (sel != null) orderItems.remove(sel);
        });

        if (existing != null) {
            try {
                Order full = orderDAO.getById(existing.getId());
                orderItems.setAll(full.getItems());
                tableBox.getItems().stream()
                        .filter(t -> t.getId() == existing.getTableId())
                        .findFirst().ifPresent(tableBox::setValue);
                serveurField.setText(existing.getServeur() != null ? existing.getServeur() : "");
                noteArea.setText(existing.getNote() != null ? existing.getNote() : "");
            } catch (SQLException ex) { error(ex.getMessage()); }
        }

        GridPane form = new GridPane();
        form.setHgap(10); form.setVgap(8);
        form.setPadding(new Insets(10));
        form.addRow(0, new Label("Table :"), tableBox);
        form.addRow(1, new Label("Serveur :"), serveurField);
        form.addRow(2, new Label("Note :"), noteArea);
        tableBox.setMaxWidth(Double.MAX_VALUE);
        serveurField.setMaxWidth(Double.MAX_VALUE);

        HBox addBar = new HBox(8, menuCombo, new Label("x"), qtySpinner, btnAddItem, btnRemItem);
        addBar.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(menuCombo, Priority.ALWAYS);

        VBox content = new VBox(10, form, new Label("Articles de la commande :"), addBar, itemTable);
        content.setPadding(new Insets(10));
        dialog.getDialogPane().setContent(content);

        dialog.setResultConverter(btn -> {
            if (btn == saveBtn) {
                if (tableBox.getValue() == null) { alert("Choisissez une table."); return null; }
                if (orderItems.isEmpty()) { alert("Ajoutez au moins un article."); return null; }
                Order o = existing != null ? existing : new Order();
                o.setTableId(tableBox.getValue().getId());
                o.setServeur(serveurField.getText().trim());
                o.setNote(noteArea.getText().trim());
                o.setItems(new ArrayList<>(orderItems));
                return o;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(o -> {
            if (o != null) {
                try { orderDAO.save(o); loadData(); }
                catch (SQLException ex) { error(ex.getMessage()); }
            }
        });
    }

    private void changeStatus() {
        Order sel = orderTable.getSelectionModel().getSelectedItem();
        if (sel == null) { alert("Sélectionnez une commande."); return; }
        ChoiceDialog<Order.Statut> d = new ChoiceDialog<>(sel.getStatut(), Order.Statut.values());
        d.setTitle("Changer le statut");
        d.setHeaderText("Nouveau statut pour la commande #" + sel.getId());
        d.showAndWait().ifPresent(newStatut -> {
            try { orderDAO.updateStatut(sel.getId(), newStatut); loadData(); }
            catch (SQLException ex) { error(ex.getMessage()); }
        });
    }

    private void deleteSelected() {
        Order sel = orderTable.getSelectionModel().getSelectedItem();
        if (sel == null) { alert("Sélectionnez une commande."); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer la commande #" + sel.getId() + " ?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.YES) {
                try { orderDAO.delete(sel.getId()); loadData(); }
                catch (SQLException ex) { error(ex.getMessage()); }
            }
        });
    }

    private <T> TableColumn<T, String> col(String name, double width) {
        TableColumn<T, String> c = new TableColumn<>(name);
        c.setPrefWidth(width);
        return c;
    }

    private Button btn(String text, String color) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color:" + color + "; -fx-text-fill:white; -fx-background-radius:5; -fx-padding:6 12;");
        return b;
    }

    private void alert(String msg) { new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK).showAndWait(); }
    private void error(String msg) { new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait(); }
}
