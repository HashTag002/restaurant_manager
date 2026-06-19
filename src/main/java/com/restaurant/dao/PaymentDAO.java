package com.restaurant.dao;

import com.restaurant.config.DatabaseConfig;
import com.restaurant.model.Payment;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PaymentDAO {

    public Payment save(Payment payment) throws SQLException {
        String sql = """
            INSERT INTO paiements(commande_id, montant, methode, montant_recu, monnaie, reference)
            VALUES(?,?,?,?,?,?) RETURNING id
            """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, payment.getCommandeId());
            ps.setBigDecimal(2, payment.getMontant());
            ps.setString(3, payment.getMethode().name());
            ps.setBigDecimal(4, payment.getMontantRecu());
            ps.setBigDecimal(5, payment.getMonnaie());
            ps.setString(6, payment.getReference());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) payment.setId(rs.getInt(1));
            }
        }
        return payment;
    }

    public List<Payment> getAll() throws SQLException {
        List<Payment> list = new ArrayList<>();
        String sql = """
            SELECT p.id, p.commande_id, t.numero AS table_num,
                   p.montant, p.methode, p.montant_recu, p.monnaie, p.reference, p.created_at
            FROM paiements p
            JOIN commandes c ON c.id = p.commande_id
            LEFT JOIN tables_restaurant t ON t.id = c.table_id
            ORDER BY p.created_at DESC
            """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapPayment(rs));
        }
        return list;
    }

    public Map<String, BigDecimal> getTotalByMethode() throws SQLException {
        Map<String, BigDecimal> map = new LinkedHashMap<>();
        String sql = "SELECT methode, SUM(montant) AS total FROM paiements GROUP BY methode ORDER BY methode";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) map.put(rs.getString("methode"), rs.getBigDecimal("total"));
        }
        return map;
    }

    public BigDecimal getTotalJour() throws SQLException {
        String sql = "SELECT COALESCE(SUM(montant),0) FROM paiements WHERE DATE(created_at)=CURRENT_DATE";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getBigDecimal(1);
        }
        return BigDecimal.ZERO;
    }

    public BigDecimal getTotalSemaine() throws SQLException {
        String sql = "SELECT COALESCE(SUM(montant),0) FROM paiements WHERE created_at >= CURRENT_DATE - INTERVAL '7 days'";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getBigDecimal(1);
        }
        return BigDecimal.ZERO;
    }

    public BigDecimal getTotalMois() throws SQLException {
        String sql = "SELECT COALESCE(SUM(montant),0) FROM paiements WHERE DATE_TRUNC('month', created_at)=DATE_TRUNC('month', CURRENT_DATE)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getBigDecimal(1);
        }
        return BigDecimal.ZERO;
    }

    public List<Object[]> getVentesParJour(int nbJours) throws SQLException {
        List<Object[]> list = new ArrayList<>();
        String sql = String.format("""
            SELECT DATE(created_at) AS jour, COUNT(*) AS nb, SUM(montant) AS total
            FROM paiements
            WHERE created_at >= CURRENT_DATE - INTERVAL '%d days'
            GROUP BY DATE(created_at)
            ORDER BY jour
            """, nbJours);
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new Object[]{rs.getDate("jour").toString(), rs.getInt("nb"), rs.getBigDecimal("total")});
            }
        }
        return list;
    }

    public List<Object[]> getTopMenuItems(int limit) throws SQLException {
        List<Object[]> list = new ArrayList<>();
        String sql = """
            SELECT m.nom, SUM(ci.quantite) AS total_qte, SUM(ci.quantite * ci.prix_unitaire) AS ca
            FROM commande_items ci
            JOIN menu_items m ON m.id = ci.menu_item_id
            GROUP BY m.nom
            ORDER BY total_qte DESC
            LIMIT ?
            """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Object[]{rs.getString("nom"), rs.getInt("total_qte"), rs.getBigDecimal("ca")});
                }
            }
        }
        return list;
    }

    private Payment mapPayment(ResultSet rs) throws SQLException {
        Payment p = new Payment();
        p.setId(rs.getInt("id"));
        p.setCommandeId(rs.getInt("commande_id"));
        p.setTableNumero("Table " + rs.getString("table_num"));
        p.setMontant(rs.getBigDecimal("montant"));
        p.setMethode(Payment.Methode.valueOf(rs.getString("methode")));
        p.setMontantRecu(rs.getBigDecimal("montant_recu"));
        p.setMonnaie(rs.getBigDecimal("monnaie"));
        p.setReference(rs.getString("reference"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) p.setCreatedAt(ts.toLocalDateTime());
        return p;
    }
}
