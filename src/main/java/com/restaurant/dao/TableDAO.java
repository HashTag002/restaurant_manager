package com.restaurant.dao;

import com.restaurant.config.DatabaseConfig;
import com.restaurant.model.RestaurantTable;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TableDAO {

    public List<RestaurantTable> getAll() throws SQLException {
        List<RestaurantTable> list = new ArrayList<>();
        String sql = "SELECT id, numero, capacite, statut, zone FROM tables_restaurant ORDER BY numero";
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    public List<RestaurantTable> getFreeTablesForOrder() throws SQLException {
        List<RestaurantTable> list = new ArrayList<>();
        String sql = "SELECT id, numero, capacite, statut, zone FROM tables_restaurant WHERE statut='LIBRE' ORDER BY numero";
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public RestaurantTable save(RestaurantTable table) throws SQLException {
        if (table.getId() == 0) {
            String sql = "INSERT INTO tables_restaurant(numero, capacite, statut, zone) VALUES(?,?,?,?) RETURNING id";
            try (Connection c = DatabaseConfig.getConnection();
                 PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setInt(1, table.getNumero());
                ps.setInt(2, table.getCapacite());
                ps.setString(3, table.getStatut().name());
                ps.setString(4, table.getZone());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) table.setId(rs.getInt(1));
                }
            }
        } else {
            String sql = "UPDATE tables_restaurant SET numero=?, capacite=?, statut=?, zone=? WHERE id=?";
            try (Connection c = DatabaseConfig.getConnection();
                 PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setInt(1, table.getNumero());
                ps.setInt(2, table.getCapacite());
                ps.setString(3, table.getStatut().name());
                ps.setString(4, table.getZone());
                ps.setInt(5, table.getId());
                ps.executeUpdate();
            }
        }
        return table;
    }

    public void updateStatut(int tableId, RestaurantTable.Statut statut) throws SQLException {
        String sql = "UPDATE tables_restaurant SET statut=? WHERE id=?";
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, statut.name());
            ps.setInt(2, tableId);
            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM tables_restaurant WHERE id=?";
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private RestaurantTable mapRow(ResultSet rs) throws SQLException {
        RestaurantTable t = new RestaurantTable();
        t.setId(rs.getInt("id"));
        t.setNumero(rs.getInt("numero"));
        t.setCapacite(rs.getInt("capacite"));
        t.setStatut(RestaurantTable.Statut.valueOf(rs.getString("statut")));
        t.setZone(rs.getString("zone"));
        return t;
    }
}
