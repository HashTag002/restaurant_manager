package com.restaurant.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Order {
    public enum Statut {
        EN_ATTENTE, EN_COURS, SERVIE, PAYEE, ANNULEE;

        public String getLabel() {
            return switch (this) {
                case EN_ATTENTE -> "⏳ En attente";
                case EN_COURS   -> "👨‍🍳 En cours";
                case SERVIE     -> "🍽️ Servie";
                case PAYEE      -> "✅ Payée";
                case ANNULEE    -> "❌ Annulée";
            };
        }
    }

    private int id;
    private int tableId;
    private String tableNumero;
    private String serveur;
    private Statut statut;
    private String note;
    private List<OrderItem> items;
    private LocalDateTime createdAt;

    public Order() {
        this.statut = Statut.EN_ATTENTE;
        this.items  = new ArrayList<>();
    }

    public BigDecimal getTotal() {
        return items.stream()
                .map(OrderItem::getSousTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getTableId() { return tableId; }
    public void setTableId(int tableId) { this.tableId = tableId; }

    public String getTableNumero() { return tableNumero; }
    public void setTableNumero(String tableNumero) { this.tableNumero = tableNumero; }

    public String getServeur() { return serveur; }
    public void setServeur(String serveur) { this.serveur = serveur; }

    public Statut getStatut() { return statut; }
    public void setStatut(Statut statut) { this.statut = statut; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
