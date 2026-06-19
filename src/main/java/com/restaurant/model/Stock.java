package com.restaurant.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Stock {
    private int id;
    private String nom;
    private String categorie;
    private BigDecimal quantite;
    private String unite;
    private BigDecimal seuilAlerte;
    private BigDecimal prixUnitaire;
    private String fournisseur;
    private LocalDateTime updatedAt;

    public Stock() {}

    public boolean isEnAlerte() {
        if (quantite == null || seuilAlerte == null) return false;
        return quantite.compareTo(seuilAlerte) <= 0;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getCategorie() { return categorie; }
    public void setCategorie(String categorie) { this.categorie = categorie; }

    public BigDecimal getQuantite() { return quantite; }
    public void setQuantite(BigDecimal quantite) { this.quantite = quantite; }

    public String getUnite() { return unite; }
    public void setUnite(String unite) { this.unite = unite; }

    public BigDecimal getSeuilAlerte() { return seuilAlerte; }
    public void setSeuilAlerte(BigDecimal seuilAlerte) { this.seuilAlerte = seuilAlerte; }

    public BigDecimal getPrixUnitaire() { return prixUnitaire; }
    public void setPrixUnitaire(BigDecimal prixUnitaire) { this.prixUnitaire = prixUnitaire; }

    public String getFournisseur() { return fournisseur; }
    public void setFournisseur(String fournisseur) { this.fournisseur = fournisseur; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() { return nom; }
}
