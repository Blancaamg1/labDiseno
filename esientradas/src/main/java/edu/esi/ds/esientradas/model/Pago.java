package edu.esi.ds.esientradas.model;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.ConstraintMode;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "pagos")
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_usuario", nullable = false)
    private Long idUsuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_espectaculo", referencedColumnName = "id", nullable = false)
    private Espectaculo espectaculo;

    @Column(name = "cantidad_entradas", nullable = false)
    private Integer cantidadEntradas;

    @Column(name = "importe_total_centimos", nullable = false)
    private Long importeTotalCentimos;

    @Column(nullable = false, length = 10)
    private String moneda;

    @Column(nullable = false, length = 30)
    private String estado;

    @Column(name = "stripe_payment_intent_id", nullable = false, length = 255, unique = true)
    private String stripePaymentIntentId;

    @Column(name = "stripe_client_secret", nullable = false, length = 255)
    private String stripeClientSecret;

    @Column(name = "email_comprador", nullable = false, length = 255)
    private String emailComprador;

    @Column(name = "fecha_pago", nullable = false)
    private LocalDateTime fechaPago;

    @JsonIgnore
    @OneToMany(mappedBy = "pago", fetch = FetchType.LAZY)
    private Set<Entrada> entradas = new HashSet<>();

    public Pago() {
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getIdUsuario() {
        return this.idUsuario;
    }

    public void setIdUsuario(Long idUsuario) {
        this.idUsuario = idUsuario;
    }

    public Espectaculo getEspectaculo() {
        return this.espectaculo;
    }

    public void setEspectaculo(Espectaculo espectaculo) {
        this.espectaculo = espectaculo;
    }

    public Integer getCantidadEntradas() {
        return this.cantidadEntradas;
    }

    public void setCantidadEntradas(Integer cantidadEntradas) {
        this.cantidadEntradas = cantidadEntradas;
    }

    public Long getImporteTotalCentimos() {
        return this.importeTotalCentimos;
    }

    public void setImporteTotalCentimos(Long importeTotalCentimos) {
        this.importeTotalCentimos = importeTotalCentimos;
    }

    public String getMoneda() {
        return this.moneda;
    }

    public void setMoneda(String moneda) {
        this.moneda = moneda;
    }

    public String getEstado() {
        return this.estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getStripePaymentIntentId() {
        return this.stripePaymentIntentId;
    }

    public void setStripePaymentIntentId(String stripePaymentIntentId) {
        this.stripePaymentIntentId = stripePaymentIntentId;
    }

    public String getStripeClientSecret() {
        return this.stripeClientSecret;
    }

    public void setStripeClientSecret(String stripeClientSecret) {
        this.stripeClientSecret = stripeClientSecret;
    }

    public String getEmailComprador() {
        return this.emailComprador;
    }

    public void setEmailComprador(String emailComprador) {
        this.emailComprador = emailComprador;
    }

    public LocalDateTime getFechaPago() {
        return this.fechaPago;
    }

    public void setFechaPago(LocalDateTime fechaPago) {
        this.fechaPago = fechaPago;
    }

    public Set<Entrada> getEntradas() {
        return this.entradas;
    }

    public void setEntradas(Set<Entrada> entradas) {
        this.entradas = entradas;
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
}