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

    private static final String UUID_REGEX =
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";

    public TokenAuthenticationFilter(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        if (token.isBlank() || !isValidTokenFormat(token)) {
            SecurityContextHolder.clearContext();
            filterChain.doFilter(request, response);
            return;
        }

        try {
            DtoUsuarioInfo userInfo = usuarioService.getUserInfo(token);

            if (userInfo != null) {
                String role = userInfo.getRole() != null ? userInfo.getRole() : "USER";

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userInfo.getName(),
                        null,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role)));

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }

        } catch (Exception ex) {
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    private boolean isValidTokenFormat(String token) {
        return token.matches(UUID_REGEX);
    }
}