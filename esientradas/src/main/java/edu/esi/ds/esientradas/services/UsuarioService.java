package edu.esi.ds.esientradas.services;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import edu.esi.ds.esientradas.dto.DtoUsuarioInfo;

@Service
public class UsuarioService {

    public DtoUsuarioInfo getUserInfo(String userToken) {
        String endpoint = "http://localhost:8081/external/checkToken";
        RestTemplate rest = new RestTemplate();

        try {
            DtoUsuarioInfo userInfo = rest.getForObject(endpoint + "/" + userToken, DtoUsuarioInfo.class);
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
