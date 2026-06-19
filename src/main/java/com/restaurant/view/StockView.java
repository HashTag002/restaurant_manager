package com.restaurant.view;

import com.restaurant.dao.StockDAO;
import com.restaurant.model.Stock;
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
import java.util.List;

public class StockView extends VBox {

    private final StockDAO dao = new StockDAO();
    private final javafx.scene.control.TableView<Stock> stockTable = new javafx.scene.control.TableView<>();
    private final ObservableList<Stock> stocks = FXCollections.observableArrayList();
    private final CheckBox showAlerteOnly = new CheckBox("⚠️ Alertes seulement");
    private Label alerteCount = new Label("");

    public StockView() {
        setSpacing(10);
        setPadding(new Insets(20));
        setStyle("-fx-background-color: #f8f9fa;");

        Label title = new Label("📦  Gestion des Stocks");
        title.setFont(Font.font("System", FontWeight.BOLD, 22));
        title.setStyle("-fx-text-fill: #2c3e50;");

        buildTable();
        getChildren().addAll(title, buildAlertBar(), buildToolbar(), stockTable);
        VBox.setVgrow(stockTable, Priority.ALWAYS);
        loadData();
    }

    private HBox buildAlertBar() {
        alerteCount.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold; -fx-font-size: 13;");
        showAlerteOnly.setOnAction(e -> loadData());
        HBox bar = new HBox(15, alerteCount, showAlerteOnly);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(6, 10, 6, 10));
        bar.setStyle("-fx-background-color: #fdecea; -fx-background-radius: 8;");
        return bar;
    }

    private HBox buildToolbar() {
        Button btnAdd     = btn("➕ Ajouter",         "#27ae60");
        Button btnEdit    = btn("✏️ Modifier",         "#2980b9");
        Button btnEntree  = btn("📥 Entrée stock",     "#16a085");
        Button btnSortie  = btn("📤 Sortie stock",     "#e67e22");
        Button btnDel     = btn("🗑️ Supprimer",       "#e74c3c");
        Button btnRefresh = btn("🔄 Actualiser",      "#95a5a6");

        btnAdd.setOnAction(e -> showStockDialog(null));
        btnEdit.setOnAction(e -> {
            Stock sel = stockTable.getSelectionModel().getSelectedItem();
            if (sel != null) showStockDialog(sel);
            else alert("Sélectionnez un article.");
        });
        btnEntree.setOnAction(e -> showMouvement("ENTREE"));
        btnSortie.setOnAction(e -> showMouvement("SORTIE"));
        btnDel.setOnAction(e -> deleteSelected());
        btnRefresh.setOnAction(e -> loadData());

        HBox bar = new HBox(8, btnAdd, btnEdit, btnEntree, btnSortie, btnDel, new Region(), btnRefresh);
        HBox.setHgrow(bar.getChildren().get(5), Priority.ALWAYS);
        bar.setAlignment(Pos.CENTER_LEFT);
        return bar;
    }

    private void buildTable() {
        TableColumn<Stock, String> colNom = col("Article", 180);
        colNom.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNom()));

        TableColumn<Stock, String> colCat = col("Catégorie", 120);
        colCat.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCategorie()));

        TableColumn<Stock, String> colQte = col("Quantité", 100);
        colQte.setCellValueFactory(d -> {
            Stock s = d.getValue();
            return new SimpleStringProperty(String.format("%.3f %s", s.getQuantite(), s.getUnite()));
        });
        colQte.setStyle("-fx-alignment: CENTER-RIGHT;");

        TableColumn<Stock, String> colSeuil = col("Seuil alerte", 110);
        colSeuil.setCellValueFactory(d -> new SimpleStringProperty(
                String.format("%.3f %s", d.getValue().getSeuilAlerte(), d.getValue().getUnite())));
        colSeuil.setStyle("-fx-alignment: CENTER-RIGHT;");

        TableColumn<Stock, String> colPrix = col("Prix unit.", 90);
        colPrix.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getPrixUnitaire() != null ? String.format("%f FCFA", d.getValue().getPrixUnitaire()) : "-"));
        colPrix.setStyle("-fx-alignment: CENTER-RIGHT;");

        TableColumn<Stock, String> colFourn = col("Fournisseur", 160);
        colFourn.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFournisseur()));

        TableColumn<Stock, String> colDate = col("Mis à jour", 140);
        colDate.setCellValueFactory(d -> {
            var dt = d.getValue().getUpdatedAt();
            return new SimpleStringProperty(dt != null ? dt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "");
        });

        stockTable.getColumns().addAll(colNom, colCat, colQte, colSeuil, colPrix, colFourn, colDate);
        stockTable.setItems(stocks);
        stockTable.setColumnResizePolicy(javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY);
        stockTable.setRowFactory(tv -> {
            var row = new TableRow<Stock>();
            row.itemProperty().addListener((obs, old, s) -> {
                if (s != null && s.isEnAlerte()) {
                    row.setStyle("-fx-background-color: #fdecea;");
                } else {
                    row.setStyle("");
                }
            });
            return row;
        });
    }

    private void loadData() {
        try {
            List<Stock> all;
            List<Stock> alertes = dao.getEnAlerte();
            alerteCount.setText("⚠️ " + alertes.size() + " article(s) en alerte de stock !");
            if (showAlerteOnly.isSelected()) {
                all = alertes;
            } else {
                all = dao.getAll();
            }
            stocks.setAll(all);
        } catch (SQLException e) {
            error("Erreur : " + e.getMessage());
        }
    }

    private void showStockDialog(Stock existing) {
        Dialog<Stock> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Nouvel article de stock" : "Modifier le stock");
        ButtonType saveBtn = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField nomField    = new TextField();
        TextField catField    = new TextField();
        TextField qteField    = new TextField();
        TextField uniteField  = new TextField();
        TextField seuilField  = new TextField();
        TextField prixField   = new TextField();
        TextField fourn       = new TextField();

        nomField.setPromptText("Nom de l'article");
        catField.setPromptText("Ex: Légumes, Viandes...");
        qteField.setPromptText("Quantité actuelle");
        uniteField.setPromptText("kg, L, unité...");
        seuilField.setPromptText("Seuil minimum");
        prixField.setPromptText("Prix unitaire d'achat");
        fourn.setPromptText("Nom du fournisseur");

        if (existing != null) {
            nomField.setText(existing.getNom());
            catField.setText(existing.getCategorie());
            qteField.setText(existing.getQuantite() != null ? existing.getQuantite().toPlainString() : "0");
            uniteField.setText(existing.getUnite());
            seuilField.setText(existing.getSeuilAlerte() != null ? existing.getSeuilAlerte().toPlainString() : "0");
            prixField.setText(existing.getPrixUnitaire() != null ? existing.getPrixUnitaire().toPlainString() : "0");
            fourn.setText(existing.getFournisseur());
        }

        grid.addRow(0, new Label("Nom :"),         nomField);
        grid.addRow(1, new Label("Catégorie :"),   catField);
        grid.addRow(2, new Label("Quantité :"),    qteField);
        grid.addRow(3, new Label("Unité :"),       uniteField);
        grid.addRow(4, new Label("Seuil alerte :"),seuilField);
        grid.addRow(5, new Label("Prix unit. (FCFA):"),prixField);
        grid.addRow(6, new Label("Fournisseur :"), fourn);
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == saveBtn) {
                Stock s = existing != null ? existing : new Stock();
                s.setNom(nomField.getText().trim());
                s.setCategorie(catField.getText().trim());
                s.setUnite(uniteField.getText().trim());
                s.setFournisseur(fourn.getText().trim());
                try {
                    s.setQuantite(new BigDecimal(qteField.getText().replace(",", ".")));
                    s.setSeuilAlerte(new BigDecimal(seuilField.getText().replace(",", ".")));
                    s.setPrixUnitaire(new BigDecimal(prixField.getText().replace(",", ".")));
                } catch (NumberFormatException ex) {
                    alert("Valeurs numériques invalides.");
                    return null;
                }
                return s;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(s -> {
            if (s != null) {
                try { dao.save(s); loadData(); }
                catch (SQLException ex) { error(ex.getMessage()); }
            }
        });
    }

    private void showMouvement(String type) {
        Stock sel = stockTable.getSelectionModel().getSelectedItem();
        if (sel == null) { alert("Sélectionnez un article de stock."); return; }

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle((type.equals("ENTREE") ? "📥 Entrée" : "📤 Sortie") + " de stock : " + sel.getNom());
        ButtonType okBtn = new ButtonType("Valider", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okBtn, ButtonType.CANCEL);

        TextField qteField = new TextField();
        qteField.setPromptText("Quantité en " + sel.getUnite());
        TextField motifField = new TextField();
        motifField.setPromptText("Motif (optionnel)");

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(10);
        grid.setPadding(new Insets(20));
        grid.addRow(0, new Label("Quantité (" + sel.getUnite() + ") :"), qteField);
        grid.addRow(1, new Label("Stock actuel :"), new Label(String.format("%.3f %s", sel.getQuantite(), sel.getUnite())));
        grid.addRow(2, new Label("Motif :"), motifField);
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == okBtn) {
                try {
                    BigDecimal qte = new BigDecimal(qteField.getText().replace(",", "."));
                    dao.ajouterMouvement(sel.getId(), type, qte, motifField.getText().trim());
                    loadData();
                } catch (NumberFormatException ex) {
                    alert("Quantité invalide.");
                } catch (SQLException ex) {
                    error(ex.getMessage());
                }
            }
            return null;
        });
        dialog.showAndWait();
    }

    private void deleteSelected() {
        Stock sel = stockTable.getSelectionModel().getSelectedItem();
        if (sel == null) { alert("Sélectionnez un article."); return; }
        new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer « " + sel.getNom() + " » du stock ?", ButtonType.YES, ButtonType.NO)
                .showAndWait().ifPresent(r -> {
                    if (r == ButtonType.YES) {
                        try { dao.delete(sel.getId()); loadData(); }
                        catch (SQLException ex) { error(ex.getMessage()); }
                    }
                });
    }

    private <T> TableColumn<T, String> col(String name, double w) {
        TableColumn<T, String> c = new TableColumn<>(name);
        c.setPrefWidth(w);
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
