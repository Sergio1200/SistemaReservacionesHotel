package com.hotel.reservacioneshotel.domain.habitaciones;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "habitaciones")
public class Habitacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_habitacion")
    private Long idHabitacion;

    @Column(nullable = false, length = 20)
    private String numero;

    @Column(nullable = false)
    private int piso;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "estado_operativo", nullable = false, length = 16)
    private EstadoHabitacion estadoOperativo;

    @Column(name = "precio_por_noche", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioPorNoche;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_tipo", nullable = false)
    private TipoHabitacion tipoHabitacion;

    protected Habitacion() {
    }

    public Habitacion(String numero, int piso, EstadoHabitacion estadoOperativo,
                      BigDecimal precioPorNoche, TipoHabitacion tipoHabitacion) {
        this.numero = numero;
        this.piso = piso;
        this.estadoOperativo = estadoOperativo;
        this.precioPorNoche = precioPorNoche;
        this.tipoHabitacion = tipoHabitacion;
    }

    public Long getIdHabitacion() {
        return idHabitacion;
    }

    public String getNumero() {
        return numero;
    }

    public int getPiso() {
        return piso;
    }

    public EstadoHabitacion getEstadoOperativo() {
        return estadoOperativo;
    }

    public BigDecimal getPrecioPorNoche() {
        return precioPorNoche;
    }

    public TipoHabitacion getTipoHabitacion() {
        return tipoHabitacion;
    }

    public void actualizarDatos(String numero, int piso, BigDecimal precioPorNoche, TipoHabitacion tipoHabitacion) {
        this.numero = numero;
        this.piso = piso;
        this.precioPorNoche = precioPorNoche;
        this.tipoHabitacion = tipoHabitacion;
    }

    public void ocupar() {
        estadoOperativo = EstadoHabitacion.OCUPADA;
    }

    public void liberar() {
        estadoOperativo = EstadoHabitacion.DISPONIBLE;
    }
}
