package com.restaurant.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class MenuItem {
    private int id;
    private int categorieId;
    private String categorieNom;
    private String nom;
    private String description;
    private BigDecimal prix;
    private boolean disponible;
    private LocalDateTime createdAt;

    public MenuItem() { this.disponible = true; }

    public MenuItem(int id, int categorieId, String nom, String description,
                    BigDecimal prix, boolean disponible) {
        this.id = id;
        this.categorieId = categorieId;
        this.nom = nom;
        this.description = description;
        this.prix = prix;
        this.disponible = disponible;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getCategorieId() { return categorieId; }
    public void setCategorieId(int categorieId) { this.categorieId = categorieId; }

    public String getCategorieNom() { return categorieNom; }
    public void setCategorieNom(String categorieNom) { this.categorieNom = categorieNom; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getPrix() { return prix; }
    public void setPrix(BigDecimal prix) { this.prix = prix; }

    public boolean isDisponible() { return disponible; }
    public void setDisponible(boolean disponible) { this.disponible = disponible; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() { return nom + " (" + String.format("%f FCFA", prix) + ")"; }
}
