package edu.esi.ds.esientradas.dto;

public class DtoReserva {
    private String tokenReserva;
    private Long precioTotal;

    public DtoReserva() {}

    public DtoReserva(String tokenReserva, Long precioTotal) {
        this.tokenReserva = tokenReserva;
        this.precioTotal = precioTotal;
    }

    public String getTokenReserva() {
        return tokenReserva;
    }

    public void setTokenReserva(String tokenReserva) {
        this.tokenReserva = tokenReserva;
    }

    public Long getPrecioTotal() {
        return precioTotal;
    }

    public void setPrecioTotal(Long precioTotal) {
        this.precioTotal = precioTotal;
    }
}
