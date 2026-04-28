package edu.esi.ds.esientradas.services;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Image;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;

import edu.esi.ds.esientradas.dao.EspectaculoDao;
import edu.esi.ds.esientradas.dao.PDFDao;
import edu.esi.ds.esientradas.model.Espectaculo;
import edu.esi.ds.esientradas.model.PDFEntrada;
import edu.esi.ds.esientradas.model.Pago;

@Service
public class PDFService {

    private static final DateTimeFormatter FECHA_EVENTO_FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Autowired
    private PDFDao pdfDao;

    @Autowired
    private EspectaculoDao espectaculoDao;

    @Transactional
    public byte[] generarEntradaPDF(Pago pago) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            Document document = new Document();
            PdfWriter.getInstance(document, baos);

            document.open();
            Espectaculo espectaculo = this.espectaculoDao.findById(pago.getIdEspectaculo()).orElse(null);

            this.agregarLogo(document);
            this.agregarCabecera(document);
            this.agregarDetallePago(document, pago);
            this.agregarDetalleEvento(document, espectaculo);
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

    private void agregarLogo(Document document) throws Exception {
        Image logo = this.cargarLogoEmpresa();
        if (logo == null) {
            return;
        }

        logo.scaleToFit(140f, 60f);
        logo.setAlignment(Element.ALIGN_RIGHT);
        document.add(logo);
    }

    private void agregarCabecera(Document document) throws Exception {
        document.add(new Paragraph("ENTRADA"));
        document.add(new Paragraph(" "));
    }

    private void agregarDetallePago(Document document, Pago pago) throws Exception {
        document.add(new Paragraph("Pago ID: " + pago.getId()));
        document.add(new Paragraph("Cantidad de entradas: " + pago.getCantidadEntradas()));
        document.add(new Paragraph("Comprador: " + pago.getEmailComprador()));
        document.add(new Paragraph("Importe total: " + (pago.getImporteTotalCentimos() / 100.0) + " €"));
    }

    private void agregarDetalleEvento(Document document, Espectaculo espectaculo) throws Exception {
        String nombreEvento = "No disponible";
        String fechaEvento = "No disponible";
        String lugarEvento = "No disponible";

        if (espectaculo != null) {
            if (espectaculo.getArtista() != null && !espectaculo.getArtista().isBlank()) {
                nombreEvento = espectaculo.getArtista();
            }

            if (espectaculo.getFecha() != null) {
                fechaEvento = espectaculo.getFecha().format(FECHA_EVENTO_FORMATO);
            }

            if (espectaculo.getEscenario() != null
                    && espectaculo.getEscenario().getNombre() != null
                    && !espectaculo.getEscenario().getNombre().isBlank()) {
                lugarEvento = espectaculo.getEscenario().getNombre();
            }
        }

        document.add(new Paragraph("Evento: " + nombreEvento));
        document.add(new Paragraph("Fecha del evento: " + fechaEvento));
        document.add(new Paragraph("Lugar: " + lugarEvento));
    }

    private Image cargarLogoEmpresa() throws IOException, com.lowagie.text.BadElementException {
        URL logoUrl = this.getClass().getResource("/logo.png");
        if (logoUrl == null) {
            return null;
        }

        return Image.getInstance(logoUrl);
    }
}