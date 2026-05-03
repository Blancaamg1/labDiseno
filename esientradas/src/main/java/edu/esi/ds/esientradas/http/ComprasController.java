package edu.esi.ds.esientradas.http;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import edu.esi.ds.esientradas.dto.DtoConfirmarPagoRequest;
import edu.esi.ds.esientradas.dto.DtoConfirmarPagoResponse;
import edu.esi.ds.esientradas.services.ComprasService;

@RestController
@CrossOrigin(
    origins = "http://localhost:4200",
    methods = { RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS },
    allowedHeaders = "*"
)
@RequestMapping("/compras")
public class ComprasController {

    @Autowired
    private ComprasService comprasService;
    
    @PostMapping("/confirmar")
    public DtoConfirmarPagoResponse confirmarCompra(@RequestBody DtoConfirmarPagoRequest request) {
        return this.comprasService.procesarCompra(request);
    }

}
