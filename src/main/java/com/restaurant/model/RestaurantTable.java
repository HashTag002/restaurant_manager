package com.restaurant.model;

public class RestaurantTable {
    public enum Statut { LIBRE, OCCUPEE, RESERVEE }

    private int id;
    private int numero;
    private int capacite;
    private Statut statut;
    private String zone;

    public RestaurantTable() { this.statut = Statut.LIBRE; }

    public RestaurantTable(int id, int numero, int capacite, Statut statut, String zone) {
        this.id = id;
        this.numero = numero;
        this.capacite = capacite;
        this.statut = statut;
        this.zone = zone;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getNumero() { return numero; }
    public void setNumero(int numero) { this.numero = numero; }

    public int getCapacite() { return capacite; }
    public void setCapacite(int capacite) { this.capacite = capacite; }

    public Statut getStatut() { return statut; }
    public void setStatut(Statut statut) { this.statut = statut; }

    public String getZone() { return zone; }
    public void setZone(String zone) { this.zone = zone; }

    public String getStatutLabel() {
        return switch (statut) {
            case LIBRE    -> "🟢 Libre";
            case OCCUPEE  -> "🔴 Occupée";
            case RESERVEE -> "🟡 Réservée";
        };
    }

    @Override
    public String toString() {
        return "Table " + numero + " (" + zone + " – " + capacite + " pers.)";
    }
}
