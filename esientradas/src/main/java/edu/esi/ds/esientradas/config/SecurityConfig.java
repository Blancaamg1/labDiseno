package edu.esi.ds.esientradas.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import edu.esi.ds.esientradas.services.UsuarioService;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, UsuarioService usuarioService) throws Exception {
        http
            .cors(cors -> {}) // Activa la integración con @CrossOrigin
            .csrf(csrf -> csrf.disable()) // Desactiva CSRF para permitir llamadas desde Angular
            .addFilterBefore(new TokenAuthenticationFilter(usuarioService), UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth
                // Permitir peticiones OPTIONS (CORS Preflight)
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                
                // --- BusquedaController ---
                .requestMatchers(HttpMethod.GET, "/busqueda/getEscenarios").permitAll()
                .requestMatchers(HttpMethod.GET, "/busqueda/getEspectaculos").permitAll()
                .requestMatchers(HttpMethod.GET, "/busqueda/getEspectaculos/*").permitAll()
                .requestMatchers(HttpMethod.GET, "/busqueda/getNumeroDeEntradas").permitAll()
                .requestMatchers(HttpMethod.GET, "/busqueda/getEntradasLibres").permitAll()
                .requestMatchers(HttpMethod.GET, "/busqueda/getNumeroDeEntradasComoDto").permitAll()
                .requestMatchers(HttpMethod.GET, "/busqueda/getEntradas/*").hasAnyRole("USER", "ADMIN")
                
                // --- ColaVirtualController ---
                .requestMatchers(HttpMethod.POST, "/cola/entrar").hasAnyRole("USER", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/cola/estado").hasAnyRole("USER", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/cola/salir").hasAnyRole("USER", "ADMIN")
                
                // --- ReservasController ---
                .requestMatchers(HttpMethod.PUT, "/reservas/reservar").hasAnyRole("USER", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/reservas/liberar").hasAnyRole("USER", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/reservas/infoCompra").hasAnyRole("USER", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/reservas/entradasMapa").hasAnyRole("USER", "ADMIN")
                
                // --- ComprasController ---
                .requestMatchers(HttpMethod.POST, "/compras/confirmar").hasAnyRole("USER", "ADMIN")
                
                // --- PagosController ---
                .requestMatchers(HttpMethod.POST, "/pagos/prepararPago").hasAnyRole("USER", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/pagos/*/pdf").hasAnyRole("USER", "ADMIN")
                
                // --- EscenarioController ---
                .requestMatchers(HttpMethod.POST, "/escenarios/insertar").hasRole("ADMIN")
                
                // Cualquier otra ruta, protegida por defecto
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.sendError(401, "No autorizado");
                })
            );
            
        return http.build();
    }
}
