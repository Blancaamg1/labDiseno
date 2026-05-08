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
    public DtoColaEstado entrarEnCola(@RequestParam Long idEspectaculo) {
        return this.colaVirtualService.entrarEnCola(idEspectaculo);
    }

    @GetMapping("/estado")
    public DtoColaEstado consultarEstado(@RequestParam Long idEspectaculo,
                                         @RequestParam String tokenTurno) {
        return this.colaVirtualService.consultarEstado(idEspectaculo, tokenTurno);
    }

    @PostMapping("/salir")
    public void salirDeCola(@RequestParam Long idEspectaculo,
                            @RequestParam String tokenTurno) {
        this.colaVirtualService.salirDeCola(idEspectaculo, tokenTurno);
    }
}