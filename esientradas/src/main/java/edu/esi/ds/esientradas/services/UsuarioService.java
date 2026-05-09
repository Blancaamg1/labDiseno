package edu.esi.ds.esientradas.services;

import java.net.URI;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import edu.esi.ds.esientradas.dto.DtoUsuarioInfo;

@Service
public class UsuarioService {

    private static final Pattern TOKEN_PATTERN =
        Pattern.compile("^[A-Za-z0-9\\-_\\.]{10,512}$");

    private boolean isValidToken(String token) {
        return token != null && TOKEN_PATTERN.matcher(token).matches();
    }

    public DtoUsuarioInfo getUserInfo(String userToken) {
        if (!isValidToken(userToken)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token inválido");
        }
        String endpoint = "http://localhost:8081/external/checkToken/{token}";
        RestTemplate rest = new RestTemplate();
        URI uri = UriComponentsBuilder.fromUriString(endpoint)
                .buildAndExpand(userToken)
                .encode()
                .toUri();
        try {
            DtoUsuarioInfo userInfo = rest.getForObject(uri, DtoUsuarioInfo.class);
            if (userInfo == null || userInfo.getId() == null || userInfo.getName() == null || userInfo.getName().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Se necesita el token");
            }
            return userInfo;
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No se pudo validar el token", ex);
        }
    }

    public String checkToken(String userToken) {
        return getUserInfo(userToken).getName();
    }
}