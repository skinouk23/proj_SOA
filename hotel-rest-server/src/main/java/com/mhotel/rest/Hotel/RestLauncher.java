package com.mhotel.rest.Hotel;
import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import java.net.URI;

public class RestLauncher {
    public static void main(String[] args) {
        String url = "http://localhost:8081/"; // Port différent de SOAP (8080)
        ResourceConfig config = new ResourceConfig().packages("com.mhotel.rest");
        JdkHttpServerFactory.createHttpServer(URI.create(url), config);
        System.out.println("Service REST lancé sur : " + url + "hotels");
    }
}