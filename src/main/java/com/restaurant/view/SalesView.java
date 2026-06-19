package com.restaurant.view;

import com.restaurant.dao.PaymentDAO;
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
import java.util.List;
import java.util.Map;

public class SalesView extends VBox {

    private final PaymentDAO dao = new PaymentDAO();

    private final Label lblCA   = new Label("0 FCFA");
    private final Label lblSem  = new Label("0 FCFA");
    private final Label lblMois = new Label("0 FCFA");

    private final javafx.scene.control.TableView<Object[]> ventesTable  = new javafx.scene.control.TableView<>();
    private final ObservableList<Object[]> ventesData = FXCollections.observableArrayList();

    private final javafx.scene.control.TableView<Object[]> topTable = new javafx.scene.control.TableView<>();
    private final ObservableList<Object[]> topData = FXCollections.observableArrayList();

    private final javafx.scene.control.TableView<String[]> methodeTable = new javafx.scene.control.TableView<>();
    private final ObservableList<String[]> methodeData = FXCollections.observableArrayList();

    private final Spinner<Integer> nbJoursSpinner = new Spinner<>(7, 90, 30);

    public SalesView() {
        setSpacing(15);
        setPadding(new Insets(20));
        setStyle("-fx-background-color: #f8f9fa;");

        Label title = new Label("📊  Suivi des Ventes & Rapports");
        title.setFont(Font.font("System", FontWeight.BOLD, 22));
        title.setStyle("-fx-text-fill: #2c3e50;");

        buildVentesTable();
        buildTopTable();
        buildMethodeTable();

        ScrollPane bottomScroll = new ScrollPane(buildTablesSection());
        bottomScroll.setFitToWidth(true);
        bottomScroll.setStyle("-fx-background-color: transparent;");
        VBox.setVgrow(bottomScroll, Priority.ALWAYS);

        getChildren().addAll(title, buildKPIRow(), buildControlBar(), bottomScroll);
        loadData();
    }

    private HBox buildKPIRow() {
        HBox row = new HBox(15);
        row.getChildren().addAll(
                kpiCard("📅 CA Aujourd'hui",   lblCA,   "#3498db"),
                kpiCard("📆 CA 7 jours",       lblSem,  "#27ae60"),
                kpiCard("🗓️ CA Ce mois",       lblMois, "#e67e22")
        );
        return row;
    }

