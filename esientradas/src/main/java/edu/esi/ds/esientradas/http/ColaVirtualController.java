package edu.esi.ds.esientradas.http;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import edu.esi.ds.esientradas.dto.DtoColaEstado;
import edu.esi.ds.esientradas.services.ColaVirtualService;

@RestController
@CrossOrigin(origins = "http://localhost:4200")
@RequestMapping("/cola")
public class ColaVirtualController {

    @Autowired
    private ColaVirtualService colaVirtualService;

    @PostMapping("/entrar")
    public DtoColaEstado entrarEnCola(@RequestParam Long idEspectaculo,
                                      @RequestParam String userToken) {
        return this.colaVirtualService.entrarEnCola(idEspectaculo, userToken);
    }

    @GetMapping("/estado")
    public DtoColaEstado consultarEstado(@RequestParam Long idEspectaculo,
                                         @RequestParam String userToken) {
        return this.colaVirtualService.consultarEstado(idEspectaculo, userToken);
    }

    @PostMapping("/salir")
    public void salirDeCola(@RequestParam Long idEspectaculo,
                            @RequestParam String userToken) {
        this.colaVirtualService.salirDeCola(idEspectaculo, userToken);
    }
}