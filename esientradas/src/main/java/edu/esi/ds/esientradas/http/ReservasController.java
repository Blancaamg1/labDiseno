package edu.esi.ds.esientradas.http;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.esi.ds.esientradas.dto.DtoReserva;
import edu.esi.ds.esientradas.dto.DtoCompraInfo;
import edu.esi.ds.esientradas.dto.DtoEntradaMapa;
import edu.esi.ds.esientradas.services.ReservasService;
import jakarta.servlet.http.HttpSession;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/reservas")
public class ReservasController {

    @Autowired
    private ReservasService service;

    @PutMapping("/reservar")
    public DtoReserva reservar(HttpSession session,
                               @RequestParam Long idEntrada,
                               @RequestParam(required = false) String tokenReserva,
                               @RequestParam(required = false) String tokenTurno) {
        
        // Si nos envían un tokenReserva, lo usamos. Si no, usamos el de la sesión.
        String idParaReserva = (tokenReserva != null && !tokenReserva.isBlank()) ? tokenReserva : session.getId();
        
        // Llamamos al servicio.
        Long precioEntrada = this.service.reservar(idEntrada, idParaReserva, tokenTurno);
        
        // Manejo seguro del precio en sesión
        Object precioTotalObj = session.getAttribute("precioTotal");
        long precioTotal = (precioTotalObj instanceof Number) ? ((Number) precioTotalObj).longValue() : 0L;

        precioTotal += precioEntrada;
        session.setAttribute("precioTotal", precioTotal);

        return new DtoReserva(idParaReserva, precioTotal);
    }

    @PutMapping("/liberar")
    public void liberar(@RequestParam Long idEntrada, @RequestParam(required = false) String tokenTurno) {
        this.service.liberar(idEntrada, tokenTurno);
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