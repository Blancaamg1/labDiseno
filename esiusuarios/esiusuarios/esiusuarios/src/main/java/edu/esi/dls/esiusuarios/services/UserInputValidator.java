package edu.esi.dls.esiusuarios.services;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class UserInputValidator {

    public void validateLoginData(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
    }

    public void validateRegistrationData(String username, String email, String pwd1, String pwd2) {
        if (!isValidUsername(username)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Los datos proporcionados no son validos.");
        }

        if (!isValidEmail(email)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Los datos proporcionados no son validos.");
        }

        if (!pwd1.equals(pwd2)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Las contrasenias no coinciden.");
        }

        if (!isStrongPassword(pwd1)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Los datos proporcionados no son validos.");
        }
    }

    public void validatePasswordResetData(String pwd1, String pwd2) {
        if (!pwd1.equals(pwd2)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Las contrasenias no coinciden");
        }

        if (!isStrongPassword(pwd1)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "La contrasena no cumple los requisitos de seguridad");
        }
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
        if (password == null || password.length() < 8) {
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