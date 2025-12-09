package com.mhotel.soap;
import javax.jws.WebMethod;
import javax.jws.WebService;

@WebService
public class BookingService {
    @WebMethod
    public String reserver(String hotelId, String clientName) {
        return "SUCCES : Réservation confirmée pour " + clientName + " à l'hotel " + hotelId;
    }
}