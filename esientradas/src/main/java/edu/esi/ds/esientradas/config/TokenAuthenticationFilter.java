package edu.esi.ds.esientradas.config;

import java.io.IOException;
import java.util.Collections;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import edu.esi.ds.esientradas.dto.DtoUsuarioInfo;
import edu.esi.ds.esientradas.services.UsuarioService;

public class TokenAuthenticationFilter extends OncePerRequestFilter {

    private final UsuarioService usuarioService;

    public TokenAuthenticationFilter(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 1. Obtener el token de la cabecera (Angular debe enviarlo como "Authorization: Bearer <token>" o similar)
        String authHeader = request.getHeader("Authorization");
        String token = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        } else if (authHeader != null) {
            token = authHeader; // Por si mandan el token a pelo
        }

        if (token != null && !token.isBlank()) {
            try {
                // 2. Preguntar a esiusuarios si el token es válido
                DtoUsuarioInfo userInfo = usuarioService.getUserInfo(token);

                if (userInfo != null) {
                    // 3. Obtener el rol que nos devuelve esiusuarios (por defecto USER si no viene)
                    String role = userInfo.getRole() != null ? userInfo.getRole() : "USER";

                    // 4. Inyectarlo en Spring Security
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            userInfo.getName(), 
                            null, 
                            Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role)));
                    
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (Exception ex) {
                // Token inválido o esiusuarios caído, ignoramos y Spring Security devolverá 401 si la ruta está protegida
                SecurityContextHolder.clearContext();
            }
        }

        // Continuar con la cadena de filtros
        filterChain.doFilter(request, response);
    }
}
