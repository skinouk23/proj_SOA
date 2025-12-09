package com.mhotel.client;

import com.mhotel.model.Hotel; // Assurez-vous d'avoir accès au modèle (dependance maven)
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.io.StringWriter;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class HotelClientApp extends Application {

    private Stage primaryStage;
    private String currentUser = "";

    // URLs
    private static final String AUTH_URL = "http://localhost:8080/auth";
    private static final String REST_URL = "http://localhost:8081/hotels";
    private static final String BOOKING_URL = "http://localhost:8080/booking";

    public static void main(String[] args) { launch(args); }

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        showLoginScreen();
    }

    // --- 1. ÉCRAN LOGIN ---
    private void showLoginScreen() {
        VBox root = new VBox(15);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #2b2b2b;");

        Label lblTitle = new Label("Connexion Hotel SOA");
        lblTitle.setStyle("-fx-text-fill: white; -fx-font-size: 20px;");

        TextField txtUser = new TextField("client"); // defaut
        PasswordField txtPass = new PasswordField();
        txtPass.setText("client"); // defaut

        Button btnLogin = new Button("Se connecter");
        btnLogin.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");

        Label lblError = new Label();
        lblError.setStyle("-fx-text-fill: red;");

        btnLogin.setOnAction(e -> {
            String role = callSoapAuth(txtUser.getText(), txtPass.getText());
            if ("ADMIN".equals(role)) {
                currentUser = txtUser.getText();
                showAdminScreen();
            } else if ("CLIENT".equals(role)) {
                currentUser = txtUser.getText();
                showClientScreen();
            } else {
                lblError.setText("Identifiants incorrects (Essayer admin/admin ou client/client)");
            }
        });

        root.getChildren().addAll(lblTitle, txtUser, txtPass, btnLogin, lblError);
        Scene scene = new Scene(root, 400, 300);
        primaryStage.setTitle("Login");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // --- 2. ÉCRAN CLIENT ---
    private void showClientScreen() {
        BorderPane root = new BorderPane();
        root.setTop(new Label("Bienvenue Client : " + currentUser));

        // Liste Hotels (REST)
        ListView<String> listHotels = new ListView<>();
        Button btnRefresh = new Button("Actualiser Catalogue");
        btnRefresh.setOnAction(e -> loadHotelsRest(listHotels));

        // Réservation
        Button btnBook = new Button("Réserver Sélection");
        btnBook.setOnAction(e -> {
            String selected = listHotels.getSelectionModel().getSelectedItem();
            if(selected != null) callSoapBooking(selected);
        });

        // Historique
        TextArea historyArea = new TextArea();
        Button btnHistory = new Button("Voir mes réservations");
        btnHistory.setOnAction(e -> loadHistorySoap(historyArea));

        VBox center = new VBox(10, btnRefresh, listHotels, btnBook, btnHistory, historyArea);
        center.setPadding(new Insets(10));
        root.setCenter(center);

        primaryStage.setScene(new Scene(root, 600, 500));
    }

    // --- 3. ÉCRAN ADMIN ---
    private void showAdminScreen() {
        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.CENTER);

        Label lbl = new Label("PANEL ADMIN - Ajouter un Hôtel");
        TextField txtId = new TextField(); txtId.setPromptText("ID");
        TextField txtNom = new TextField(); txtNom.setPromptText("Nom Hôtel");
        TextField txtPrix = new TextField(); txtPrix.setPromptText("Prix");

        Button btnAdd = new Button("Ajouter au Catalogue (REST POST)");

        btnAdd.setOnAction(e -> {
            try {
                Hotel h = new Hotel(txtId.getText(), txtNom.getText(), Double.parseDouble(txtPrix.getText()));
                postHotelRest(h);
            } catch (Exception ex) { ex.printStackTrace(); }
        });

        root.getChildren().addAll(lbl, txtId, txtNom, txtPrix, btnAdd);
        primaryStage.setScene(new Scene(root, 600, 400));
    }

    // --- APPELS RESEAU ---

    private String callSoapAuth(String u, String p) {
        // En vrai projet : parser le XML proprement. Ici hack rapide pour démo.
        if(u.equals("admin") && p.equals("admin")) return "ADMIN";
        if(u.equals("client") && p.equals("client")) return "CLIENT";
        return "NONE";
    }

    private void loadHotelsRest(ListView<String> listView) {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(REST_URL)).GET().build();
        client.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                .thenAccept(resp -> Platform.runLater(() -> {
                    listView.getItems().clear();
                    // Parsing manuel simplifié
                    String[] parts = resp.body().split("</hotel>");
                    for(String s : parts) {
                        if(s.contains("<nom>")) {
                            String n = s.substring(s.indexOf("<nom>") + 5, s.indexOf("</nom>"));
                            listView.getItems().add(n);
                        }
                    }
                }));
    }

    private void postHotelRest(Hotel h) {
        try {
            // Convertir objet Hotel en XML
            JAXBContext ctx = JAXBContext.newInstance(Hotel.class);
            Marshaller m = ctx.createMarshaller();
            StringWriter sw = new StringWriter();
            m.marshal(h, sw);
            String xmlBody = sw.toString();

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(REST_URL))
                    .header("Content-Type", "application/xml")
                    .POST(HttpRequest.BodyPublishers.ofString(xmlBody))
                    .build();

            client.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(r -> Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.INFORMATION, "Hotel Ajouté !");
                        alert.show();
                    }));
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void callSoapBooking(String hotelName) {
        String soap =
                "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:soap=\"http://soap.mhotel.com/\">" +
                        "<soapenv:Header/><soapenv:Body><soap:reserver>" +
                        "<arg0>" + hotelName + "</arg0><arg1>" + currentUser + "</arg1>" +
                        "</soap:reserver></soapenv:Body></soapenv:Envelope>";

        postSoap(BOOKING_URL, soap, "Réservation envoyée !");
    }

    private void loadHistorySoap(TextArea area) {
        String soap =
                "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:soap=\"http://soap.mhotel.com/\">" +
                        "<soapenv:Header/><soapenv:Body><soap:getHistorique>" +
                        "<arg0>" + currentUser + "</arg0>" +
                        "</soap:getHistorique></soapenv:Body></soapenv:Envelope>";

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BOOKING_URL))
                .header("Content-Type", "text/xml")
                .POST(HttpRequest.BodyPublishers.ofString(soap))
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(r -> Platform.runLater(() -> {
                    // Extraction brute du resultat (pas propre mais efficace pour demo)
                    String res = r.body();
                    if(res.contains("<return>")) {
                        String content = res.substring(res.indexOf("<return>") + 8, res.indexOf("</return>"));
                        area.setText(content.replace(";", "\n"));
                    }
                }));
    }

    private void postSoap(String url, String soapXml, String successMsg) {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "text/xml")
                .POST(HttpRequest.BodyPublishers.ofString(soapXml))
                .build();
        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(r -> Platform.runLater(() -> {
                    new Alert(Alert.AlertType.INFORMATION, successMsg).show();
                }));
    }
}