package edu.esi.ds.esientradas.model;


import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Configuration {
    
    @Id
    private String nombre;

    private String valor;

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getValor() {
        return valor;
    }

    public void setValor(String valor) {
        this.valor = valor;
    }
}

