package edu.esi.ds.esientradas.dto;

public class DtoConfirmarPagoResponse {

    private String paymentIntentId;
    private String estadoStripe;
    private String estadoPago;
    private Long pagoId;
    private boolean yaConfirmado;
    private boolean pdfGenerado;
    private String mensaje;

    public String getPaymentIntentId() {
        return paymentIntentId;
    }

    public void setPaymentIntentId(String paymentIntentId) {
        this.paymentIntentId = paymentIntentId;
    }

    public String getEstadoStripe() {
        return estadoStripe;
    }

    public void setEstadoStripe(String estadoStripe) {
        this.estadoStripe = estadoStripe;
    }

    public String getEstadoPago() {
        return estadoPago;
    }

    public void setEstadoPago(String estadoPago) {
        this.estadoPago = estadoPago;
    }

    public Long getPagoId() {
        return pagoId;
    }

    public void setPagoId(Long pagoId) {
        this.pagoId = pagoId;
    }

    public boolean isYaConfirmado() {
        return yaConfirmado;
    }

    public void setYaConfirmado(boolean yaConfirmado) {
        this.yaConfirmado = yaConfirmado;
    }

    public boolean isPdfGenerado() {
        return pdfGenerado;
    }

    public void setPdfGenerado(boolean pdfGenerado) {
        this.pdfGenerado = pdfGenerado;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }
}
