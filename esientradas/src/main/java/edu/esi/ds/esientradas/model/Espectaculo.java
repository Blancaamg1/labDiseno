package edu.esi.ds.esientradas.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

@Entity
public class Espectaculo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String artista;
    private LocalDateTime fecha;

    private Boolean usaColaVirtual;
    private LocalDateTime fechaAperturaCola;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "escenario_id", nullable = false)
    private Escenario escenario;

    @OneToMany(mappedBy = "espectaculo", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Entrada> entradas = new ArrayList<>();

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getArtista() {
        return this.artista;
    }

    public void setArtista(String artista) {
        this.artista = artista;
    }

    public LocalDateTime getFecha() {
        return this.fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public Boolean getUsaColaVirtual() {
        return this.usaColaVirtual;
    }

    public void setUsaColaVirtual(Boolean usaColaVirtual) {
        this.usaColaVirtual = usaColaVirtual;
    }

    public LocalDateTime getFechaAperturaCola() {
        return this.fechaAperturaCola;
    }

    public void setFechaAperturaCola(LocalDateTime fechaAperturaCola) {
        this.fechaAperturaCola = fechaAperturaCola;
    }

    @JsonIgnore
    public Escenario getEscenario() {
        return this.escenario;
    }

    public void setEscenario(Escenario escenario) {
        this.escenario = escenario;
    }

    @JsonIgnore
    public List<Entrada> getEntradas() {
        return this.entradas;
    }

    public void setEntradas(List<Entrada> entradas) {
        this.entradas = entradas;
    }
}