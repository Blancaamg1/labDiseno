package edu.esi.ds.esientradas.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.esi.ds.esientradas.dao.PDFDao;
import edu.esi.ds.esientradas.model.PDFEntrada;
import edu.esi.ds.esientradas.model.Pago;

@Service
public class PDFService {

    @Autowired
    private PDFDao pdfDao;

    public byte[] generarEntradaPDF(Pago pago) {

        String contenido = "ENTRADA\n"
                + "Pago ID: " + pago.getId() + "\n"
                + "Espectaculo: " + pago.getIdEspectaculo() + "\n"
                + "Entradas: " + pago.getCantidadEntradas();

        byte[] pdf = contenido.getBytes();

        PDFEntrada entrada = new PDFEntrada();
        entrada.setIdPago(pago.getId());
        entrada.setPdf(pdf);

        pdfDao.save(entrada);

        return pdf;
    }

}