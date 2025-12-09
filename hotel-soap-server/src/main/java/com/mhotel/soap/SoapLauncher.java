package com.mhotel.soap;
import javax.xml.ws.Endpoint;

public class SoapLauncher {
    public static void main(String[] args) {
        String url = "http://localhost:8080/booking";
        Endpoint.publish("http://localhost:8080/auth", new AuthService());
        Endpoint.publish("http://localhost:8080/booking", new BookingService());
        System.out.println("Service SOAP lancé sur : " + url + "?wsdl");
    }
}