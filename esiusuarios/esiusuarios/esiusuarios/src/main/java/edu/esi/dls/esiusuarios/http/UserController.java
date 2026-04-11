package edu.esi.dls.esiusuarios.http;

import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.http.HttpSession;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
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

        if(name.isEmpty() || password.isEmpty()){
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
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
        Object userId = session.getAttribute("userId");
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No active session");
        }

        String name = this.service.checkToken(userId.toString());
        if (name == null) {
            session.invalidate();
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid session");
        }

        HashMap<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        result.put("name", name);
        result.put("httpSessionId", session.getId());
        return result;

    }

    @GetMapping("/sessionInfo")
    public UserInfoDto getSessionInfo(HttpSession session) {
        Object userId = session.getAttribute("userId");
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No active session");
        }

        UserInfoDto userInfo = this.service.getUserInfo(userId.toString());
        if (userInfo == null) {
            session.invalidate();
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid session");
        }

        return userInfo;
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
        Object userId = session.getAttribute("userId");
        if(userId == null){
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No active session");
        }

        String result = this.service.checkToken(userId.toString());
        if(result == null){
            session.invalidate();
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid session");
        }
        return result;
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

        if(!isValidUsername(username)){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Los datos proporcionados no son validos.");
        }

        if(!isValidEmail(email)){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Los datos proporcionados no son validos.");
        }

        if(!pwd1.equals(pwd2)){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Las contrasenias no coinciden.");
        }

        if(!isStrongPassword(pwd1)){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Los datos proporcionados no son validos.");
        }

        String result = this.service.register(username, email, pwd1);
        if(result == null){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se pudo completar el registro. Los datos proporcionados no son validos.");
        }

        return result;

    }

    private boolean isValidUsername(String username) {
        if (username.length() < 3 || username.length() > 20) {
            return false;
        }

        for (char c : username.toCharArray()) {
            if (Character.isLetterOrDigit(c) || c == '_' || c == '.') {
                continue;
            }
            return false;
        }

        return true;
    }

    private boolean isValidEmail(String email) {
        if (email.contains(" ")) {
            return false;
        }

        int atIndex = email.indexOf('@');
        int lastAtIndex = email.lastIndexOf('@');
        int dotIndex = email.lastIndexOf('.');

        if (atIndex <= 0 || atIndex != lastAtIndex) {
            return false;
        }

        if (dotIndex <= atIndex + 1 || dotIndex >= email.length() - 1) {
            return false;
        }

        return true;
    }

    private boolean isStrongPassword(String password) {
        if (password.length() < 8) {
            return false;
        }

        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasDigit = false;

        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) {
                hasUpper = true;
            } else if (Character.isLowerCase(c)) {
                hasLower = true;
            } else if (Character.isDigit(c)) {
                hasDigit = true;
            }
        }

        return hasUpper && hasLower && hasDigit;
    }


}
