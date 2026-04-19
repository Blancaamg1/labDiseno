package edu.esi.ds.esientradas.dto;

public class DtoColaEstado {

    private Long idEspectaculo;
    private Long idUsuario;
    private Integer posicion;
    private Integer personasDelante;
    private String estado;
    private Boolean puedeComprar;
    private String tokenTurno;
    private Long segundosRestantes;
    private String mensaje;

    public DtoColaEstado() {
    }

    public Long getIdEspectaculo() {
        return this.idEspectaculo;
    }

    public void setIdEspectaculo(Long idEspectaculo) {
        this.idEspectaculo = idEspectaculo;
    }

    public Long getIdUsuario() {
        return this.idUsuario;
    }

    public void setIdUsuario(Long idUsuario) {
        this.idUsuario = idUsuario;
    }

    public Integer getPosicion() {
        return this.posicion;
    }

    public void setPosicion(Integer posicion) {
        this.posicion = posicion;
    }

    public Integer getPersonasDelante() {
        return this.personasDelante;
    }

    public void setPersonasDelante(Integer personasDelante) {
        this.personasDelante = personasDelante;
    }

    public String getEstado() {
        return this.estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public Boolean getPuedeComprar() {
        return this.puedeComprar;
    }

    public void setPuedeComprar(Boolean puedeComprar) {
        this.puedeComprar = puedeComprar;
    }

    public String getTokenTurno() {
        return this.tokenTurno;
    }

    public void setTokenTurno(String tokenTurno) {
        this.tokenTurno = tokenTurno;
    }

    public Long getSegundosRestantes() {
        return this.segundosRestantes;
    }

    public void setSegundosRestantes(Long segundosRestantes) {
        this.segundosRestantes = segundosRestantes;
    }

    public String getMensaje() {
        return this.mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }
}