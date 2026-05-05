package edu.esi.ds.esientradas.http;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import edu.esi.ds.esientradas.dao.PDFDao;
import edu.esi.ds.esientradas.model.PDFEntrada;
import edu.esi.ds.esientradas.services.PagosService;

@RestController
@CrossOrigin(
    origins = "http://localhost:4200",
    methods = { RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS },
    allowedHeaders = "*"
)
@RequestMapping("/pagos")
public class PagosController {

    @Autowired
    private PagosService service;

    @Autowired
    private PDFDao pdfDao;

    @PostMapping("/prepararPago")
    public String prepararPago(@RequestBody Map<String, Object> infoPago) {
        Long centimos = ((Number) infoPago.get("centimos")).longValue();
        try {
            return this.service.prepararPago(centimos);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Error al preparar el pago:" + e.getMessage());
        }
    }


    @GetMapping("/{idPago}/pdf")
    public ResponseEntity<byte[]> descargarPdf(@PathVariable Long idPago) {
        PDFEntrada pdfEntrada = pdfDao.findByPago_Id(idPago)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "PDF no encontrado"));

        return ResponseEntity.ok()
            .header("Content-Type", "application/pdf")
            .header("Content-Disposition", "attachment; filename=entrada_" + idPago + ".pdf")
            .body(pdfEntrada.getPdf());
    }
}