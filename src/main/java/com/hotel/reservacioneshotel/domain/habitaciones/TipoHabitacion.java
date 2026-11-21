package com.hotel.reservacioneshotel.domain.habitaciones;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tipos_habitacion")
public class TipoHabitacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tipo")
    private Long idTipo;

    @Column(nullable = false, length = 80)
    private String nombre;

    @Column(nullable = false)
    private int capacidad;

    @Column(length = 255)
    private String descripcion;

    protected TipoHabitacion() {
    }

    public TipoHabitacion(String nombre, int capacidad, String descripcion) {
        this.nombre = nombre;
        this.capacidad = capacidad;
        this.descripcion = descripcion;
    }

    public Long getIdTipo() {
        return idTipo;
    }

    public String getNombre() {
        return nombre;
    }

    public int getCapacidad() {
        return capacidad;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void actualizarDatos(String nombre, int capacidad, String descripcion) {
        this.nombre = nombre;
        this.capacidad = capacidad;
        this.descripcion = descripcion;
    }
}
