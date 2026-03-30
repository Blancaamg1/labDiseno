package edu.esi.ds.esientradas.services;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UsuarioService {

    public String checkToken(String userToken) {
        String endpoint = "http://localhost:8081/external/checkToken";
        RestTemplate rest = new RestTemplate();

        try{
            String userName = rest.getForObject(endpoint + "/" + userToken, String.class);
            if(userName == null || userName.isEmpty()){
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Se necesita el token");
            }
            return userName;
        }catch(RestClientException ex){
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No se pudo validar el token", ex);
        }
    }

}
