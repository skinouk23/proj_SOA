package com.mhotel.rest.Hotel;

import com.mhotel.model.Hotel;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Arrays;
import java.util.List;

@Path("/hotels")
public class HotelResource {

    @GET
    @Produces(MediaType.APPLICATION_XML)
    public List<Hotel> getHotels() {
        return com.mhotel.model.DataStore.getInstance().getHotels();
    }

    @POST
    @Consumes(MediaType.APPLICATION_XML)
    public String addHotel(Hotel hotel) {
        // AJOUTEZ CETTE LIGNE DE DEBUG :
        System.out.println(">>> REÇU HOTEL : " + hotel.getNom() + " Prix: " + hotel.getPrix());

        com.mhotel.model.DataStore.getInstance().addHotel(hotel);
        return "Hotel ajouté";
    }
}