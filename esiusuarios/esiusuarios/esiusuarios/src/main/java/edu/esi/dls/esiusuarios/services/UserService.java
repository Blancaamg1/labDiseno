package edu.esi.dls.esiusuarios.services;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.esi.dls.esiusuarios.auxiliares.Manager;
import edu.esi.dls.esiusuarios.dao.TokenDao;
import edu.esi.dls.esiusuarios.dao.UserDao;
import edu.esi.dls.esiusuarios.dto.UserInfoDto;
import edu.esi.dls.esiusuarios.model.Token;
import edu.esi.dls.esiusuarios.model.User;

@Service
public class UserService {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserService.class);

    private final UserDao repository;
    private final TokenDao tokenDao;

    @Value("${brevo.force.to:entradaseventosesi@gmail.com}")
    private String forcedRecipientEmail;

    @Value("${brevo.force.name:Pruebas ESI}")
    private String forcedRecipientName;

    private static final long TOKEN_VALIDITY_HOURS = 24L;
    private static final String CONFIRM_BASE_URL = "http://localhost:8081/users/confirm/";

    @Autowired
    public UserService(UserDao repository, TokenDao tokenDao) {
        this.repository = repository;
        this.tokenDao = tokenDao;
        if (repository.count() == 0) {
            repository.save(new User("Pepe", "pepe@example.com", "pepe123", generateSessionToken(), System.currentTimeMillis()));
            repository.save(new User("Ana", "ana@example.com", "ana123", generateSessionToken(), System.currentTimeMillis()));
        }
    }

    public String login(String name, String password) {
        Optional<User> user = repository.findByNameIgnoreCase(name);
        if (user.isPresent()
                && user.get().getPassword().equals(password)
                && user.get().getValidationDate() != null) {
            return user.get().getToken();
        }
        return null;
    }

    public String checkToken(String token) {
        Optional<User> user = repository.findByToken(token);
        if (user.isPresent() && user.get().getValidationDate() == null) {
            return null;
        }
        return user.map(User::getName).orElse(null);
    }

    public UserInfoDto getUserInfo(String token) {
        Optional<User> user = repository.findByToken(token);
        if (user.isPresent() && user.get().getValidationDate() == null) {
            return null;
        }
        return user.map(u -> {
            UserInfoDto dto = new UserInfoDto();
            dto.setId(u.getId());
            dto.setName(u.getName());
            dto.setEmail(u.getEmail());
            return dto;
        }).orElse(null);
    }

    @Transactional
    public String register(String username, String email, String pwd1) {
        if (repository.findByNameIgnoreCase(username).isPresent()) {
            return null;
        }

        if (repository.findByEmailIgnoreCase(email).isPresent()) {
            return null;
        }

        User newUser = new User(username, email, pwd1, generateSessionToken(), (Long) null);
        repository.save(newUser);

        String confirmationToken = UUID.randomUUID().toString();
        Token tokenEntity = new Token(
            confirmationToken,
            LocalDateTime.now(),
            LocalDateTime.now().plusHours(TOKEN_VALIDITY_HOURS),
            newUser
        );
        tokenDao.save(tokenEntity);

        this.sendEmail(newUser, tokenEntity);

        return "Le hemos enviado un correo de confirmación a " + forcedRecipientEmail;
    }

    @Transactional
    public String confirm(String tokenValue) {
        Token token = tokenDao.findByValue(tokenValue)
            .orElseThrow(() -> new IllegalArgumentException("Token de confirmación inválido"));

        long tokenMillis = token.getCreatedAt().toInstant(ZoneOffset.UTC).toEpochMilli();
        long time = System.currentTimeMillis();
        if (time - tokenMillis > 24L * 60L * 60L * 1000L) {
            tokenDao.delete(token);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encuentra el token o ha caducado");
        }

        User user = token.getUser();
        user.setValidationDate(time);
        repository.save(user);
        tokenDao.delete(token);

        return "Cuenta confirmada correctamente";
    }

    private void sendEmail(User user, Token token) {
        try {
            String body = Manager.getInstance().readFile("welcome.html.txt");
            body = body.replace("#TOKEN#", token.getValue());

            String paramsText = Manager.getInstance().readFile("brevo.parameters.txt");
            JSONObject emailParameters = new JSONObject(paramsText);

            String endPoint = emailParameters.getString("endpoint");
            JSONArray headers = emailParameters.getJSONArray("headers");

            String apiKey = System.getenv("BREVO_API_KEY");
            if (apiKey != null && !apiKey.isBlank()) {
                for (int i = 0; i < headers.length(); i++) {
                    String header = headers.getString(i);
                    if (header.trim().toLowerCase().startsWith("api-key")) {
                        headers.put(i, "api-key: " + apiKey);
                    }
                }
            }

            JSONObject payload = new JSONObject();
            payload.put("sender", emailParameters.getJSONObject("sender"));

            JSONArray to = new JSONArray();
            to.put(new JSONObject().put("email", forcedRecipientEmail).put("name", forcedRecipientName));
            payload.put("to", to);
            payload.put("subject", emailParameters.getString("subject"));
            payload.put("htmlContent", body);

            LOGGER.info("Enviando correo de confirmacion a {} con asunto '{}'", forcedRecipientEmail, emailParameters.getString("subject"));

            Manager.getInstance().getEmailService().sendEmail(
                forcedRecipientEmail,
                "endpoint", endPoint,
                "headers", headers,
                "payload", payload
            );
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se pudo enviar email de confirmacion: " + e.getMessage());
        }
    }

    private String generateSessionToken() {
        return UUID.randomUUID().toString();
    }
}