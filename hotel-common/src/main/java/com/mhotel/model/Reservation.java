package com.mhotel.model;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Reservation {
    private String id;
    private String hotelNom;
    private String clientNom;
    private String statut;

    public Reservation() {}
    public Reservation(String id, String hotelNom, String clientNom, String statut) {
        this.id = id;
        this.hotelNom = hotelNom;
        this.clientNom = clientNom;
        this.statut = statut;
    }
    // Getters/Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getHotelNom() { return hotelNom; }
    public void setHotelNom(String hotelNom) { this.hotelNom = hotelNom; }
    public String getClientNom() { return clientNom; }
    public void setClientNom(String clientNom) { this.clientNom = clientNom; }
    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }
}