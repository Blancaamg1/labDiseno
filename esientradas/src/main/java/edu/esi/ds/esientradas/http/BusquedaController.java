package edu.esi.ds.esientradas.http;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.esi.ds.esientradas.dto.DtoEntradas;
import edu.esi.ds.esientradas.dto.DtoEspectaculo;
import edu.esi.ds.esientradas.model.Entrada;
import edu.esi.ds.esientradas.model.Escenario;
import edu.esi.ds.esientradas.model.Espectaculo;
import edu.esi.ds.esientradas.services.BusquedaService;
import jakarta.websocket.server.PathParam;
@RestController
@RequestMapping("/busqueda") // Las peticiones del servicio saludar, se envian a la url http://localhost:8080/busqueda/saludar
// Un parámetro en la url se pone con un ? por ejemplo ?nombre=
@CrossOrigin(origins = "*") // Permite que cualquier cliente pueda hacer peticiones a este servicio, sin importar su origen
public class BusquedaController {

    @Autowired // cuando revisa el código de esta clase al arrancar busca el busquedaservice, si no está creado, lo crea
    private BusquedaService service;

    @GetMapping("/getEntradas/{idEspectaculo}") 
    public List<Entrada> getEntradas(@PathVariable Long idEspectaculo){
        return this.service.getEntradas(idEspectaculo);
    }
    
     @GetMapping("/getNumeroDeEntradas") 
    public Integer getNumeroDeEntradas(@RequestParam Long idEspectaculo){
        return this.service.getNumeroDeEntradas(idEspectaculo);
    }

     @GetMapping("/getEntradasLibres") 
    public Integer getEntradasLibres(@RequestParam Long idEspectaculo){
        return this.service.getEntradasLibres(idEspectaculo);
    }

     @GetMapping("/getNumeroDeEntradasComoDto") 
    public DtoEntradas getNumeroDeEntradasComoDto(@RequestParam Long idEspectaculo){
        DtoEntradas dto = this.service.getNumeroDeEntradasComoDto(idEspectaculo);
        return dto;
    }
    
    @GetMapping("/getEscenarios")
    public List<Escenario> getEscenarios(){
        return this.service.getEscenarios();
    }    

    @GetMapping("/getEspectaculos")
    public List<DtoEspectaculo> getEspectaculos(@RequestParam String artista){
        List<Espectaculo> espectaculos =  this.service.getEspectaculos(artista);

        List<DtoEspectaculo> dtos = espectaculos.stream().map(e -> {
            DtoEspectaculo dto = new DtoEspectaculo();
            dto.setId(e.getId());
            dto.setArtista(e.getArtista());
            dto.setFecha(e.getFecha());
            dto.setEscenario(e.getEscenario().getNombre());
            return dto;
        }).toList();

        return dtos;
    }  

    @GetMapping("/getEspectaculos/{idEscenario}")
    public List<DtoEspectaculo> getEspectaculos(@PathVariable Long idEscenario){
        List<Espectaculo> espectaculos =  this.service.getEspectaculos(idEscenario);

        List<DtoEspectaculo> dtos = espectaculos.stream().map(e -> {
            DtoEspectaculo dto = new DtoEspectaculo();
            dto.setId(e.getId());
            dto.setArtista(e.getArtista());
            dto.setFecha(e.getFecha());
            dto.setEscenario(e.getEscenario().getNombre());
            return dto;
        }).toList();

        return dtos;
    }  

    
    @GetMapping("/saludar/{nombre}")
    public String saludar(@PathVariable String nombre, @RequestParam String apellido){
        return "Hola " + nombre + " " + apellido + " ,esta es la busqueda de entradas";
    }
}
