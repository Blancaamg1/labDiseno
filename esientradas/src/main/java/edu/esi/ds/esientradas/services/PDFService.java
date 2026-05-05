package edu.esi.ds.esientradas.services;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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
            System.out.println("Generando PDF para pago: " + pago.getId());

            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            Document document = new Document();
            PdfWriter.getInstance(document, baos);

            document.open();

            Espectaculo espectaculo = null;

            if (pago.getIdEspectaculo() != null) {
                espectaculo = this.espectaculoDao.findById(pago.getIdEspectaculo()).orElse(null);
            }

            this.agregarLogo(document);
            this.agregarCabecera(document);
            this.agregarDetallePago(document, pago);
            this.agregarDetalleEvento(document, espectaculo);

            document.close();

            byte[] pdf = baos.toByteArray();

            PDFEntrada entrada = new PDFEntrada();
            entrada.setIdPago(pago.getId());
            entrada.setPdf(pdf);

            this.pdfDao.save(entrada);

            System.out.println("PDF guardado en base de datos para pago: " + pago.getId());
            System.out.println("Intentando enviar PDF al correo: " + pago.getEmailComprador());

            try {
                this.enviarPdfPorBrevo(
                        "entradaseventosesi@gmail.com",
                        pago.getId(),
                        pdf);

                System.out.println("Correo enviado correctamente por Brevo");

            } catch (Exception e) {
                System.err.println("El PDF se genero, pero no se pudo enviar por correo");
                e.printStackTrace();
            }

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

    private void enviarPdfPorBrevo(String destinatario, Long idPago, byte[] pdf) {
        if (destinatario == null || destinatario.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "No hay email del comprador para enviar el PDF");
        }

        if (pdf == null || pdf.length == 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El PDF generado esta vacio");
        }

        JSONObject configuracion = this.cargarConfiguracionBrevo();

        String endpoint = configuracion.getString("endpoint");
        JSONArray headers = configuracion.getJSONArray("headers");
        JSONObject sender = configuracion.getJSONObject("sender");
        String subject = configuracion.getString("subject");

        JSONObject receptor = new JSONObject();
        receptor.put("email", destinatario);

        JSONArray destinatarios = new JSONArray();
        destinatarios.put(receptor);

        String pdfBase64 = Base64.getEncoder().encodeToString(pdf);

        JSONObject adjunto = new JSONObject();
        adjunto.put("content", pdfBase64);
        adjunto.put("name", "entrada_" + idPago + ".pdf");

        JSONArray adjuntos = new JSONArray();
        adjuntos.put(adjunto);

        JSONObject payload = new JSONObject();
        payload.put("sender", sender);
        payload.put("to", destinatarios);
        payload.put("subject", subject);
        payload.put("htmlContent", this.crearContenidoCorreo(idPago));
        payload.put("attachment", adjuntos);

        this.enviarPostABrevo(endpoint, headers, payload);
    }

    private JSONObject cargarConfiguracionBrevo() {
        try {
            InputStream inputStream = this.getClass().getResourceAsStream("/brevo.parameters.txt");

            if (inputStream == null) {
                throw new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "No se ha encontrado brevo.parameters.txt en resources");
            }

            String contenido = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

            return new JSONObject(contenido);

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error leyendo brevo.parameters.txt",
                    e);
        }
    }

    private void enviarPostABrevo(String endpoint, JSONArray headers, JSONObject payload) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .POST(HttpRequest.BodyPublishers.ofString(payload.toString()));

            for (int i = 0; i < headers.length(); i++) {
                String header = headers.getString(i);
                int posicionDosPuntos = header.indexOf(":");

                if (posicionDosPuntos != -1) {
                    String nombre = header.substring(0, posicionDosPuntos).trim();
                    String valor = header.substring(posicionDosPuntos + 1).trim();

                    if (valor != null && !valor.isBlank()) {
                        builder.header(nombre, valor);
                    } else {
                        System.out.println("Header ignorado por estar vacio: " + nombre);
                    }
                }
            }

            HttpRequest request = builder.build();

            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("Respuesta Brevo status: " + response.statusCode());
            System.out.println("Respuesta Brevo body: " + response.body());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Brevo no pudo enviar el correo: " + response.body());
            }

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Error enviando el PDF por correo con Brevo",
                    e);
        }
    }

    private String crearContenidoCorreo(Long idPago) {
        return """
                <h2>Compra confirmada</h2>
                <p>Gracias por tu compra.</p>
                <p>Adjuntamos tu entrada en formato PDF.</p>
                <p>Numero de pago: %s</p>
                """.formatted(idPago);
    }
}