    private VBox kpiCard(String label, Label valueLabel, String color) {
        valueLabel.setFont(Font.font("System", FontWeight.BOLD, 24));
        valueLabel.setStyle("-fx-text-fill: " + color + ";");
        Label lbl = new Label(label);
        lbl.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12;");
        VBox card = new VBox(4, lbl, valueLabel);
        card.setPadding(new Insets(16));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.10), 6, 0, 0, 2);");
        card.setPrefWidth(220);
        return card;
    }

    private HBox buildControlBar() {
        nbJoursSpinner.setPrefWidth(80);
        Label lbl = new Label("Période d'analyse :");
        Label lblJ = new Label("jours");
        Button btnRefresh = new Button("🔄 Actualiser");
        btnRefresh.setStyle("-fx-background-color:#3498db; -fx-text-fill:white; -fx-background-radius:5; -fx-padding:6 14;");
        btnRefresh.setOnAction(e -> loadData());

        HBox bar = new HBox(10, lbl, nbJoursSpinner, lblJ, new Region(), btnRefresh);
        HBox.setHgrow(bar.getChildren().get(3), Priority.ALWAYS);
        bar.setAlignment(Pos.CENTER_LEFT);
        return bar;
    }

    private void buildVentesTable() {
        TableColumn<Object[], String> cDate = new TableColumn<>("Date");
        cDate.setCellValueFactory(d -> new SimpleStringProperty((String) d.getValue()[0]));
        cDate.setPrefWidth(130);

        TableColumn<Object[], String> cNb = new TableColumn<>("Transactions");
        cNb.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue()[1])));
        cNb.setPrefWidth(120);

        TableColumn<Object[], String> cTotal = new TableColumn<>("Total (FCFA)");
        cTotal.setCellValueFactory(d -> {
            BigDecimal val = (BigDecimal) d.getValue()[2];
            return new SimpleStringProperty(String.format("%.0f", val));
        });
        cTotal.setPrefWidth(120);

        ventesTable.getColumns().addAll(cDate, cNb, cTotal);
        ventesTable.setItems(ventesData);
        ventesTable.setColumnResizePolicy(javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY);
        ventesTable.setPrefHeight(220);
    }

    private void buildTopTable() {
        TableColumn<Object[], String> tNom = new TableColumn<>("Article");
        tNom.setCellValueFactory(d -> new SimpleStringProperty((String) d.getValue()[0]));
        tNom.setPrefWidth(200);

        TableColumn<Object[], String> tQte = new TableColumn<>("Qté vendue");
        tQte.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue()[1])));
        tQte.setPrefWidth(110);

        TableColumn<Object[], String> tCA = new TableColumn<>("CA (FCFA)");
        tCA.setCellValueFactory(d -> {
            BigDecimal val = (BigDecimal) d.getValue()[2];
            return new SimpleStringProperty(String.format("%.0f", val));
        });
        tCA.setPrefWidth(110);

        topTable.getColumns().addAll(tNom, tQte, tCA);
        topTable.setItems(topData);
        topTable.setColumnResizePolicy(javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY);
        topTable.setPrefHeight(220);
    }

    private void buildMethodeTable() {
        TableColumn<String[], String> mMeth = new TableColumn<>("Méthode");
        mMeth.setCellValueFactory(d -> new SimpleStringProperty(d.getValue()[0]));
        mMeth.setPrefWidth(160);

        TableColumn<String[], String> mTotal = new TableColumn<>("Total (FCFA)");
        mTotal.setCellValueFactory(d -> new SimpleStringProperty(d.getValue()[1]));
        mTotal.setPrefWidth(130);

        methodeTable.getColumns().addAll(mMeth, mTotal);
        methodeTable.setItems(methodeData);
        methodeTable.setColumnResizePolicy(javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY);
        methodeTable.setPrefHeight(220);
    }

    private HBox buildTablesSection() {
        VBox ventesBox = labeledBox("📅 Ventes par jour", ventesTable);
        VBox topBox    = labeledBox("🏆 Top 10 articles",  topTable);
        VBox methBox   = labeledBox("💳 Répartition paiements", methodeTable);

        HBox row = new HBox(15, ventesBox, topBox, methBox);
        HBox.setHgrow(ventesBox, Priority.ALWAYS);
        HBox.setHgrow(topBox,   Priority.ALWAYS);
        HBox.setHgrow(methBox,  Priority.ALWAYS);
        return row;
    }

    private VBox labeledBox(String title, javafx.scene.Node content) {
        Label lbl = new Label(title);
        lbl.setFont(Font.font("System", FontWeight.BOLD, 13));
        lbl.setStyle("-fx-text-fill: #2c3e50;");
        VBox box = new VBox(8, lbl, content);
        box.setPadding(new Insets(12));
        box.setStyle("-fx-background-color: white; -fx-background-radius: 10; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 6, 0, 0, 2);");
        return box;
    }

    private void loadData() {
        try {
            lblCA.setText(String.format("%.0f FCFA",   dao.getTotalJour()));
            lblSem.setText(String.format("%.0f FCFA",  dao.getTotalSemaine()));
            lblMois.setText(String.format("%.0f FCFA", dao.getTotalMois()));

            ventesData.setAll(dao.getVentesParJour(nbJoursSpinner.getValue()));
            topData.setAll(dao.getTopMenuItems(10));

            methodeData.clear();
            Map<String, BigDecimal> methodes = dao.getTotalByMethode();
            methodes.forEach((k, v) -> methodeData.add(new String[]{k, String.format("%.0f FCFA", v)}));

        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, "Erreur : " + e.getMessage()).showAndWait();
        }
    }
}
