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

@RestController
@CrossOrigin(origins = "http://localhost:4200")
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserService service;


    @PostMapping("/login")
    public HashMap<String, Object> login(HttpSession session, @RequestBody Map<String, String> credentials){
        JSONObject jsonCredentials = new JSONObject(credentials);
        String name = jsonCredentials.optString("name").trim();
        String password = jsonCredentials.optString("pwd");

        String userId = this.service.login(name, password);
        if(userId == null){
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        session.setAttribute("userId", userId);

        HashMap<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        result.put("name", name);
        result.put("httpSessionId", session.getId());
        return result;

    }

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

    @GetMapping("/sessionInfo")
    public UserInfoDto getSessionInfo(HttpSession session) {
        return this.service.getValidatedSessionUserInfo(session);
    }

    @PostMapping("/logout")
    public HashMap<String, Object> logout(HttpSession session) {
        session.invalidate();

        HashMap<String, Object> result = new HashMap<>();
        result.put("message", "Logout successful");
        return result;

    }

    @GetMapping("/me")
    public String currentUser(HttpSession session){
        return this.service.getValidatedSessionUserName(session);
    }

    @PostMapping("/register")
    public String register(@RequestBody Map<String, String> credentials){
        JSONObject jsonCredentials = new JSONObject(credentials);
        String username = jsonCredentials.optString("username").trim();
        String email = jsonCredentials.optString("email").trim().toLowerCase();
        String pwd1 = jsonCredentials.optString("pwd1");
        String pwd2 = jsonCredentials.optString("pwd2");

        if( username.isEmpty() || email.isEmpty() || pwd1.isEmpty() || pwd2.isEmpty()){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid credentials");
        }

        String result = this.service.register(username, email, pwd1, pwd2);
        if(result == null){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se pudo completar el registro. Los datos proporcionados no son validos.");
        }

        return result;

    }

    @GetMapping("/confirm/{tokenId}")
    public String confirm(@PathVariable String tokenId) {
        if (tokenId == null || tokenId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token de confirmación requerido");
        }

        try {
            return this.service.confirm(tokenId.trim());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage());
        }
    }

    @PostMapping("/password-reset/request")
    public String requestPasswordReset(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        return this.service.requestPasswordReset(email);
    }

    @PostMapping("/password-reset/confirm")
    public String confirmPasswordReset(@RequestBody Map<String, String> request) {
        String token = request.get("token");
        String pwd1 = request.get("pwd1");
        String pwd2 = request.get("pwd2");

        if (token == null || token.isBlank() || pwd1 == null || pwd2 == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Datos de recuperacion invalidos");
        }

        return this.service.resetPassword(token, pwd1, pwd2);
    }


}
