package com.restaurant.dao;

import com.restaurant.config.DatabaseConfig;
import com.restaurant.model.Category;
import com.restaurant.model.MenuItem;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MenuItemDAO {

    // ---- CATEGORIES ----

    public List<Category> getAllCategories() throws SQLException {
        List<Category> list = new ArrayList<>();
        String sql = "SELECT id, nom, description FROM categories ORDER BY nom";
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new Category(rs.getInt("id"), rs.getString("nom"), rs.getString("description")));
            }
        }
        return list;
    }

    public Category saveCategory(Category cat) throws SQLException {
        if (cat.getId() == 0) {
            String sql = "INSERT INTO categories(nom, description) VALUES(?,?) RETURNING id";
            try (Connection c = DatabaseConfig.getConnection();
                 PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, cat.getNom());
                ps.setString(2, cat.getDescription());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) cat.setId(rs.getInt(1));
                }
            }
        } else {
            String sql = "UPDATE categories SET nom=?, description=? WHERE id=?";
            try (Connection c = DatabaseConfig.getConnection();
                 PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, cat.getNom());
                ps.setString(2, cat.getDescription());
                ps.setInt(3, cat.getId());
                ps.executeUpdate();
            }
        }
        return cat;
    }

    public void deleteCategory(int id) throws SQLException {
        String sql = "DELETE FROM categories WHERE id=?";
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    // ---- MENU ITEMS ----

    public List<MenuItem> getAllMenuItems() throws SQLException {
        List<MenuItem> list = new ArrayList<>();
        String sql = """
            SELECT m.id, m.categorie_id, cat.nom AS cat_nom, m.nom, m.description,
                   m.prix, m.disponible
            FROM menu_items m
            LEFT JOIN categories cat ON cat.id = m.categorie_id
            ORDER BY cat.nom, m.nom
            """;
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                MenuItem item = new MenuItem();
                item.setId(rs.getInt("id"));
                item.setCategorieId(rs.getInt("categorie_id"));
                item.setCategorieNom(rs.getString("cat_nom"));
                item.setNom(rs.getString("nom"));
                item.setDescription(rs.getString("description"));
                item.setPrix(rs.getBigDecimal("prix"));
                item.setDisponible(rs.getBoolean("disponible"));
                list.add(item);
            }
        }
        return list;
    }

    public List<MenuItem> getAvailableMenuItems() throws SQLException {
        List<MenuItem> list = new ArrayList<>();
        String sql = """
            SELECT m.id, m.categorie_id, cat.nom AS cat_nom, m.nom, m.description,
                   m.prix, m.disponible
            FROM menu_items m
            LEFT JOIN categories cat ON cat.id = m.categorie_id
            WHERE m.disponible = true
            ORDER BY cat.nom, m.nom
            """;
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                MenuItem item = new MenuItem();
                item.setId(rs.getInt("id"));
                item.setCategorieId(rs.getInt("categorie_id"));
                item.setCategorieNom(rs.getString("cat_nom"));
                item.setNom(rs.getString("nom"));
                item.setDescription(rs.getString("description"));
                item.setPrix(rs.getBigDecimal("prix"));
                item.setDisponible(rs.getBoolean("disponible"));
                list.add(item);
            }
        }
        return list;
    }

    public MenuItem save(MenuItem item) throws SQLException {
        if (item.getId() == 0) {
            String sql = """
                INSERT INTO menu_items(categorie_id, nom, description, prix, disponible)
                VALUES(?,?,?,?,?) RETURNING id
                """;
            try (Connection c = DatabaseConfig.getConnection();
                 PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setInt(1, item.getCategorieId());
                ps.setString(2, item.getNom());
                ps.setString(3, item.getDescription());
                ps.setBigDecimal(4, item.getPrix());
                ps.setBoolean(5, item.isDisponible());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) item.setId(rs.getInt(1));
                }
            }
        } else {
            String sql = """
                UPDATE menu_items SET categorie_id=?, nom=?, description=?, prix=?,
                disponible=?, updated_at=CURRENT_TIMESTAMP WHERE id=?
                """;
            try (Connection c = DatabaseConfig.getConnection();
                 PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setInt(1, item.getCategorieId());
                ps.setString(2, item.getNom());
                ps.setString(3, item.getDescription());
                ps.setBigDecimal(4, item.getPrix());
                ps.setBoolean(5, item.isDisponible());
                ps.setInt(6, item.getId());
                ps.executeUpdate();
            }
        }
        return item;
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM menu_items WHERE id=?";
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }
}
