package edu.esi.ds.esientradas.http;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import edu.esi.ds.esientradas.services.PagosService;



@RestController
@CrossOrigin(origins = "http://localhost:4200")
@RequestMapping("/pagos")
public class PagosController {

    @Autowired
    private PagosService service;

    @PostMapping("/prepararPago")
    public String prepararPago(@RequestBody Map<String,Object> infoPago){
        Long centimos = ((Number) infoPago.get("centimos")).longValue();
        try{
           return this.service.prepararPago(centimos);
        }catch(Exception e){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Error al preparar el pago:" + e.getMessage());
        }
    }
    
    @PostMapping("/confirmarPago")
    public void confirmarPago(){

    }


    

}