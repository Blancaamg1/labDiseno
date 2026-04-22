package edu.esi.dls.esiusuarios.services;

import java.time.LocalDateTime;
import java.util.List;
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
import jakarta.servlet.http.HttpSession;

@Service
public class UserService {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserService.class);

    private final UserDao repository;
    private final TokenDao tokenDao;
    private final UserInputValidator userInputValidator;

    @Value("${brevo.force.to:entradaseventosesi@gmail.com}")
    private String forcedRecipientEmail;

    @Value("${brevo.force.name:Pruebas ESI}")
    private String forcedRecipientName;

    private static final long TOKEN_VALIDITY_HOURS = 24L;
    private static final long RESET_TOKEN_VALIDITY_HOURS = 1L;

    @Value("${frontend.reset-password-url:http://localhost:4200/restablecer-contrasena}")
    private String resetPasswordUrl;

    @Autowired
    public UserService(UserDao repository, TokenDao tokenDao, UserInputValidator userInputValidator) {
        this.repository = repository;
        this.tokenDao = tokenDao;
        this.userInputValidator = userInputValidator;
        if (repository.count() == 0) {
            repository.save(new User("Pepe", "pepe@example.com", "pepe123", generateSessionToken(), System.currentTimeMillis()));
            repository.save(new User("Ana", "ana@example.com", "ana123", generateSessionToken(), System.currentTimeMillis()));
        }
    }

    public String login(String name, String password) {
        this.userInputValidator.validateLoginData(name, password);

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

    public String getValidatedSessionUserName(HttpSession session) {
        String userToken = this.getSessionToken(session);
        String name = this.checkToken(userToken);
        if (name == null) {
            session.invalidate();
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid session");
        }
        return name;
    }

    public UserInfoDto getValidatedSessionUserInfo(HttpSession session) {
        String userToken = this.getSessionToken(session);
        UserInfoDto userInfo = this.getUserInfo(userToken);
        if (userInfo == null) {
            session.invalidate();
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid session");
        }
        return userInfo;
    }

    public String getSessionToken(HttpSession session) {
        Object userId = session.getAttribute("userId");
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No active session");
        }
        return userId.toString();
    }

    @Transactional
    public String register(String username, String email, String pwd1, String pwd2) {
        this.userInputValidator.validateRegistrationData(username, email, pwd1, pwd2);

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
            Token.PURPOSE_EMAIL_CONFIRMATION,
            LocalDateTime.now(),
            LocalDateTime.now().plusHours(TOKEN_VALIDITY_HOURS),
            newUser
        );
        tokenDao.save(tokenEntity);

        this.sendConfirmationEmail(newUser, tokenEntity);

        return "Le hemos enviado un correo de confirmación a " + forcedRecipientEmail;
    }

    @Transactional
    public String confirm(String tokenValue) {
        Token token = tokenDao.findByValueAndPurpose(tokenValue, Token.PURPOSE_EMAIL_CONFIRMATION)
            .orElseThrow(() -> new IllegalArgumentException("Token de confirmación inválido"));

        LocalDateTime now = LocalDateTime.now();
        if (token.isExpired(now)) {
            tokenDao.delete(token);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encuentra el token o ha caducado");
        }

        User user = token.getUser();
        user.setValidationDate(System.currentTimeMillis());
        repository.save(user);
        tokenDao.delete(token);

        return "Cuenta confirmada correctamente";
    }

    @Transactional
    public String requestPasswordReset(String email) {
        if (email == null || email.isBlank()) {
            return "Si el correo existe, se enviara un enlace para restablecer la contrasena.";
        }

        Optional<User> userOpt = repository.findByEmailIgnoreCase(email.trim().toLowerCase());
        if (userOpt.isEmpty()) {
            return "Si el correo existe, se enviara un enlace para restablecer la contrasena.";
        }

        User user = userOpt.get();

        List<Token> existingTokens = tokenDao.findByUserAndPurposeAndUsedAtIsNull(user, Token.PURPOSE_PASSWORD_RESET);
        LocalDateTime now = LocalDateTime.now();
        for (Token existingToken : existingTokens) {
            existingToken.setUsedAt(now);
            tokenDao.save(existingToken);
        }

        Token tokenEntity = new Token(
            UUID.randomUUID().toString(),
            Token.PURPOSE_PASSWORD_RESET,
            now,
            now.plusHours(RESET_TOKEN_VALIDITY_HOURS),
            user
        );
        tokenDao.save(tokenEntity);

        this.sendResetPasswordEmail(user, tokenEntity);

        return "Si el correo existe, se enviara un enlace para restablecer la contrasena.";
    }

    @Transactional
    public String resetPassword(String tokenValue, String newPassword, String confirmPassword) {
        if (tokenValue == null || tokenValue.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token de recuperacion requerido");
        }

        this.userInputValidator.validatePasswordResetData(newPassword, confirmPassword);

        Token token = tokenDao.findByValueAndPurpose(tokenValue.trim(), Token.PURPOSE_PASSWORD_RESET)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Token de recuperacion invalido"));

        LocalDateTime now = LocalDateTime.now();
        if (token.isUsed()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El enlace de recuperacion ya fue utilizado");
        }

        if (token.isExpired(now)) {
            throw new ResponseStatusException(HttpStatus.GONE, "El enlace de recuperacion ha caducado");
        }

        User user = token.getUser();
        user.setPassword(newPassword);
        if (user.getValidationDate() == null) {
            user.setValidationDate(System.currentTimeMillis());
        }
        user.setToken(generateSessionToken());
        repository.save(user);

        token.setUsedAt(now);
        tokenDao.save(token);

        return "Contrasena actualizada correctamente";
    }

    private void sendConfirmationEmail(User user, Token token) {
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

    private void sendResetPasswordEmail(User user, Token token) {
        try {
            String body = Manager.getInstance().readFile("password-reset.html.txt");
            String resetLink = this.resetPasswordUrl + "?token=" + token.getValue();
            body = body.replace("#USER_NAME#", user.getName() == null ? "usuario" : user.getName());
            body = body.replace("#RESET_LINK#", resetLink);

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
            payload.put("subject", "Recuperacion de contrasena");
            payload.put("htmlContent", body);

            LOGGER.info("Enviando correo de recuperacion de contrasena a {} para usuario {}", forcedRecipientEmail, user.getEmail());

            Manager.getInstance().getEmailService().sendEmail(
                forcedRecipientEmail,
                "endpoint", endPoint,
                "headers", headers,
                "payload", payload
            );
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se pudo enviar email de recuperacion: " + e.getMessage());
        }
    }

    private String generateSessionToken() {
        return UUID.randomUUID().toString();
    }
}