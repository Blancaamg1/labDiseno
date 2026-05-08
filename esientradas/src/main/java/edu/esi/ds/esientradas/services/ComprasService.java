package edu.esi.ds.esientradas.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.esi.ds.esientradas.dto.DtoConfirmarPagoRequest;
import edu.esi.ds.esientradas.dto.DtoConfirmarPagoResponse;
import edu.esi.ds.esientradas.model.Pago;

@Service
public class ComprasService {

    @Autowired
    private PagosService pagosService;

    @Autowired
    private ReservasService reservasService;

    @Autowired
    private ColaVirtualService colaVirtualService;

    @Autowired
    private PDFService pdfService;

    @Transactional
    public DtoConfirmarPagoResponse procesarCompra(DtoConfirmarPagoRequest request) {
        // 1. Delegar al servicio de pagos la validación financiera y creación del registro de Pago
        DtoConfirmarPagoResponse response = pagosService.confirmarPago(request);

        // 2. Si el pago fue exitoso y es una confirmación nueva, actualizar el inventario
        if ("PAGADO".equalsIgnoreCase(response.getEstadoPago()) && !response.isYaConfirmado()) {
            reservasService.finalizarVenta(request.getIdsEntradas(), request.getIdEspectaculo());
            
            try {
                if (request.getIdEspectaculo() != null && request.getUserToken() != null) {
                    colaVirtualService.salirDeCola(request.getIdEspectaculo(), request.getUserToken());
                }
            } catch (Exception e) {
                // Ignorar si el usuario no estaba en la cola o el token es inválido
            }
        }

        // 3. Generar el PDF (Responsabilidad de reportes/documentación)
        if (response.getPagoId() != null) {
            try {
                Pago pago = pagosService.obtenerPago(response.getPagoId());
                if (pago != null) {
                    pdfService.generarEntradaPDF(pago);
                    response.setPdfGenerado(true);
                }
            } catch (Exception e) {
                response.setPdfGenerado(false);
            }
        }

        return response;
    }
}
