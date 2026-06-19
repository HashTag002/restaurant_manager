package com.restaurant.view;

import com.restaurant.dao.MenuItemDAO;
import com.restaurant.model.Category;
import com.restaurant.model.MenuItem;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

public class MenuView extends VBox {

    private final MenuItemDAO dao = new MenuItemDAO();
    private final javafx.scene.control.TableView<MenuItem> table = new javafx.scene.control.TableView<>();
    private final ObservableList<MenuItem> items = FXCollections.observableArrayList();
    private final ComboBox<Category> filterCat = new ComboBox<>();

    public MenuView() {
        setSpacing(10);
        setPadding(new Insets(20));
        setStyle("-fx-background-color: #f8f9fa;");

        Label title = new Label("🍽️  Gestion du Menu");
        title.setFont(Font.font("System", FontWeight.BOLD, 22));
        title.setStyle("-fx-text-fill: #2c3e50;");

        buildToolbar();
        buildTable();

        getChildren().addAll(title, buildToolbar(), table);
        VBox.setVgrow(table, Priority.ALWAYS);
        loadData();
    }

    private HBox buildToolbar() {
        filterCat.setPromptText("Toutes catégories");
        filterCat.setOnAction(e -> loadData());

        Button btnAdd  = styledButton("➕ Nouvel article", "#27ae60");
        Button btnEdit = styledButton("✏️ Modifier",       "#2980b9");
        Button btnDel  = styledButton("🗑️ Supprimer",     "#e74c3c");
        Button btnRefresh = styledButton("🔄 Actualiser", "#95a5a6");

        btnAdd.setOnAction(e -> showItemDialog(null));
        btnEdit.setOnAction(e -> {
            MenuItem selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) showItemDialog(selected);
            else showAlert("Sélectionnez un article à modifier.");
        });
        btnDel.setOnAction(e -> deleteSelected());
        btnRefresh.setOnAction(e -> loadData());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox bar = new HBox(10, filterCat, btnAdd, btnEdit, btnDel, spacer, btnRefresh);
        bar.setAlignment(Pos.CENTER_LEFT);
        return bar;
    }

    private void buildTable() {
        TableColumn<MenuItem, String> colCat  = col("Catégorie", 140);
        colCat.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCategorieNom()));

        TableColumn<MenuItem, String> colNom  = col("Nom", 200);
        colNom.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNom()));

        TableColumn<MenuItem, String> colDesc = col("Description", 300);
        colDesc.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDescription()));

        TableColumn<MenuItem, String> colPrix = col("Prix (FCFA)", 90);
        colPrix.setCellValueFactory(d -> new SimpleStringProperty(String.format("%.0f", d.getValue().getPrix())));
        colPrix.setStyle("-fx-alignment: CENTER-RIGHT;");

        TableColumn<MenuItem, Boolean> colDispo = new TableColumn<>("Disponible");
        colDispo.setCellValueFactory(d -> new SimpleBooleanProperty(d.getValue().isDisponible()));
        colDispo.setCellFactory(CheckBoxTableCell.forTableColumn(colDispo));
        colDispo.setEditable(false);
        colDispo.setPrefWidth(90);

        table.setEditable(false);
        table.getColumns().addAll(colCat, colNom, colDesc, colPrix, colDispo);
        table.setItems(items);
        table.setColumnResizePolicy(
            javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY
        );
        table.setRowFactory(tv -> {
            TableRow<MenuItem> row = new TableRow<>();
            row.itemProperty().addListener((obs, old, item) -> {
                if (item != null && !item.isDisponible()) {
                    row.setStyle("-fx-text-fill: #999;");
                } else {
                    row.setStyle("");
                }
            });
            return row;
        });
    }

    private void loadData() {
        try {
            List<Category> cats = dao.getAllCategories();
            Category all = new Category(0, "Toutes catégories", "");
            filterCat.setItems(FXCollections.observableArrayList());
            filterCat.getItems().add(all);
            filterCat.getItems().addAll(cats);

            List<MenuItem> all_items = dao.getAllMenuItems();
            Category sel = filterCat.getValue();
            if (sel != null && sel.getId() != 0) {
                all_items.removeIf(i -> i.getCategorieId() != sel.getId());
            }
            items.setAll(all_items);
        } catch (SQLException e) {
            showError("Erreur de chargement : " + e.getMessage());
        }
    }

    private void showItemDialog(MenuItem existing) {
        Dialog<MenuItem> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Nouvel article" : "Modifier l'article");
        dialog.setHeaderText(null);

        ButtonType saveBtn = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        // Form
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        ComboBox<Category> catBox = new ComboBox<>();
        try { catBox.setItems(FXCollections.observableArrayList(dao.getAllCategories())); }
        catch (SQLException ex) { showError(ex.getMessage()); }

        TextField nomField   = new TextField();
        TextField prixField  = new TextField();
        TextArea descArea    = new TextArea();
        descArea.setPrefRowCount(3);
        CheckBox dispoCheck  = new CheckBox("Disponible");

        nomField.setPromptText("Nom de l'article");
        prixField.setPromptText("Ex: 12.50");

        if (existing != null) {
            catBox.getItems().stream()
                    .filter(c -> c.getId() == existing.getCategorieId())
                    .findFirst().ifPresent(catBox::setValue);
            nomField.setText(existing.getNom());
            prixField.setText(existing.getPrix() != null ? existing.getPrix().toPlainString() : "");
            descArea.setText(existing.getDescription());
            dispoCheck.setSelected(existing.isDisponible());
        } else {
            dispoCheck.setSelected(true);
        }

        grid.addRow(0, new Label("Catégorie :"), catBox);
        grid.addRow(1, new Label("Nom :"), nomField);
        grid.addRow(2, new Label("Prix (FCFA) :"), prixField);
        grid.addRow(3, new Label("Description :"), descArea);
        grid.addRow(4, new Label(""), dispoCheck);
        catBox.setMaxWidth(Double.MAX_VALUE);
        nomField.setMaxWidth(Double.MAX_VALUE);
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == saveBtn) {
                MenuItem item = existing != null ? existing : new MenuItem();
                if (catBox.getValue() != null) item.setCategorieId(catBox.getValue().getId());
                item.setNom(nomField.getText().trim());
                item.setDescription(descArea.getText().trim());
                try {
                    item.setPrix(new BigDecimal(prixField.getText().trim().replace(",", ".")));
                } catch (NumberFormatException ex) {
                    showAlert("Prix invalide.");
                    return null;
                }
                item.setDisponible(dispoCheck.isSelected());
                return item;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(item -> {
            if (item != null) {
                try {
                    dao.save(item);
                    loadData();
                } catch (SQLException ex) {
                    showError("Erreur d'enregistrement : " + ex.getMessage());
                }
            }
        });
    }

    private void deleteSelected() {
        MenuItem sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) { showAlert("Sélectionnez un article."); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer « " + sel.getNom() + " » ?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.YES) {
                try {
                    dao.delete(sel.getId());
                    loadData();
                } catch (SQLException ex) {
                    showError("Erreur de suppression : " + ex.getMessage());
                }
            }
        });
    }

    private <T> TableColumn<MenuItem, T> col(String name, double width) {
        TableColumn<MenuItem, T> c = new TableColumn<>(name);
        c.setPrefWidth(width);
        return c;
    }

    private Button styledButton(String text, String color) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color:" + color + "; -fx-text-fill:white; -fx-background-radius:5; -fx-padding:6 14;");
        return b;
    }

    private void showAlert(String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK).showAndWait();
    }

    private void showError(String msg) {
        new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait();
    }
}
