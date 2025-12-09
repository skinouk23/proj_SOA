package com.mhotel.rest;

import com.mhotel.model.DataStore;
import com.mhotel.model.Hotel;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Path("/hotels")
public class HotelResource {
    @GET
    @Produces(MediaType.APPLICATION_XML)
    public List<Hotel> getHotels() {
        return DataStore.getInstance().getHotels();
    }

    @POST
    @Consumes(MediaType.APPLICATION_XML)
    public String addHotel(Hotel hotel) {
        System.out.println("Ajout Hotel: " + hotel.getNom());
        DataStore.getInstance().addHotel(hotel);
        return "OK";
    }
}