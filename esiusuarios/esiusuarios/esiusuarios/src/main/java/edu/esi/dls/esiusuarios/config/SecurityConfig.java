package edu.esi.dls.esiusuarios.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> {
                }) // Activa la integración con @CrossOrigin
                .csrf(csrf -> csrf.disable()) // Desactiva CSRF para permitir POSTs desde Angular
                .authorizeHttpRequests(auth -> auth
                        // Permitir peticiones OPTIONS (CORS Preflight)
                        .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()

                        // Rutas de administración y usuarios (Ejemplos)
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/user/**").hasAnyRole("USER", "ADMIN")

                        // UserController (Exactamente como en la tabla)
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/users/login").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/users/register").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/users/confirm/*").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/users/cancelAccount").permitAll()

                        // Rutas públicas de validación de tokens
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/external/checkToken/**").permitAll()

                        // Otras rutas que deben ser públicas para que funcione la recuperación de
                        // contraseña
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/users/password-reset/**")
                        .permitAll()

                        // Cualquier otra petición, requiere autenticación
                        .anyRequest().authenticated())
                // Si el usuario no está autenticado, devuelve 401 en lugar de redirigir a un
                // HTML
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.sendError(401, "No autorizado");
                        }));
        return http.build();
    }
}
