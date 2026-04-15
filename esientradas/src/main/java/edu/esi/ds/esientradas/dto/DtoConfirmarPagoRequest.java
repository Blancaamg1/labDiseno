package edu.esi.ds.esientradas.dto;

import java.util.List;

public class DtoConfirmarPagoRequest {

    private String paymentIntentId;
    private String clientSecret;
    private String userToken;
    private Long idUsuario;
    private Long idEspectaculo;
    private Integer cantidadEntradas;
    private String emailComprador;
    private List<Long> idsEntradas;

    public String getUserToken() {
        return userToken;
    }

    public void setUserToken(String userToken) {
        this.userToken = userToken;
    }

    public String getPaymentIntentId() {
        return paymentIntentId;
    }

    public void setPaymentIntentId(String paymentIntentId) {
        this.paymentIntentId = paymentIntentId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public Long getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Long idUsuario) {
        this.idUsuario = idUsuario;
    }

    public Long getIdEspectaculo() {
        return idEspectaculo;
    }

    public void setIdEspectaculo(Long idEspectaculo) {
        this.idEspectaculo = idEspectaculo;
    }

    public Integer getCantidadEntradas() {
        return cantidadEntradas;
    }

    public void setCantidadEntradas(Integer cantidadEntradas) {
        this.cantidadEntradas = cantidadEntradas;
    }

    public String getEmailComprador() {
        return emailComprador;
    }

    public void setEmailComprador(String emailComprador) {
        this.emailComprador = emailComprador;
    }

    public List<Long> getIdsEntradas() {
        return idsEntradas;
    }

    public void setIdsEntradas(List<Long> idsEntradas) {
        this.idsEntradas = idsEntradas;
    }
}