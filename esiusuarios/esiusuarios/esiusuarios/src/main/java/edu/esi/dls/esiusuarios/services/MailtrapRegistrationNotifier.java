package edu.esi.dls.esiusuarios.services;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class MailtrapRegistrationNotifier {
    private static final Logger LOGGER = LoggerFactory.getLogger(MailtrapRegistrationNotifier.class);
    private static final String MAILTRAP_SANDBOX_URL = "https://sandbox.api.mailtrap.io/api/send/";

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Value("${mailtrap.api.token:}")
    private String apiToken;

    @Value("${mailtrap.inbox.id:0}")
    private long inboxId;

    @Value("${mailtrap.notify.to:mario.munoz9@alu.uclm.es}")
    private String notifyTo;

    @Value("${mailtrap.from.email:hello@example.com}")
    private String fromEmail;

    @Value("${mailtrap.from.name:ESIUsuarios}")
    private String fromName;

    public void notifyNewRegistration(String username, String email) {
        if (apiToken == null || apiToken.isBlank() || inboxId <= 0) {
            LOGGER.warn("Mailtrap no configurado. Se omite notificacion de registro para {}", username);
            return;
        }

        JSONObject payload = new JSONObject()
            .put("from", new JSONObject()
                .put("email", fromEmail)
                .put("name", fromName))
            .put("to", new JSONArray().put(new JSONObject().put("email", notifyTo)))
            .put("subject", "Nuevo usuario registrado")
            .put("text", "Se ha registrado un nuevo usuario en ESIUsuarios.\n"
                + "Usuario: " + username + "\n"
                + "Correo: " + email)
            .put("category", "Registro usuarios");

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(MAILTRAP_SANDBOX_URL + inboxId))
            .header("Content-Type", "application/json")
            .header("Api-Token", apiToken)
            .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
            .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                LOGGER.error("Error enviando notificacion a Mailtrap. Status: {}, body: {}",
                    response.statusCode(), response.body());
            }
        } catch (IOException | InterruptedException ex) {
            LOGGER.error("No se pudo enviar notificacion de registro a Mailtrap", ex);
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
    }
}