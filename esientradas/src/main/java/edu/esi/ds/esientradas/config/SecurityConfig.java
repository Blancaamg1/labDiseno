package edu.esi.ds.esientradas.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import edu.esi.ds.esientradas.services.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           UsuarioService usuarioService) throws Exception {
        http
            .cors(cors -> {})
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                .ignoringRequestMatchers(this::shouldIgnoreCsrf)
            )
            .addFilterBefore(new TokenAuthenticationFilter(usuarioService),
                    UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth

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
                    .requestMatchers(HttpMethod.POST, "/cola/entrar").permitAll()
                    .requestMatchers(HttpMethod.GET, "/cola/estado").permitAll()
                    .requestMatchers(HttpMethod.POST, "/cola/salir").permitAll()

                    // --- ReservasController ---
                    .requestMatchers(HttpMethod.PUT, "/reservas/reservar").permitAll()
                    .requestMatchers(HttpMethod.PUT, "/reservas/liberar").permitAll()
                    .requestMatchers(HttpMethod.GET, "/reservas/infoCompra").permitAll()
                    .requestMatchers(HttpMethod.GET, "/reservas/entradasMapa").permitAll()

                    // --- ComprasController ---
                    .requestMatchers(HttpMethod.POST, "/compras/confirmar").hasAnyRole("USER", "ADMIN")

                    // --- PagosController ---
                    .requestMatchers(HttpMethod.POST, "/pagos/prepararPago").hasAnyRole("USER", "ADMIN")
                    .requestMatchers(HttpMethod.GET, "/pagos/*/pdf").hasAnyRole("USER", "ADMIN")

                    // --- EscenarioController ---
                    .requestMatchers(HttpMethod.POST, "/escenarios/insertar").hasRole("ADMIN")

                    .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                    .authenticationEntryPoint((request, response, authException) -> {
                        response.sendError(401, "No autorizado");
                    }));

        return http.build();
    }

    private boolean shouldIgnoreCsrf(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();

        if (HttpMethod.OPTIONS.name().equals(method)) return true;
        if (HttpMethod.GET.name().equals(method)) return true;
        if (HttpMethod.POST.name().equals(method) &&
            (path.equals("/cola/entrar") || path.equals("/cola/salir"))) return true;

        return false;
    }
}