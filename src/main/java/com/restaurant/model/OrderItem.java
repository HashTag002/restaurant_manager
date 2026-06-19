package com.restaurant.model;

import java.math.BigDecimal;

public class OrderItem {
    private int id;
    private int commandeId;
    private int menuItemId;
    private String menuItemNom;
    private int quantite;
    private BigDecimal prixUnitaire;
    private String note;

    public OrderItem() {}

    public OrderItem(int menuItemId, String menuItemNom, int quantite, BigDecimal prixUnitaire) {
        this.menuItemId   = menuItemId;
        this.menuItemNom  = menuItemNom;
        this.quantite     = quantite;
        this.prixUnitaire = prixUnitaire;
    }

    public BigDecimal getSousTotal() {
        if (prixUnitaire == null) return BigDecimal.ZERO;
        return prixUnitaire.multiply(BigDecimal.valueOf(quantite));
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getCommandeId() { return commandeId; }
    public void setCommandeId(int commandeId) { this.commandeId = commandeId; }

    public int getMenuItemId() { return menuItemId; }
    public void setMenuItemId(int menuItemId) { this.menuItemId = menuItemId; }

    public String getMenuItemNom() { return menuItemNom; }
    public void setMenuItemNom(String menuItemNom) { this.menuItemNom = menuItemNom; }

    public int getQuantite() { return quantite; }
    public void setQuantite(int quantite) { this.quantite = quantite; }

    public BigDecimal getPrixUnitaire() { return prixUnitaire; }
    public void setPrixUnitaire(BigDecimal prixUnitaire) { this.prixUnitaire = prixUnitaire; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
