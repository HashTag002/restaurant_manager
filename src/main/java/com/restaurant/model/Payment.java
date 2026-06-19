package com.restaurant.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Payment {
    public enum Methode {
        ESPECES("Espèces"),
        CARTE("Carte bancaire"),
        MOMO("MOBILE_MONEY"),
        OM("ORANGE_MONEY"),
        TICKET_RESTO("Ticket restaurant");

        private final String label;
        Methode(String label) { this.label = label; }
        public String getLabel() { return label; }
        @Override public String toString() { return label; }
    }

    private int id;
    private int commandeId;
    private String tableNumero;
    private BigDecimal montant;
    private Methode methode;
    private BigDecimal montantRecu;
    private BigDecimal monnaie;
    private String reference;
    private LocalDateTime createdAt;

    public Payment() { this.methode = Methode.ESPECES; }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getCommandeId() { return commandeId; }
    public void setCommandeId(int commandeId) { this.commandeId = commandeId; }

    public String getTableNumero() { return tableNumero; }
    public void setTableNumero(String tableNumero) { this.tableNumero = tableNumero; }

    public BigDecimal getMontant() { return montant; }
    public void setMontant(BigDecimal montant) { this.montant = montant; }

    public Methode getMethode() { return methode; }
    public void setMethode(Methode methode) { this.methode = methode; }

    public BigDecimal getMontantRecu() { return montantRecu; }
    public void setMontantRecu(BigDecimal montantRecu) { this.montantRecu = montantRecu; }

    public BigDecimal getMonnaie() { return monnaie; }
    public void setMonnaie(BigDecimal monnaie) { this.monnaie = monnaie; }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
