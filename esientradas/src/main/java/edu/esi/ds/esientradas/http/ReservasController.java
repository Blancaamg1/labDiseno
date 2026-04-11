package edu.esi.ds.esientradas.http;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.esi.ds.esientradas.dto.DtoEntradaMapa;
import edu.esi.ds.esientradas.dto.DtoCompraInfo;
import edu.esi.ds.esientradas.services.ReservasService;
import jakarta.servlet.http.HttpSession;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/reservas")
public class ReservasController {

    @Autowired
    private ReservasService service;

    @PutMapping("/reservar")
    public Long reservar(HttpSession session, @RequestParam Long idEntrada) {
        Long precioEntrada = this.service.reservar(idEntrada, session.getId());
        Long precioTotal = (Long) session.getAttribute("precioTotal");

        if (precioTotal == null) {
            precioTotal = precioEntrada;
            session.setAttribute("precioTotal", precioTotal);

        } else {
            precioTotal += precioEntrada;
            session.setAttribute("precioTotal", precioTotal);
        }

        return precioTotal;
    }

    @GetMapping("/infoCompra")
    public DtoCompraInfo obtenerInfoCompra(@RequestParam Long idEspectaculo) {
        return this.service.obtenerInfoCompra(idEspectaculo);
    }

    @GetMapping("/entradasMapa")
    public List<DtoEntradaMapa> obtenerEntradasMapa(@RequestParam Long idEspectaculo) {
        return this.service.obtenerEntradasMapa(idEspectaculo);
    }
}
