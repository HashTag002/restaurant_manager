package com.restaurant.view;

import com.restaurant.dao.OrderDAO;
import com.restaurant.dao.PaymentDAO;
import com.restaurant.dao.TableDAO;
import com.restaurant.model.Order;
import com.restaurant.model.OrderItem;
import com.restaurant.model.Payment;
import com.restaurant.util.InvoiceGenerator;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.awt.Desktop;
import java.io.File;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class PaymentView extends VBox {

    private final PaymentDAO payDAO = new PaymentDAO();
    private final OrderDAO orderDAO = new OrderDAO();

    private final javafx.scene.control.TableView<Payment> payTable = new javafx.scene.control.TableView<>();
    private final ObservableList<Payment> payments = FXCollections.observableArrayList();

    // Summary cards
    private Label lblJour   = new Label("0 FCFA");
    private Label lblSem    = new Label("0 FCFA");
    private Label lblMois   = new Label("0 FCFA");

    public PaymentView() {
        setSpacing(10);
        setPadding(new Insets(20));
        setStyle("-fx-background-color: #f8f9fa;");

        Label title = new Label("💳  Paiements & Encaissements");
        title.setFont(Font.font("System", FontWeight.BOLD, 22));
        title.setStyle("-fx-text-fill: #2c3e50;");

        buildPayTable();
        getChildren().addAll(title, buildSummaryCards(), buildToolbar(), payTable);
        VBox.setVgrow(payTable, Priority.ALWAYS);
        loadData();
    }

    private HBox buildSummaryCards() {
        HBox row = new HBox(15);
        row.getChildren().addAll(
                summaryCard("Aujourd'hui", lblJour,   "#3498db"),
                summaryCard("7 derniers jours", lblSem,  "#27ae60"),
                summaryCard("Ce mois", lblMois,   "#8e44ad")
        );
        return row;
    }

    private VBox summaryCard(String label, Label valueLabel, String color) {
        valueLabel.setFont(Font.font("System", FontWeight.BOLD, 22));
        valueLabel.setStyle("-fx-text-fill: " + color + ";");
        Label lbl = new Label(label);
        lbl.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 11;");
        VBox card = new VBox(4, lbl, valueLabel);
        card.setPadding(new Insets(14));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.10), 6, 0, 0, 2);");
        card.setPrefWidth(200);
        return card;
    }

    private HBox buildToolbar() {
        Button btnPay     = btn("💳 Encaisser commande", "#27ae60");
        Button btnInvoice = btn("🧾 Générer facture",    "#f39c12");
        Button btnRefresh = btn("🔄 Actualiser",         "#95a5a6");

        btnPay.setOnAction(e -> showPaymentDialog());
        btnInvoice.setOnAction(e -> generateInvoice());
        btnRefresh.setOnAction(e -> loadData());

        HBox bar = new HBox(10, btnPay, btnInvoice, new Region(), btnRefresh);
        HBox.setHgrow(bar.getChildren().get(2), Priority.ALWAYS);
        bar.setAlignment(Pos.CENTER_LEFT);
        return bar;
    }

    private void buildPayTable() {
        TableColumn<Payment, String> colId    = col("N°", 55);
        colId.setCellValueFactory(d -> new SimpleStringProperty("#" + d.getValue().getCommandeId()));

        TableColumn<Payment, String> colTable = col("Table", 90);
        colTable.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTableNumero()));

        TableColumn<Payment, String> colMontant = col("Montant", 100);
        colMontant.setCellValueFactory(d -> new SimpleStringProperty(String.format("%.0f FCFA", d.getValue().getMontant())));
        colMontant.setStyle("-fx-alignment: CENTER-RIGHT;");

        TableColumn<Payment, String> colMethode = col("Méthode", 150);
        colMethode.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getMethode().getLabel()));

        TableColumn<Payment, String> colRef = col("Référence", 130);
        colRef.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getReference()));

        TableColumn<Payment, String> colDate = col("Date", 150);
        colDate.setCellValueFactory(d -> {
            var dt = d.getValue().getCreatedAt();
            return new SimpleStringProperty(dt != null ? dt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "");
        });

        payTable.getColumns().addAll(colId, colTable, colMontant, colMethode, colRef, colDate);
        payTable.setItems(payments);
        payTable.setColumnResizePolicy(javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void loadData() {
        try {
            payments.setAll(payDAO.getAll());
            lblJour.setText(String.format("%.0f FCFA", payDAO.getTotalJour()));
            lblSem.setText(String.format("%.0f FCFA",  payDAO.getTotalSemaine()));
            lblMois.setText(String.format("%.0f FCFA", payDAO.getTotalMois()));
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, "Erreur : " + e.getMessage()).showAndWait();
        }
    }

    private void showPaymentDialog() {
        Dialog<Payment> dialog = new Dialog<>();
        dialog.setTitle("Encaisser une commande");
        dialog.getDialogPane().setPrefSize(520, 480);
        ButtonType payBtn = new ButtonType("Encaisser", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(payBtn, ButtonType.CANCEL);

        ComboBox<Order> orderBox = new ComboBox<>();
        try {
            List<Order> active = orderDAO.getActive();
            active.removeIf(o -> o.getStatut() == com.restaurant.model.Order.Statut.PAYEE);
            orderBox.setItems(FXCollections.observableArrayList(active));
            orderBox.setConverter(new javafx.util.StringConverter<>() {
                public String toString(Order o) { return o == null ? "" : "#" + o.getId() + " — " + o.getTableNumero(); }
                public Order fromString(String s) { return null; }
            });
        } catch (SQLException e) { error(e.getMessage()); }

        javafx.scene.control.TableView<OrderItem> itemsView = new javafx.scene.control.TableView<>();
        ObservableList<OrderItem> itemsObs = FXCollections.observableArrayList();
        itemsView.setItems(itemsObs);
        TableColumn<OrderItem, String> cNom = new TableColumn<>("Article");
        cNom.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getMenuItemNom()));
        cNom.setPrefWidth(200);
        TableColumn<OrderItem, String> cQ = new TableColumn<>("Qté");
        cQ.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getQuantite())));
        TableColumn<OrderItem, String> cP = new TableColumn<>("S-total");
        cP.setCellValueFactory(d -> new SimpleStringProperty(String.format("%.0f FCFA", d.getValue().getSousTotal())));
        itemsView.getColumns().addAll(cNom, cQ, cP);
        itemsView.setPrefHeight(150);

        Label lblTotal = new Label("Total : 0 FCFA");
        lblTotal.setFont(Font.font("System", FontWeight.BOLD, 16));
        lblTotal.setStyle("-fx-text-fill: #27ae60;");

        orderBox.setOnAction(e -> {
            Order sel = orderBox.getValue();
            if (sel == null) return;
            try {
                Order full = orderDAO.getById(sel.getId());
                itemsObs.setAll(full.getItems());
                lblTotal.setText(String.format("Total : %.0f FCFA", full.getTotal()));
            } catch (SQLException ex) { error(ex.getMessage()); }
        });

        ComboBox<Payment.Methode> methodeBox = new ComboBox<>(FXCollections.observableArrayList(Payment.Methode.values()));
        methodeBox.setValue(Payment.Methode.ESPECES);

        TextField montantRecuField = new TextField();
        montantRecuField.setPromptText("Montant remis par le client");
        Label lblMonnaie = new Label("Monnaie : 0 FCFA");

        montantRecuField.textProperty().addListener((obs, old, val) -> {
            Order sel = orderBox.getValue();
            if (sel == null) return;
            try {
                Order full = orderDAO.getById(sel.getId());
                BigDecimal recu = new BigDecimal(val.replace(",", "."));
                BigDecimal monnaie = recu.subtract(full.getTotal());
                lblMonnaie.setText("Monnaie : " + String.format("%.0f FCFA", monnaie.max(BigDecimal.ZERO)));
            } catch (Exception ignored) {}
        });

        TextField refField = new TextField();
        refField.setPromptText("Référence / N° transaction");

        GridPane form = new GridPane();
        form.setHgap(10); form.setVgap(10);
        form.setPadding(new Insets(10));
        form.addRow(0, new Label("Commande :"),   orderBox);
        form.addRow(1, new Label("Articles :"),   itemsView);
        form.addRow(2, new Label(""),             lblTotal);
        form.addRow(3, new Label("Méthode :"),    methodeBox);
        form.addRow(4, new Label("Reçu (FCFA) :"),   montantRecuField);
        form.addRow(5, new Label(""),             lblMonnaie);
        form.addRow(6, new Label("Référence :"),  refField);
        orderBox.setMaxWidth(Double.MAX_VALUE);
        methodeBox.setMaxWidth(Double.MAX_VALUE);
        dialog.getDialogPane().setContent(form);

        dialog.setResultConverter(btn -> {
            if (btn == payBtn) {
                Order sel = orderBox.getValue();
                if (sel == null) { alert("Choisissez une commande."); return null; }
                Payment p = new Payment();
                p.setCommandeId(sel.getId());
                p.setMethode(methodeBox.getValue());
                p.setReference(refField.getText().trim());
                try {
                    Order full = orderDAO.getById(sel.getId());
                    p.setMontant(full.getTotal());
                    if (!montantRecuField.getText().isBlank()) {
                        BigDecimal recu = new BigDecimal(montantRecuField.getText().replace(",", "."));
                        p.setMontantRecu(recu);
                        p.setMonnaie(recu.subtract(full.getTotal()).max(BigDecimal.ZERO));
                    }
                } catch (Exception ex) { error(ex.getMessage()); return null; }
                return p;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(p -> {
            if (p != null) {
                try {
                    payDAO.save(p);
                    orderDAO.updateStatut(p.getCommandeId(), Order.Statut.PAYEE);
                    alert("✅ Commande encaissée avec succès !");
                    loadData();
                } catch (SQLException ex) { error(ex.getMessage()); }
            }
        });
    }

    private void generateInvoice() {
        Payment sel = payTable.getSelectionModel().getSelectedItem();
        if (sel == null) { alert("Sélectionnez un paiement pour générer la facture."); return; }

        try {
            Order order = orderDAO.getById(sel.getCommandeId());
            if (order == null) { error("Commande introuvable."); return; }
            order.setItems(orderDAO.getItemsByOrderId(order.getId()));

            String dir = System.getProperty("user.home") + "/Desktop";
            File destDir = new File(dir);
            if (!destDir.exists()) dir = System.getProperty("user.home");

            String path = InvoiceGenerator.generate(order, sel, dir);
            alert("✅ Facture générée :\n" + path);

            try {
                if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(new File(path));
            } catch (Exception ignored) {}
        } catch (Exception e) {
            error("Erreur génération facture : " + e.getMessage());
        }
    }

    private Button btn(String text, String color) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color:" + color + "; -fx-text-fill:white; -fx-background-radius:5; -fx-padding:6 14;");
        return b;
    }

    private <T> TableColumn<T, String> col(String name, double w) {
        TableColumn<T, String> c = new TableColumn<>(name);
        c.setPrefWidth(w);
        return c;
    }

    private void alert(String msg) { new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK).showAndWait(); }
    private void error(String msg) { new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait(); }
}
