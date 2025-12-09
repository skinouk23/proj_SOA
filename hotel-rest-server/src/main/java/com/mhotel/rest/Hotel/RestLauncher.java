package com.mhotel.rest;
import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import java.net.URI;

public class RestLauncher {
    public static void main(String[] args) {
        ResourceConfig config = new ResourceConfig().packages("com.mhotel.rest");
        JdkHttpServerFactory.createHttpServer(URI.create("http://localhost:8081/"), config);
        System.out.println("REST Service lancé sur http://localhost:8081/hotels");
    }
}