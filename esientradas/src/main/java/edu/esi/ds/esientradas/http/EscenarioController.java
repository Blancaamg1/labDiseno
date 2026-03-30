package edu.esi.ds.esientradas.http;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import edu.esi.ds.esientradas.model.Escenario;
import edu.esi.ds.esientradas.services.EscenarioService;
@RestController 
@RequestMapping("/escenarios") 
public class EscenarioController {
    
    @Autowired
    private EscenarioService service;

    @PostMapping("/insertar")
    public void insertarEscenario(@RequestBody Escenario escenario){
        if(escenario.getNombre() == null || escenario.getNombre().isEmpty()){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre del escenario no puede ser null o vacio");
        }

        if(escenario.getDescripcion() == null || escenario.getDescripcion().isEmpty()){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"La descripción del escenario no puede ser nulo o vacio");
        }

        this.service.insertar(escenario);
    }

}
