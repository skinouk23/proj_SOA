package com.mhotel.client;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class HotelClientApp extends Application {

    private ListView<String> hotelList = new ListView<>();
    private TextArea consoleArea = new TextArea();

    // URLs des services
    private static final String REST_URL = "http://localhost:8081/hotels";
    private static final String SOAP_URL = "http://localhost:8080/booking";

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        // --- 1. Layout Principal ---
        BorderPane root = new BorderPane();
        Label title = new Label("Système de Réservation SOA");
        title.getStyleClass().add("title-label");
        BorderPane.setAlignment(title, Pos.CENTER);
        root.setTop(title);

        // --- 2. Partie Gauche : Catalogue (REST) ---
        VBox leftPanel = new VBox(10);
        leftPanel.setPadding(new Insets(15));
        Label lblCat = new Label("Catalogue Hôtels (REST)");
        lblCat.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");

        Button btnLoad = new Button("Charger la liste (XML)");
        btnLoad.setMaxWidth(Double.MAX_VALUE);
        btnLoad.setOnAction(e -> loadHotelsRest());

        VBox.setVgrow(hotelList, Priority.ALWAYS);
        leftPanel.getChildren().addAll(lblCat, btnLoad, hotelList);

        // --- 3. Partie Droite : Réservation (SOAP) ---
        VBox rightPanel = new VBox(15);
        rightPanel.setPadding(new Insets(15));
        rightPanel.setStyle("-fx-background-color: #333; -fx-background-radius: 10;");

        Label lblBook = new Label("Réserver une chambre (SOAP)");
        lblBook.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");

        TextField txtHotelId = new TextField();
        txtHotelId.setPromptText("ID de l'hôtel (ex: 1)");

        TextField txtClientName = new TextField();
        txtClientName.setPromptText("Nom du client");

        Button btnBook = new Button("Confirmer la réservation");
        btnBook.setMaxWidth(Double.MAX_VALUE);
        btnBook.setOnAction(e -> bookHotelSoap(txtHotelId.getText(), txtClientName.getText()));

        // Zone de log pour voir les réponses XML
        consoleArea.setEditable(false);
        consoleArea.setPrefHeight(150);
        consoleArea.setStyle("-fx-control-inner-background: #000; -fx-font-family: Consolas;");

        rightPanel.getChildren().addAll(lblBook, new Label("ID Hôtel:"), txtHotelId, new Label("Nom Client:"), txtClientName, btnBook, new Label("Logs serveur:"), consoleArea);

        // Assemblage
        SplitPane splitPane = new SplitPane();
        splitPane.getItems().addAll(leftPanel, rightPanel);
        root.setCenter(splitPane);

        // --- 4. Configuration de la Scène ---
        Scene scene = new Scene(root, 900, 600);

        // Chargement du CSS
        String css = getClass().getResource("/style.css").toExternalForm();
        scene.getStylesheets().add(css);

        primaryStage.setTitle("Hotel SOA Client - Special Edition");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // --- LOGIQUE MÉTIER ---

    private void loadHotelsRest() {
        consoleArea.appendText(">>> Appel REST GET " + REST_URL + "...\n");
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(REST_URL)).GET().build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenAccept(responseXml -> {
                    // On est dans un thread séparé, on doit revenir au thread UI pour afficher
                    javafx.application.Platform.runLater(() -> {
                        consoleArea.appendText("Réponse XML reçue (REST).\n");
                        parseAndDisplayHotels(responseXml);
                    });
                })
                .exceptionally(e -> {
                    javafx.application.Platform.runLater(() -> consoleArea.appendText("Erreur: " + e.getMessage() + "\n"));
                    return null;
                });
    }

    private void bookHotelSoap(String hotelId, String clientName) {
        if(hotelId.isEmpty() || clientName.isEmpty()) {
            showAlert("Erreur", "Veuillez remplir tous les champs !");
            return;
        }

        consoleArea.appendText(">>> Construction de l'enveloppe SOAP...\n");

        // Construction manuelle du XML SOAP (C'est ce que font les outils en arrière plan)
        String soapEnvelope =
                "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:soap=\"http://soap.mhotel.com/\">\n" +
                        "   <soapenv:Header/>\n" +
                        "   <soapenv:Body>\n" +
                        "      <soap:reserver>\n" +
                        "         <arg0>" + hotelId + "</arg0>\n" +
                        "         <arg1>" + clientName + "</arg1>\n" +
                        "      </soap:reserver>\n" +
                        "   </soapenv:Body>\n" +
                        "</soapenv:Envelope>";

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(SOAP_URL))
                .header("Content-Type", "text/xml") // Important pour SOAP
                .POST(HttpRequest.BodyPublishers.ofString(soapEnvelope))
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenAccept(responseXml -> {
                    javafx.application.Platform.runLater(() -> {
                        consoleArea.appendText("Réponse SOAP reçue :\n" + responseXml + "\n");
                        if(responseXml.contains("SUCCES")) {
                            showAlert("Succès", "Réservation réussie !");
                        }
                    });
                });
    }

    // Parsing "rapide" du XML pour l'affichage (sans JAXB pour simplifier le client UI)
    private void parseAndDisplayHotels(String xml) {
        hotelList.getItems().clear();
        // C'est du parsing manuel simple pour l'exemple. En prod, on utiliserait JAXB ou DOM.
        String[] hotels = xml.split("</hotel>");
        for (String h : hotels) {
            if (h.contains("<nom>")) {
                String nom = h.substring(h.indexOf("<nom>") + 5, h.indexOf("</nom>"));
                String prix = h.substring(h.indexOf("<prix>") + 6, h.indexOf("</prix>"));
                String id = h.substring(h.indexOf("<id>") + 4, h.indexOf("</id>"));
                hotelList.getItems().add(id + " - " + nom + " (" + prix + " TND)");
            }
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}