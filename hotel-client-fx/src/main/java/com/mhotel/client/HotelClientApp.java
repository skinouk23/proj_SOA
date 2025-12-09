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
        VBox root = new VBox(20); // Espacement vertical de 20px
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(40)); // Marge interne globale

        // Titre
        Label lblTitle = new Label("CONNEXION HOTEL");
        lblTitle.getStyleClass().add("header-label"); // Application du style CSS

        // Champs
        TextField txtUser = new TextField("client");
        txtUser.setPromptText("Nom d'utilisateur");
        txtUser.setMaxWidth(300); // Pour ne pas qu'il prenne toute la largeur

        PasswordField txtPass = new PasswordField();
        txtPass.setText("client");
        txtPass.setPromptText("Mot de passe");
        txtPass.setMaxWidth(300);

        // Bouton
        Button btnLogin = new Button("SE CONNECTER");
        btnLogin.setPrefWidth(200); // Largeur du bouton

        Label lblError = new Label();
        lblError.setStyle("-fx-text-fill: #ef233c; -fx-font-weight: bold;"); // Rouge erreur direct

        // Action
        btnLogin.setOnAction(e -> {
            String role = callSoapAuth(txtUser.getText(), txtPass.getText());
            if ("ADMIN".equals(role)) {
                currentUser = txtUser.getText();
                showAdminScreen();
            } else if ("CLIENT".equals(role)) {
                currentUser = txtUser.getText();
                showClientScreen();
            } else {
                lblError.setText("Accès refusé. Vérifiez vos identifiants.");
            }
        });

        root.getChildren().addAll(lblTitle, txtUser, txtPass, btnLogin, lblError);

        // Création de la scène avec taille fixe
        Scene scene = new Scene(root, 500, 450);
        // Chargement du CSS
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());

        primaryStage.setTitle("Hotel App - Login");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // --- 2. ÉCRAN CLIENT ---
    private void showClientScreen() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(20));

        // --- EN-TÊTE ---
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 0, 20, 0));
        Label title = new Label("Espace Client : " + currentUser);
        title.getStyleClass().add("header-label");
        header.getChildren().add(title);
        root.setTop(header);

        // --- CENTRE (Liste) ---
        VBox centerLayout = new VBox(15);

        // 1. On déclare la liste AVANT le bouton
        ListView<String> listHotels = new ListView<>();
        VBox.setVgrow(listHotels, Priority.ALWAYS);

        // 2. Le bouton utilise maintenant la bonne variable "listHotels"
        Button btnRefresh = new Button("Actualiser le Catalogue");
        btnRefresh.getStyleClass().add("button-action");
        btnRefresh.setMaxWidth(Double.MAX_VALUE);
        btnRefresh.setOnAction(e -> loadHotelsRest(listHotels));

        centerLayout.getChildren().addAll(btnRefresh, listHotels);
        root.setCenter(centerLayout);

        // --- BAS (Actions) ---
        VBox bottomLayout = new VBox(15);
        bottomLayout.setPadding(new Insets(20, 0, 0, 0));

        Button btnBook = new Button("Réserver l'hôtel sélectionné");
        btnBook.setMaxWidth(Double.MAX_VALUE);
        btnBook.setOnAction(e -> {
            String selected = listHotels.getSelectionModel().getSelectedItem();
            if(selected != null) callSoapBooking(selected);
            else new Alert(Alert.AlertType.WARNING, "Veuillez choisir un hôtel").show();
        });

        TextArea historyArea = new TextArea();
        historyArea.setPrefHeight(100);
        historyArea.setEditable(false);
        historyArea.setPromptText("L'historique s'affichera ici...");

        Button btnHistory = new Button("Voir mes réservations");
        btnHistory.getStyleClass().add("button-action");
        btnHistory.setMaxWidth(Double.MAX_VALUE);
        btnHistory.setOnAction(e -> loadHistorySoap(historyArea));

        bottomLayout.getChildren().addAll(btnBook, btnHistory, historyArea);
        root.setBottom(bottomLayout);

        Scene scene = new Scene(root, 600, 700);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());

        primaryStage.setTitle("Hotel App - Catalogue");
        primaryStage.setScene(scene);
    }

    // --- 3. ÉCRAN ADMIN ---
    private void showAdminScreen() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(20));

        // --- 1. EN-TÊTE (Titre + Logout) ---
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 0, 30, 0));
        header.setSpacing(20);

        Label title = new Label("Panneau Administrateur");
        title.getStyleClass().add("header-label");

        // Un spacer pour pousser le bouton logout à droite
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnLogout = new Button("Déconnexion");
        btnLogout.getStyleClass().add("button-action"); // Style Bleu
        btnLogout.setOnAction(e -> showLoginScreen());

        header.getChildren().addAll(title, spacer, btnLogout);
        root.setTop(header);

        // --- 2. CENTRE (Formulaire d'ajout) ---
        VBox formPanel = new VBox(20);
        formPanel.setAlignment(Pos.CENTER);
        // On limite la largeur pour que ça ressemble à une fiche produit
        formPanel.setMaxWidth(400);
        formPanel.setPadding(new Insets(30));
        // Petit fond légèrement plus clair pour le formulaire
        formPanel.setStyle("-fx-background-color: rgba(255, 255, 255, 0.05); -fx-background-radius: 20;");

        Label lblForm = new Label("Ajouter un nouvel hôtel");
        lblForm.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");

        // Champ ID
        VBox boxId = new VBox(5);
        Label lblId = new Label("Identifiant Unique");
        TextField txtId = new TextField();
        txtId.setPromptText("Ex: 101");
        boxId.getChildren().addAll(lblId, txtId);

        // Champ Nom
        VBox boxNom = new VBox(5);
        Label lblNom = new Label("Nom de l'hôtel");
        TextField txtNom = new TextField();
        txtNom.setPromptText("Ex: Sheraton Tunis");
        boxNom.getChildren().addAll(lblNom, txtNom);

        // Champ Prix
        VBox boxPrix = new VBox(5);
        Label lblPrix = new Label("Prix par nuit (TND)");
        TextField txtPrix = new TextField();
        txtPrix.setPromptText("Ex: 250.0");
        boxPrix.getChildren().addAll(lblPrix, txtPrix);

        // Bouton d'action
        Button btnAdd = new Button("PUBLIER L'OFFRE");
        btnAdd.setMaxWidth(Double.MAX_VALUE); // Prend toute la largeur du formulaire
        btnAdd.setPadding(new Insets(15));

        // Logique du bouton
        btnAdd.setOnAction(e -> {
            try {
                // Vérification simple
                if(txtId.getText().isEmpty() || txtNom.getText().isEmpty() || txtPrix.getText().isEmpty()){
                    new Alert(Alert.AlertType.WARNING, "Veuillez remplir tous les champs").show();
                    return;
                }

                String id = txtId.getText();
                String nom = txtNom.getText();
                double prix = Double.parseDouble(txtPrix.getText());

                Hotel h = new Hotel(id, nom, prix);
                postHotelRest(h);

                // Reset des champs après ajout
                txtId.clear();
                txtNom.clear();
                txtPrix.clear();

            } catch (NumberFormatException ex) {
                new Alert(Alert.AlertType.ERROR, "Le prix doit être un nombre (ex: 120.5)").show();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        formPanel.getChildren().addAll(lblForm, boxId, boxNom, boxPrix, btnAdd);
        root.setCenter(formPanel);

        // --- 3. Configuration Scène ---
        Scene scene = new Scene(root, 700, 600);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());

        primaryStage.setTitle("Hotel App - Administration");
        primaryStage.setScene(scene);
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