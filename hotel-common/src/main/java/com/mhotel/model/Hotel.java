package com.mhotel.model;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Hotel {
    private String id;
    private String nom;
    private double prix;

    public Hotel() {} // Obligatoire
    public Hotel(String id, String nom, double prix) {
        this.id = id;
        this.nom = nom;
        this.prix = prix;
    }
    // Getters/Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public double getPrix() { return prix; }
    public void setPrix(double prix) { this.prix = prix; }
}