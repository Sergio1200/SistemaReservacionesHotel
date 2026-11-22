package com.hotel.reservacioneshotel.repository;

import java.math.BigDecimal;
import java.util.Collection;

import com.hotel.reservacioneshotel.domain.pagos.EstadoPago;
import com.hotel.reservacioneshotel.domain.pagos.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PagoRepository extends JpaRepository<Pago, Long> {

    @Query("""
            select coalesce(sum(p.monto), 0) from Pago p
            where p.reservacion.idReservacion = :id and p.estado = :estado
            """)
    BigDecimal sumarMontos(@Param("id") Long id, @Param("estado") EstadoPago estado);

    boolean existsByReservacion_IdReservacionAndEstadoIn(Long id, Collection<EstadoPago> estados);
}
