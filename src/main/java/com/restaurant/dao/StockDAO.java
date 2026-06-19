package com.restaurant.dao;

import com.restaurant.config.DatabaseConfig;
import com.restaurant.model.Stock;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class StockDAO {

    public List<Stock> getAll() throws SQLException {
        List<Stock> list = new ArrayList<>();
        String sql = """
            SELECT id, nom, categorie, quantite, unite, seuil_alerte, prix_unitaire, fournisseur, updated_at
            FROM stocks ORDER BY categorie, nom
            """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapStock(rs));
        }
        return list;
    }

    public List<Stock> getEnAlerte() throws SQLException {
        List<Stock> list = new ArrayList<>();
        String sql = "SELECT * FROM vue_stock_alerte";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapStock(rs));
        }
        return list;
    }

    public Stock save(Stock stock) throws SQLException {
        if (stock.getId() == 0) {
            String sql = """
                INSERT INTO stocks(nom, categorie, quantite, unite, seuil_alerte, prix_unitaire, fournisseur)
                VALUES(?,?,?,?,?,?,?) RETURNING id
                """;
            try (Connection conn = DatabaseConfig.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                setStockParams(ps, stock);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) stock.setId(rs.getInt(1));
                }
            }
        } else {
            String sql = """
                UPDATE stocks SET nom=?, categorie=?, quantite=?, unite=?,
                seuil_alerte=?, prix_unitaire=?, fournisseur=?, updated_at=CURRENT_TIMESTAMP
                WHERE id=?
                """;
            try (Connection conn = DatabaseConfig.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                setStockParams(ps, stock);
                ps.setInt(8, stock.getId());
                ps.executeUpdate();
            }
        }
        return stock;
    }

    public void ajouterMouvement(int stockId, String type, BigDecimal quantite, String motif) throws SQLException {
        Connection conn = DatabaseConfig.getConnection();
        try {
            conn.setAutoCommit(false);

            String mvtSql = "INSERT INTO mouvements_stock(stock_id, type_mouv, quantite, motif) VALUES(?,?,?,?)";
            try (PreparedStatement ps = conn.prepareStatement(mvtSql)) {
                ps.setInt(1, stockId);
                ps.setString(2, type);
                ps.setBigDecimal(3, quantite);
                ps.setString(4, motif);
                ps.executeUpdate();
            }

            String direction = type.equals("ENTREE") ? "+" : "-";
            String updSql = "UPDATE stocks SET quantite = quantite " + direction + " ?, updated_at=CURRENT_TIMESTAMP WHERE id=?";
            try (PreparedStatement ps = conn.prepareStatement(updSql)) {
                ps.setBigDecimal(1, quantite);
                ps.setInt(2, stockId);
                ps.executeUpdate();
            }

            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM stocks WHERE id=?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private void setStockParams(PreparedStatement ps, Stock s) throws SQLException {
        ps.setString(1, s.getNom());
        ps.setString(2, s.getCategorie());
        ps.setBigDecimal(3, s.getQuantite());
        ps.setString(4, s.getUnite());
        ps.setBigDecimal(5, s.getSeuilAlerte());
        ps.setBigDecimal(6, s.getPrixUnitaire());
        ps.setString(7, s.getFournisseur());
    }

    private Stock mapStock(ResultSet rs) throws SQLException {
        Stock s = new Stock();
        s.setId(rs.getInt("id"));
        s.setNom(rs.getString("nom"));
        s.setCategorie(rs.getString("categorie"));
        s.setQuantite(rs.getBigDecimal("quantite"));
        s.setUnite(rs.getString("unite"));
        s.setSeuilAlerte(rs.getBigDecimal("seuil_alerte"));
        try { s.setPrixUnitaire(rs.getBigDecimal("prix_unitaire")); } catch (Exception ignored) {}
        try { s.setFournisseur(rs.getString("fournisseur")); } catch (Exception ignored) {}
        Timestamp ts = rs.getTimestamp("updated_at");
        if (ts != null) s.setUpdatedAt(ts.toLocalDateTime());
        return s;
    }
}
