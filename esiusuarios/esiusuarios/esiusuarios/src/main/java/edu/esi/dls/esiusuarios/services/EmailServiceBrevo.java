package edu.esi.dls.esiusuarios.services;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.esi.dls.esiusuarios.auxiliares.HttpClient;

public class EmailServiceBrevo extends EmailService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailServiceBrevo.class);

    @Override
    public void sendEmail(String destinatario, Object... params) {
        String endpoint = getParam(params, "endpoint", "https://api.brevo.com/v3/smtp/email");
        JSONArray headers = getArrayParam(params, "headers");
        JSONObject payload = getObjectParam(params, "payload");

        if (payload == null) {
            new EmailServiceFalso().sendEmail(destinatario, params);
            return;
        }

        if (headers == null) {
            headers = new JSONArray();
            String apiKey = System.getenv("BREVO_API_KEY");
            headers.put("accept: application/json");
            headers.put("content-type: application/json");
            if (apiKey != null && !apiKey.isBlank()) {
                headers.put("api-key: " + apiKey);
            }
        }

        LOGGER.info("Brevo sendEmail destino parametro={}, payload.to={}", destinatario,
            payload.has("to") ? payload.getJSONArray("to").toString() : "[]");

        HttpClient client = new HttpClient();
        client.sendPost(endpoint, headers, payload);
    }

    private String getParam(Object[] params, String key, String defaultValue) {
        for (int i = 0; i < params.length - 1; i += 2) {
            if (key.equals(params[i])) {
                Object value = params[i + 1];
                return value == null ? defaultValue : value.toString();
            }
        }
        return defaultValue;
    }

    private JSONArray getArrayParam(Object[] params, String key) {
        for (int i = 0; i < params.length - 1; i += 2) {
            if (key.equals(params[i]) && params[i + 1] instanceof JSONArray) {
                return (JSONArray) params[i + 1];
            }
        }
        return null;
    }

    private JSONObject getObjectParam(Object[] params, String key) {
        for (int i = 0; i < params.length - 1; i += 2) {
            if (key.equals(params[i]) && params[i + 1] instanceof JSONObject) {
                return (JSONObject) params[i + 1];
            }
        }
        return null;
    }
}
