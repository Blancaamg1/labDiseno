package edu.esi.dls.esiusuarios.http;

import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.http.HttpSession;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import edu.esi.dls.esiusuarios.dto.UserInfoDto;
import edu.esi.dls.esiusuarios.services.UserService;

@RestController /* Usado para que los métodos devuelvan datos directamente al frontend */
@CrossOrigin(origins = "http://localhost:4200") /* Permite que angular pueda llamar a este frontend */
@RequestMapping("/users") /* URL base para todos los métodos del controllador */
public class UserController {

    @Autowired
    private UserService service;

    @PostMapping("/login") /* Recibe del frontend un JSON con el usuario y contraseña */
    public HashMap<String, Object> login(HttpSession session, @RequestBody Map<String, String> credentials) {
        JSONObject jsonCredentials = new JSONObject(credentials); /* Las convierte en un objeto JSON y las extrae */
        String name = jsonCredentials.optString("name").trim();
        String password = jsonCredentials.optString("pwd");

        /* Llama al servicio */
        String userId = this.service.login(name, password);
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        /* Si el login es correcto, se guarda el userId dentro de la sesión HTTP */
        session.setAttribute("userId", userId);

        HashMap<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        result.put("name", name);
        result.put("httpSessionId", session.getId());
        return result;

    }

    /* Comprueba qué usuario tiene sesión iniciada */
    @GetMapping("/session")
    public HashMap<String, Object> getSession(HttpSession session) {
        String userToken = this.service.getSessionToken(session);
        String name = this.service.getValidatedSessionUserName(session);

        HashMap<String, Object> result = new HashMap<>();
        result.put("userId", userToken);
        result.put("name", name);
        result.put("httpSessionId", session.getId());
        return result;

    }

    /* Obtiene la información completa de la sesión */
    @GetMapping("/sessionInfo")
    public UserInfoDto getSessionInfo(HttpSession session) {
        return this.service.getValidatedSessionUserInfo(session);
    }

    /* Cierra la sesión actual del usuario */
    @PostMapping("/logout")
    public HashMap<String, Object> logout(HttpSession session) {
        session.invalidate();

        HashMap<String, Object> result = new HashMap<>();
        result.put("message", "Logout successful");
        return result;

    }

    /*
     * Devuelve el nombre del usuario que tiene la sesión iniciada. Si no hay sesión
     * válida, se lanza un error
     */
    @GetMapping("/me")
    public String currentUser(HttpSession session) {
        return this.service.getValidatedSessionUserName(session);
    }

    /* Recibe los datos del formulario de registro */
    @PostMapping("/register")
    public String register(@RequestBody Map<String, String> credentials) {
        JSONObject jsonCredentials = new JSONObject(credentials);
        String username = jsonCredentials.optString("username").trim();
        String email = jsonCredentials.optString("email").trim().toLowerCase();
        String pwd1 = jsonCredentials.optString("pwd1");
        String pwd2 = jsonCredentials.optString("pwd2");

        /* Comprueba que ningun campo esté vacío */
        if (username.isEmpty() || email.isEmpty() || pwd1.isEmpty() || pwd2.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid credentials");
        }

        /* Llama al servicio con los datos extraidos */
        String result = this.service.register(username, email, pwd1, pwd2);
        if (result == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No se pudo completar el registro. Los datos proporcionados no son validos.");
        }

        return result;

    }

    /* Confirma una cuenta a partir de un token */
    @GetMapping("/confirm/{tokenId}")
    public String confirm(@PathVariable String tokenId) { /*
                                                           * El token se recoge de la URL y se comprueba que no está
                                                           * vacío
                                                           */
        if (tokenId == null || tokenId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token de confirmación requerido");
        }

        /*
         * Se llama al servicio para confirmar la cuenta y se controlan dos posibles
         * errores: que noe xista el token o que la cuenta ya esta confirmada o el token
         * ya fue usado
         */
        try {
            return this.service.confirm(tokenId.trim());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage());
        }
    }

    /* Petición para solicitar la recuperación de la contraseña */
    @PostMapping("/password-reset/request")
    public String requestPasswordReset(@RequestBody Map<String, String> request) {
        String email = request.get("email"); /* Se recibe un email y se llama al servicio */
        return this.service.requestPasswordReset(email);
    }

    /*
     * Se recibe la confirmación de la recuperación de la contraseña con el token y
     * las contraseñas
     */
    @PostMapping("/password-reset/confirm")
    public String confirmPasswordReset(@RequestBody Map<String, String> request) {
        String token = request.get("token");
        String pwd1 = request.get("pwd1");
        String pwd2 = request.get("pwd2");

        /*
         * El token identifica la solicitud de recuperación de la contraseña, las
         * contraseñas se reciben para cambiarlas
         */
        if (token == null || token.isBlank() || pwd1 == null || pwd2 == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Datos de recuperacion invalidos");
        }

        /* Se llama al servicio, valida el token y cambia la contraseña */
        return this.service.resetPassword(token, pwd1, pwd2);
    }

    /* Se recibe una petición para cancelar la cuenta */
    @PostMapping("/cancelAccount")
    public HashMap<String, String> cancelAccount(@RequestBody Map<String, String> payload) {
        String token = payload.get("token");
        this.service.deleteAccount(token);
        HashMap<String, String> result = new HashMap<>();
        result.put("message", "Cuenta cancelada exitosamente");
        return result;
    }

}
