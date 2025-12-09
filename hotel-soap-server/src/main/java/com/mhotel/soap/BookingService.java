package com.mhotel.soap;
import javax.jws.WebMethod;
import javax.jws.WebService;

@WebService
public class BookingService {
    @WebMethod
    public boolean reserver(String hotelNom, String clientName) {
        // Ajout dans la "DB"
        com.mhotel.model.Reservation r = new com.mhotel.model.Reservation(
                String.valueOf(System.currentTimeMillis()),
                hotelNom,
                clientName,
                "CONFIRMEE"
        );
        com.mhotel.model.DataStore.getInstance().addReservation(r);
        return true;
    }

    @WebMethod
    public String getHistorique(String clientName) {
        // On retourne une String simple pour éviter les soucis de parsing XML complexe pour ce tuto
        StringBuilder sb = new StringBuilder();
        for (com.mhotel.model.Reservation r : com.mhotel.model.DataStore.getInstance().getReservations()) {
            if (r.getClientNom().equals(clientName)) {
                sb.append(r.getHotelNom()).append(" (").append(r.getStatut()).append(");");
            }
        }
        return sb.toString();
    }
}