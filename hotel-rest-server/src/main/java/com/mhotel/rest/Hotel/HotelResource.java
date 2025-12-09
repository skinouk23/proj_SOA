package com.mhotel.rest.Hotel;

import com.mhotel.model.Hotel;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Arrays;
import java.util.List;

@Path("/hotels")
public class HotelResource {
    @GET
    @Produces(MediaType.APPLICATION_XML) // On veut du XML
    public List<Hotel> getHotels() {
        return Arrays.asList(
                new Hotel("1", "Ibis Tunis", 120.0),
                new Hotel("2", "Marriott Sousse", 250.0)
        );
    }
}