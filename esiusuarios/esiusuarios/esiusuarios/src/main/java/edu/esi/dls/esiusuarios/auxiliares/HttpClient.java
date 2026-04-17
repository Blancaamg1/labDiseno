package edu.esi.dls.esiusuarios.auxiliares;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class HttpClient {

    public String sendPost(String url, JSONArray headers, JSONObject payload) {
        try {
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()));

            for (int i = 0; i < headers.length(); i++) {
                String header = headers.getString(i);
                int separator = header.indexOf(':');
                if (separator <= 0) {
                    continue;
                }
                String headerName = header.substring(0, separator).trim();
                String headerValue = header.substring(separator + 1).trim();
                builder.header(headerName, headerValue);
            }

            HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Error enviando email (" + response.statusCode() + "): " + response.body());
            }
            return response.body();
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }
}
