package edu.esi.ds.esientradas.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;

import edu.esi.ds.esientradas.dao.ConfigurationDao;
import edu.esi.ds.esientradas.dao.EntradaDao;
import edu.esi.ds.esientradas.dao.PagoDao;
import edu.esi.ds.esientradas.dto.DtoConfirmarPagoRequest;
import edu.esi.ds.esientradas.dto.DtoConfirmarPagoResponse;
import edu.esi.ds.esientradas.dto.DtoUsuarioInfo;
import edu.esi.ds.esientradas.model.Configuration;
import edu.esi.ds.esientradas.model.Entrada;
import edu.esi.ds.esientradas.model.Estado;
import edu.esi.ds.esientradas.model.Pago;

@Service
public class PagosService {

    @Autowired
    private UsuarioService usuarioService;

    private static final String FALLBACK_STRIPE_SECRET_KEY = "";

    @Autowired
    private PagoDao pagoDao;

    @Autowired
    private EntradaDao entradaDao;

    @Autowired
    private PDFService pdfService;

    @Autowired
    private ConfigurationDao configurationDao;

    public String prepararPago(Long centimos) throws StripeException {
        Stripe.apiKey = this.getStripeSecretKey();

        PaymentIntentCreateParams params = new PaymentIntentCreateParams.Builder()
            .setCurrency("eur")
            .setAmount(centimos)
            .build();

        PaymentIntent intent = PaymentIntent.create(params);
        JSONObject json = new JSONObject(intent.toJson());

        String clientSecret = json.getString("client_secret");
        System.out.println("Client secret = " + clientSecret);
        return clientSecret;
    }

    @Transactional
    public DtoConfirmarPagoResponse confirmarPago(DtoConfirmarPagoRequest request) {
        if (request == null || request.getPaymentIntentId() == null || request.getPaymentIntentId().isBlank()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Se requiere paymentIntentId para confirmar el pago"
            );
        }

        this.validarEntradasRecibidas(request);

        try {
            Stripe.apiKey = this.getStripeSecretKey();
            PaymentIntent intent = PaymentIntent.retrieve(request.getPaymentIntentId());

            String stripeStatus = intent.getStatus();
            if (!"succeeded".equalsIgnoreCase(stripeStatus)) {
                throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El pago no esta completado en Stripe. Estado actual: " + stripeStatus
                );
            }

            Optional<Pago> existingPago = this.pagoDao.findByStripePaymentIntentId(intent.getId());
            Pago pago = existingPago.orElseGet(Pago::new);
            boolean yaConfirmado = existingPago.isPresent()
                && "PAGADO".equalsIgnoreCase(existingPago.get().getEstado());

            if (!yaConfirmado) {
                pago.setStripePaymentIntentId(intent.getId());

                String clientSecret = intent.getClientSecret();
                if (clientSecret == null || clientSecret.isBlank()) {
                    clientSecret = request.getClientSecret();
                }
                if (clientSecret == null || clientSecret.isBlank()) {
                    clientSecret = "NO_CLIENT_SECRET";
                }
                pago.setStripeClientSecret(clientSecret);

                Long amount = intent.getAmountReceived();
                if (amount == null || amount <= 0) {
                    amount = intent.getAmount();
                }
                pago.setImporteTotalCentimos(amount);

                String currency = intent.getCurrency();
                if (currency == null || currency.isBlank()) {
                    currency = "eur";
                }
                pago.setMoneda(currency.toUpperCase(Locale.ROOT));

                pago.setEstado("PAGADO");
                pago.setFechaPago(LocalDateTime.now());

                if (request.getUserToken() == null || request.getUserToken().isBlank()) {
                    throw new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Se requiere token de usuario para confirmar el pago"
                    );
                }

                DtoUsuarioInfo authenticatedUser = this.usuarioService.getUserInfo(request.getUserToken());
                if (authenticatedUser == null || authenticatedUser.getId() == null) {
                    throw new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "No se pudo identificar el comprador. Token invalido."
                    );
                }

                pago.setIdUsuario(authenticatedUser.getId());

                if (authenticatedUser.getEmail() != null && !authenticatedUser.getEmail().isBlank()) {
                    pago.setEmailComprador(authenticatedUser.getEmail().trim().toLowerCase(Locale.ROOT));
                } else if (request.getEmailComprador() != null && !request.getEmailComprador().isBlank()) {
                    pago.setEmailComprador(request.getEmailComprador().trim().toLowerCase(Locale.ROOT));
                } else {
                    pago.setEmailComprador("sin_email@local");
                }

                pago.setIdEspectaculo(request.getIdEspectaculo() != null ? request.getIdEspectaculo() : 0L);
                pago.setCantidadEntradas(request.getIdsEntradas().size());

                this.reservarEntradasSeleccionadas(request.getIdEspectaculo(), request.getIdsEntradas());
                pago = this.pagoDao.save(pago);
            }

            boolean pdfGenerado = false;
            try {
                this.pdfService.generarEntradaPDF(pago);
                pdfGenerado = true;
            } catch (Exception ex) {
                pdfGenerado = false;
            }

            DtoConfirmarPagoResponse response = new DtoConfirmarPagoResponse();
            response.setPagoId(pago.getId());
            response.setPaymentIntentId(intent.getId());
            response.setEstadoStripe(stripeStatus);
            response.setEstadoPago(pago.getEstado());
            response.setYaConfirmado(yaConfirmado);
            response.setPdfGenerado(pdfGenerado);
            response.setMensaje(yaConfirmado
                ? "Pago ya confirmado previamente"
                : "Pago confirmado correctamente");

            return response;

        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (StripeException ex) {
            throw new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "No se pudo validar el pago en Stripe",
                ex
            );
        }
    }

    private void validarEntradasRecibidas(DtoConfirmarPagoRequest request) {
        if (request.getIdsEntradas() == null || request.getIdsEntradas().isEmpty()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "No se han recibido entradas para confirmar el pago"
            );
        }

        if (request.getCantidadEntradas() != null
                && request.getCantidadEntradas().intValue() != request.getIdsEntradas().size()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La cantidad de entradas no coincide con la seleccion"
            );
        }
    }

    private void reservarEntradasSeleccionadas(Long idEspectaculo, List<Long> idsEntradas) {
        List<Entrada> entradas = this.entradaDao.findAllById(idsEntradas);

        if (entradas.size() != idsEntradas.size()) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Alguna de las entradas seleccionadas no existe"
            );
        }

        for (Entrada entrada : entradas) {
            if (entrada.getEstado() != Estado.DISPONIBLE) {
                throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Alguna de las entradas seleccionadas ya no esta disponible"
                );
            }

            if (idEspectaculo != null
                    && entrada.getEspectaculo() != null
                    && entrada.getEspectaculo().getId() != null
                    && !entrada.getEspectaculo().getId().equals(idEspectaculo)) {
                throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Alguna entrada no pertenece al espectaculo indicado"
                );
            }
        }

        for (Entrada entrada : entradas) {
            entrada.setEstado(Estado.VENDIDA);
        }

        this.entradaDao.saveAll(entradas);
    }

    private String getStripeSecretKey() {
        Optional<Configuration> config = this.configurationDao.findById("stripe.secret.key");
        if (config.isPresent()) {
            String value = config.get().getValor();
            if (value != null && !value.isBlank()) {
                return value;
            }
        }

        String envValue = System.getenv("STRIPE_SECRET_KEY");
        if (envValue != null && !envValue.isBlank()) {
            return envValue;
        }

        return FALLBACK_STRIPE_SECRET_KEY;
    }
}