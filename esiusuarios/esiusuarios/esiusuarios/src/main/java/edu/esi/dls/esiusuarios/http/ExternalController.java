package edu.esi.dls.esiusuarios.http;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import edu.esi.dls.esiusuarios.dto.UserInfoDto;
import edu.esi.dls.esiusuarios.services.UserService;


@RestController
@CrossOrigin(origins = "http://localhost:4200")
@RequestMapping("/external")
public class ExternalController {
    
    @Autowired
    private UserService service;

    @GetMapping("/checkToken/{token}")
    public UserInfoDto checkToken(@PathVariable String token) {
        if(token == null || token.isEmpty()){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Se necesita el token"); 
        }

        UserInfoDto userInfo = this.service.getUserInfo(token);
        if(userInfo == null){
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token no válido");
        }
        return userInfo;
    }

}