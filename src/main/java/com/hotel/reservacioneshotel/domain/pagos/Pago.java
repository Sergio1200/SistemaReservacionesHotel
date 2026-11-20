package com.hotel.reservacioneshotel.domain.pagos;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.hotel.reservacioneshotel.domain.reservaciones.Reservacion;
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
@Table(name = "pagos")
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pago")
    private Long idPago;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_reservacion", nullable = false)
    private Reservacion reservacion;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(name = "metodo_pago", nullable = false, length = 40)
    private String metodoPago;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 16)
    private EstadoPago estado;

    protected Pago() {
    }

    public Pago(Reservacion reservacion, LocalDateTime fecha, BigDecimal monto, String metodoPago,
                EstadoPago estado) {
        this.reservacion = reservacion;
        this.fecha = fecha;
        this.monto = monto;
        this.metodoPago = metodoPago;
        this.estado = estado;
    }

    public Long getIdPago() {
        return idPago;
    }

    public Reservacion getReservacion() {
        return reservacion;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public String getMetodoPago() {
        return metodoPago;
    }

    public EstadoPago getEstado() {
        return estado;
    }
}
