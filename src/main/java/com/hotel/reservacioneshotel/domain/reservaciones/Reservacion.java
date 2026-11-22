package com.hotel.reservacioneshotel.domain.reservaciones;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.hotel.reservacioneshotel.domain.habitaciones.Habitacion;
import com.hotel.reservacioneshotel.domain.usuarios.Huesped;
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
import jakarta.validation.constraints.AssertTrue;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "reservaciones")
public class Reservacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_reservacion")
    private Long idReservacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_huesped", nullable = false)
    private Huesped huesped;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_habitacion", nullable = false)
    private Habitacion habitacion;

    @Column(name = "fecha_entrada", nullable = false)
    private LocalDate fechaEntrada;

    @Column(name = "fecha_salida", nullable = false)
    private LocalDate fechaSalida;

    @Column(name = "numero_huespedes", nullable = false)
    private int numeroHuespedes;

    @Column(name = "precio_noche_aplicado", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioNocheAplicado;

    @Column(name = "costo_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal costoTotal;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 16)
    private EstadoReservacion estado;

    protected Reservacion() {
    }

    public Reservacion(Huesped huesped, Habitacion habitacion, LocalDate fechaEntrada, LocalDate fechaSalida,
                       int numeroHuespedes, BigDecimal precioNocheAplicado, BigDecimal costoTotal,
                       EstadoReservacion estado) {
        this.huesped = huesped;
        this.habitacion = habitacion;
        this.fechaEntrada = fechaEntrada;
        this.fechaSalida = fechaSalida;
        this.numeroHuespedes = numeroHuespedes;
        this.precioNocheAplicado = precioNocheAplicado;
        this.costoTotal = costoTotal;
        this.estado = estado;
    }

    @AssertTrue(message = "La fecha de salida debe ser posterior a la fecha de entrada.")
    public boolean isRangoDeFechasValido() {
        return fechaEntrada != null && fechaSalida != null && fechaSalida.isAfter(fechaEntrada);
    }

    public Long getIdReservacion() {
        return idReservacion;
    }

    public Huesped getHuesped() {
        return huesped;
    }

    public Habitacion getHabitacion() {
        return habitacion;
    }

    public LocalDate getFechaEntrada() {
        return fechaEntrada;
    }

    public LocalDate getFechaSalida() {
        return fechaSalida;
    }

    public int getNumeroHuespedes() {
        return numeroHuespedes;
    }

    public BigDecimal getPrecioNocheAplicado() {
        return precioNocheAplicado;
    }

    public BigDecimal getCostoTotal() {
        return costoTotal;
    }

    public EstadoReservacion getEstado() {
        return estado;
    }

    public void reprogramar(Habitacion habitacion, LocalDate entrada, LocalDate salida, int personas,
                             BigDecimal tarifa, BigDecimal total) {
        this.habitacion = habitacion;
        this.fechaEntrada = entrada;
        this.fechaSalida = salida;
        this.numeroHuespedes = personas;
        this.precioNocheAplicado = tarifa;
        this.costoTotal = total;
    }

    public void cancelar() {
        estado = EstadoReservacion.CANCELADA;
    }

    public void confirmarPagoCompleto() {
        if (estado == EstadoReservacion.PENDIENTE) {
            estado = EstadoReservacion.CONFIRMADA;
        }
    }
}
