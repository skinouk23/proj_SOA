package com.mhotel.soap;
import com.mhotel.model.DataStore;
import com.mhotel.model.Reservation;
import javax.jws.WebMethod;
import javax.jws.WebService;

@WebService
public class BookingService {
    @WebMethod
    public boolean reserver(String hotelNom, String clientName) {
        Reservation r = new Reservation(String.valueOf(System.currentTimeMillis()), hotelNom, clientName, "CONFIRMEE");
        DataStore.getInstance().addReservation(r);
        return true;
    }

    @WebMethod
    public String getHistorique(String clientName) {
        StringBuilder sb = new StringBuilder();
        for (Reservation r : DataStore.getInstance().getReservations()) {
            if (r.getClientNom().equals(clientName)) {
                sb.append(r.getHotelNom()).append(" (").append(r.getStatut()).append(");");
            }
        }
        return sb.toString();
    }
}