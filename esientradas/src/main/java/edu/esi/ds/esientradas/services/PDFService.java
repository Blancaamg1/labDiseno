package edu.esi.ds.esientradas.services;

import java.io.ByteArrayOutputStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;

import edu.esi.ds.esientradas.dao.PDFDao;
import edu.esi.ds.esientradas.model.PDFEntrada;
import edu.esi.ds.esientradas.model.Pago;

@Service
public class PDFService {

    @Autowired
    private PDFDao pdfDao;

    public byte[] generarEntradaPDF(Pago pago) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            Document document = new Document();
            PdfWriter.getInstance(document, baos);

            document.open();
            document.add(new Paragraph("ENTRADA"));
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Pago ID: " + pago.getId()));
            document.add(new Paragraph("Espectáculo: " + pago.getIdEspectaculo()));
            document.add(new Paragraph("Cantidad de entradas: " + pago.getCantidadEntradas()));
            document.add(new Paragraph("Comprador: " + pago.getEmailComprador()));
            document.add(new Paragraph("Importe total: " + (pago.getImporteTotalCentimos() / 100.0) + " €"));
            document.close();

            byte[] pdf = baos.toByteArray();

            PDFEntrada entrada = new PDFEntrada();
            entrada.setIdPago(pago.getId());
            entrada.setPdf(pdf);

            pdfDao.save(entrada);

            return pdf;
        } catch (Exception e) {
            throw new RuntimeException("Error al generar el PDF de la entrada", e);
        }
    }
}