package com.mhotel.client;

import com.mhotel.model.Hotel;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import java.io.StringWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class HotelClientApp extends Application {

    private Stage primaryStage;
    private String currentUser = "";

    private static final String AUTH_URL = "http://localhost:8080/auth";
    private static final String REST_URL = "http://localhost:8081/hotels";
    private static final String BOOKING_URL = "http://localhost:8080/booking";

    public static void main(String[] args) { launch(args); }

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        showLoginScreen();
    }

    // 1. LOGIN SCREEN
    private void showLoginScreen() {
        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(40));

        Label lblTitle = new Label("CONNEXION HOTEL");
        lblTitle.getStyleClass().add("header-label");

        TextField txtUser = new TextField("client");
        txtUser.setPromptText("Utilisateur");
        txtUser.setMaxWidth(300);

        PasswordField txtPass = new PasswordField();
        txtPass.setText("client");
        txtPass.setPromptText("Mot de passe");
        txtPass.setMaxWidth(300);

        Button btnLogin = new Button("SE CONNECTER");
        btnLogin.setPrefWidth(200);

        Label lblError = new Label();
        lblError.setStyle("-fx-text-fill: #ef233c; -fx-font-weight: bold;");

        btnLogin.setOnAction(e -> {
            String role = callSoapAuth(txtUser.getText(), txtPass.getText());
            if ("ADMIN".equals(role)) {
                currentUser = txtUser.getText();
                showAdminScreen();
            } else if ("CLIENT".equals(role)) {
                currentUser = txtUser.getText();
                showClientScreen();
            } else {
                lblError.setText("Erreur identifiants.");
            }
        });

        root.getChildren().addAll(lblTitle, txtUser, txtPass, btnLogin, lblError);
        Scene scene = new Scene(root, 500, 450);
        loadCss(scene);
        primaryStage.setTitle("Login");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // 2. CLIENT SCREEN
    private void showClientScreen() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(20));

        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 0, 20, 0));
        Label title = new Label("Espace Client : " + currentUser);
        title.getStyleClass().add("header-label");
        header.getChildren().add(title);
        root.setTop(header);

        // Liste
        ListView<String> listHotels = new ListView<>();
        VBox.setVgrow(listHotels, Priority.ALWAYS);

        Button btnRefresh = new Button("Actualiser le Catalogue");
        btnRefresh.getStyleClass().add("button-action");
        btnRefresh.setMaxWidth(Double.MAX_VALUE);
        btnRefresh.setOnAction(e -> loadHotelsRest(listHotels));

        VBox centerLayout = new VBox(15);
        centerLayout.getChildren().addAll(btnRefresh, listHotels);
        root.setCenter(centerLayout);

        // Actions
        VBox bottomLayout = new VBox(15);
        bottomLayout.setPadding(new Insets(20, 0, 0, 0));

        Button btnBook = new Button("Réserver l'hôtel sélectionné");
        btnBook.setMaxWidth(Double.MAX_VALUE);
        btnBook.setOnAction(e -> {
            String selected = listHotels.getSelectionModel().getSelectedItem();
            if(selected != null) callSoapBooking(selected);
            else showStyledAlert(Alert.AlertType.WARNING, "Attention", "Veuillez choisir un hôtel");
        });

        Label lblHist = new Label("Mes Réservations :");
        lblHist.setStyle("-fx-font-weight: bold; -fx-text-fill: #8d99ae;");

        ListView<String> historyList = new ListView<>();
        historyList.getStyleClass().add("history-list");
        historyList.setPrefHeight(150);

        Button btnHistory = new Button("Voir mes réservations");
        btnHistory.getStyleClass().add("button-action");
        btnHistory.setMaxWidth(Double.MAX_VALUE);
        btnHistory.setOnAction(e -> loadHistorySoap(historyList));

        bottomLayout.getChildren().addAll(btnBook, lblHist, historyList, btnHistory);
        root.setBottom(bottomLayout);

        Scene scene = new Scene(root, 600, 750);
        loadCss(scene);
        primaryStage.setScene(scene);
    }

    // 3. ADMIN SCREEN
    private void showAdminScreen() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(20));

        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 0, 30, 0));
        header.setSpacing(20);
        Label title = new Label("Panneau Administrateur");
        title.getStyleClass().add("header-label");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button btnLogout = new Button("Déconnexion");
        btnLogout.getStyleClass().add("button-action");
        btnLogout.setOnAction(e -> showLoginScreen());
        header.getChildren().addAll(title, spacer, btnLogout);
        root.setTop(header);

        VBox formPanel = new VBox(20);
        formPanel.setAlignment(Pos.CENTER);
        formPanel.setMaxWidth(400);
        formPanel.setPadding(new Insets(30));
        formPanel.setStyle("-fx-background-color: rgba(255, 255, 255, 0.05); -fx-background-radius: 20;");

        Label lblForm = new Label("Ajouter un nouvel hôtel");
        lblForm.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");

        TextField txtId = new TextField(); txtId.setPromptText("ID");
        TextField txtNom = new TextField(); txtNom.setPromptText("Nom");
        TextField txtPrix = new TextField(); txtPrix.setPromptText("Prix");

        Button btnAdd = new Button("PUBLIER L'OFFRE");
        btnAdd.setMaxWidth(Double.MAX_VALUE);
        btnAdd.setOnAction(e -> {
            try {
                if(txtId.getText().isEmpty() || txtNom.getText().isEmpty() || txtPrix.getText().isEmpty()) return;
                Hotel h = new Hotel(txtId.getText(), txtNom.getText(), Double.parseDouble(txtPrix.getText()));
                postHotelRest(h);
                txtId.clear(); txtNom.clear(); txtPrix.clear();
            } catch (Exception ex) { ex.printStackTrace(); }
        });

        formPanel.getChildren().addAll(lblForm, new Label("ID:"), txtId, new Label("Nom:"), txtNom, new Label("Prix:"), txtPrix, btnAdd);
        root.setCenter(formPanel);

        Scene scene = new Scene(root, 700, 600);
        loadCss(scene);
        primaryStage.setScene(scene);
    }

    // --- HELPERS ---

    private void loadCss(Scene scene) {
        if(getClass().getResource("/style.css") != null)
            scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
    }

    private void loadHotelsRest(ListView<String> listView) {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(REST_URL)).GET().build();

        client.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                .thenAccept(resp -> Platform.runLater(() -> {
                    listView.getItems().clear();
                    String xml = resp.body();
                    String[] parts = xml.split("</hotel>");
                    for(String s : parts) {
                        if(s.contains("<nom>") && s.contains("<prix>")) {
                            try {
                                String nom = s.substring(s.indexOf("<nom>") + 5, s.indexOf("</nom>"));
                                String prix = s.substring(s.indexOf("<prix>") + 6, s.indexOf("</prix>"));
                                listView.getItems().add(nom + " - " + prix + " TND");
                            } catch (Exception e) {}
                        }
                    }
                }));
    }

    private void postHotelRest(Hotel h) {
        try {
            JAXBContext ctx = JAXBContext.newInstance(Hotel.class);
            Marshaller m = ctx.createMarshaller();
            StringWriter sw = new StringWriter();
            m.marshal(h, sw);
            String xml = sw.toString();

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(REST_URL))
                    .header("Content-Type", "application/xml")
                    .POST(HttpRequest.BodyPublishers.ofString(xml))
                    .build();

            client.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(r -> Platform.runLater(() -> {
                        if(r.statusCode() == 200 || r.statusCode() == 204)
                            showStyledAlert(Alert.AlertType.INFORMATION, "Succès", "Hôtel ajouté !");
                        else
                            showStyledAlert(Alert.AlertType.ERROR, "Erreur", "Code: " + r.statusCode());
                    }));
        } catch (Exception e) { e.printStackTrace(); }
    }

    private String callSoapAuth(String u, String p) {
        if(u.equals("admin") && p.equals("admin")) return "ADMIN";
        if(u.equals("client") && p.equals("client")) return "CLIENT";
        return "NONE";
    }

    private void callSoapBooking(String infoHotel) {
        // infoHotel contient "Nom - Prix", on prend juste le Nom
        String hotelName = infoHotel.split("-")[0].trim();
        String soap = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:soap=\"http://soap.mhotel.com/\"><soapenv:Header/><soapenv:Body><soap:reserver><arg0>" + hotelName + "</arg0><arg1>" + currentUser + "</arg1></soap:reserver></soapenv:Body></soapenv:Envelope>";

        postSoap(soap, "Réservation envoyée !");
    }

    private void loadHistorySoap(ListView<String> listView) {
        String soap = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:soap=\"http://soap.mhotel.com/\"><soapenv:Header/><soapenv:Body><soap:getHistorique><arg0>" + currentUser + "</arg0></soap:getHistorique></soapenv:Body></soapenv:Envelope>";

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BOOKING_URL))
                .header("Content-Type", "text/xml")
                .POST(HttpRequest.BodyPublishers.ofString(soap))
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(r -> Platform.runLater(() -> {
                    listView.getItems().clear();
                    String res = r.body();
                    if(res.contains("<return>")) {
                        String content = res.substring(res.indexOf("<return>") + 8, res.indexOf("</return>"));
                        if(!content.isEmpty()) listView.getItems().addAll(content.split(";"));
                        else listView.getItems().add("Aucune réservation.");
                    }
                }));
    }

    private void postSoap(String xml, String successMsg) {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BOOKING_URL))
                .header("Content-Type", "text/xml")
                .POST(HttpRequest.BodyPublishers.ofString(xml))
                .build();
        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(r -> Platform.runLater(() ->
                        showStyledAlert(Alert.AlertType.INFORMATION, "Info", successMsg)
                ));
    }

    private void showStyledAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        DialogPane dialogPane = alert.getDialogPane();
        if (getClass().getResource("/style.css") != null) {
            dialogPane.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        }
        alert.showAndWait();
    }
}