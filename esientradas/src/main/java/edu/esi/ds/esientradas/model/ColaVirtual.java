package edu.esi.ds.esientradas.model;

import java.time.LocalDateTime;

import jakarta.persistence.ConstraintMode;
import jakarta.persistence.FetchType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class ColaVirtual {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(
            name = "id_espectaculo",
            referencedColumnName = "id",
            nullable = false,
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Espectaculo espectaculo;

    private Long idUsuario;

    private Integer posicion;
    private Integer personasDelante;

    private String estado;
    private String tokenTurno;

    private LocalDateTime fechaEntrada;
    private LocalDateTime fechaFinTurno;

    public ColaVirtual() {
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getIdEspectaculo() {
        return this.espectaculo == null ? null : this.espectaculo.getId();
    }

    public void setIdEspectaculo(Long idEspectaculo) {
        if (idEspectaculo == null) {
            this.espectaculo = null;
            return;
        }

        Espectaculo espectaculo = new Espectaculo();
        espectaculo.setId(idEspectaculo);
        this.espectaculo = espectaculo;
    }

    public Espectaculo getEspectaculo() {
        return this.espectaculo;
    }

    public void setEspectaculo(Espectaculo espectaculo) {
        this.espectaculo = espectaculo;
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

    public String getTokenTurno() {
        return this.tokenTurno;
    }

    public void setTokenTurno(String tokenTurno) {
        this.tokenTurno = tokenTurno;
    }

    public LocalDateTime getFechaEntrada() {
        return this.fechaEntrada;
    }

    public void setFechaEntrada(LocalDateTime fechaEntrada) {
        this.fechaEntrada = fechaEntrada;
    }

    public LocalDateTime getFechaFinTurno() {
        return this.fechaFinTurno;
    }

    public void setFechaFinTurno(LocalDateTime fechaFinTurno) {
        this.fechaFinTurno = fechaFinTurno;
    }
}