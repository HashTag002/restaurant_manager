package com.restaurant.dao;

import com.restaurant.config.DatabaseConfig;
import com.restaurant.model.Order;
import com.restaurant.model.OrderItem;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OrderDAO {

    public List<Order> getAll() throws SQLException {
        List<Order> list = new ArrayList<>();
        String sql = """
            SELECT c.id, c.table_id, t.numero AS table_num, c.serveur,
                   c.statut, c.note, c.created_at
            FROM commandes c
            LEFT JOIN tables_restaurant t ON t.id = c.table_id
            ORDER BY c.created_at DESC
            """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapOrder(rs));
        }
        return list;
    }

    public List<Order> getActive() throws SQLException {
        List<Order> list = new ArrayList<>();
        String sql = """
            SELECT c.id, c.table_id, t.numero AS table_num, c.serveur,
                   c.statut, c.note, c.created_at
            FROM commandes c
            LEFT JOIN tables_restaurant t ON t.id = c.table_id
            WHERE c.statut NOT IN ('PAYEE','ANNULEE')
            ORDER BY c.created_at DESC
            """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapOrder(rs));
        }
        return list;
    }

    public Order getById(int id) throws SQLException {
        String sql = """
            SELECT c.id, c.table_id, t.numero AS table_num, c.serveur,
                   c.statut, c.note, c.created_at
            FROM commandes c
            LEFT JOIN tables_restaurant t ON t.id = c.table_id
            WHERE c.id = ?
            """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Order order = mapOrder(rs);
                    order.setItems(getItemsByOrderId(id));
                    return order;
                }
            }
        }
        return null;
    }

    public Order save(Order order) throws SQLException {
        Connection conn = DatabaseConfig.getConnection();
        try {
            conn.setAutoCommit(false);

            if (order.getId() == 0) {
                String sql = "INSERT INTO commandes(table_id, serveur, statut, note) VALUES(?,?,?,?) RETURNING id";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, order.getTableId());
                    ps.setString(2, order.getServeur());
                    ps.setString(3, order.getStatut().name());
                    ps.setString(4, order.getNote());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) order.setId(rs.getInt(1));
                    }
                }
            } else {
                String sql = "UPDATE commandes SET table_id=?, serveur=?, statut=?, note=?, updated_at=CURRENT_TIMESTAMP WHERE id=?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, order.getTableId());
                    ps.setString(2, order.getServeur());
                    ps.setString(3, order.getStatut().name());
                    ps.setString(4, order.getNote());
                    ps.setInt(5, order.getId());
                    ps.executeUpdate();
                }
                // Delete and re-insert items
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM commande_items WHERE commande_id=?")) {
                    ps.setInt(1, order.getId());
                    ps.executeUpdate();
                }
            }

            // Insert items
            String itemSql = "INSERT INTO commande_items(commande_id, menu_item_id, quantite, prix_unitaire, note) VALUES(?,?,?,?,?)";
            try (PreparedStatement ps = conn.prepareStatement(itemSql)) {
                for (OrderItem item : order.getItems()) {
                    ps.setInt(1, order.getId());
                    ps.setInt(2, item.getMenuItemId());
                    ps.setInt(3, item.getQuantite());
                    ps.setBigDecimal(4, item.getPrixUnitaire());
                    ps.setString(5, item.getNote());
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
        return order;
    }

    public void updateStatut(int id, Order.Statut statut) throws SQLException {
        String sql = "UPDATE commandes SET statut=?, updated_at=CURRENT_TIMESTAMP WHERE id=?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, statut.name());
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM commandes WHERE id=?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public List<OrderItem> getItemsByOrderId(int commandeId) throws SQLException {
        List<OrderItem> items = new ArrayList<>();
        String sql = """
            SELECT ci.id, ci.commande_id, ci.menu_item_id, m.nom, ci.quantite, ci.prix_unitaire, ci.note
            FROM commande_items ci
            JOIN menu_items m ON m.id = ci.menu_item_id
            WHERE ci.commande_id = ?
            """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, commandeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    OrderItem oi = new OrderItem();
                    oi.setId(rs.getInt("id"));
                    oi.setCommandeId(rs.getInt("commande_id"));
                    oi.setMenuItemId(rs.getInt("menu_item_id"));
                    oi.setMenuItemNom(rs.getString("nom"));
                    oi.setQuantite(rs.getInt("quantite"));
                    oi.setPrixUnitaire(rs.getBigDecimal("prix_unitaire"));
                    oi.setNote(rs.getString("note"));
                    items.add(oi);
                }
            }
        }
        return items;
    }

    private Order mapOrder(ResultSet rs) throws SQLException {
        Order o = new Order();
        o.setId(rs.getInt("id"));
        o.setTableId(rs.getInt("table_id"));
        o.setTableNumero("Table " + rs.getString("table_num"));
        o.setServeur(rs.getString("serveur"));
        o.setStatut(Order.Statut.valueOf(rs.getString("statut")));
        o.setNote(rs.getString("note"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) o.setCreatedAt(ts.toLocalDateTime());
        return o;
    }
}
