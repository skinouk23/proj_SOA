package com.mhotel.soap;
import javax.xml.ws.Endpoint;

public class SoapLauncher {
    public static void main(String[] args) {
        Endpoint.publish("http://localhost:8080/auth", new AuthService());
        Endpoint.publish("http://localhost:8080/booking", new BookingService());
        System.out.println("SOAP Services lancés sur port 8080");
    }
}