package com.mhotel.soap;
import javax.jws.WebMethod;
import javax.jws.WebService;

@WebService
public class AuthService {
    @WebMethod
    public String login(String username, String password) {
        if ("admin".equals(username) && "admin".equals(password)) return "ADMIN";
        if ("client".equals(username) && "client".equals(password)) return "CLIENT";
        return "NONE";
    }
}