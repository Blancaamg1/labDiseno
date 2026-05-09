package edu.esi.dls.esiusuarios.http;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
public class UserSecuredController {

    @GetMapping("/profile")
    public Object userProfile() {
        // Extraemos el usuario que Spring Security sabe que está logueado
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        java.util.Map<String, Object> profile = new java.util.HashMap<>();
        profile.put("username", auth.getName());
        profile.put("authorities", auth.getAuthorities());
        profile.put("message", "Esta es la información extraída directamente del contexto de seguridad, validando que el filtro funciona.");
        
        return profile;
    }
}
