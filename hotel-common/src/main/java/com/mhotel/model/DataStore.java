package com.mhotel.model;

import java.util.ArrayList;
import java.util.List;

public class DataStore {
    private static DataStore instance;
    private List<Hotel> hotels = new ArrayList<>();
    private List<Reservation> reservations = new ArrayList<>();

    private DataStore() {
        // Données initiales
        hotels.add(new Hotel("1", "Ibis Tunis", 120.0));
        hotels.add(new Hotel("2", "Marriott Sousse", 250.0));
    }

    public static synchronized DataStore getInstance() {
        if (instance == null) instance = new DataStore();
        return instance;
    }

    public List<Hotel> getHotels() { return hotels; }
    public void addHotel(Hotel h) { hotels.add(h); }

    public List<Reservation> getReservations() { return reservations; }
    public void addReservation(Reservation r) { reservations.add(r); }
}