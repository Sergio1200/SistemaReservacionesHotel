package com.hotel.reservacioneshotel.repository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.hotel.reservacioneshotel.domain.pagos.EstadoPago;
import com.hotel.reservacioneshotel.domain.pagos.Pago;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PagoRepository extends JpaRepository<Pago, Long> {

    @Query("""
            select coalesce(sum(p.monto), 0) from Pago p
            where p.reservacion.idReservacion = :id and p.estado in :estados
            """)
    BigDecimal sumarMontos(@Param("id") Long id, @Param("estados") Collection<EstadoPago> estados);

    boolean existsByReservacion_IdReservacionAndEstadoIn(Long id, Collection<EstadoPago> estados);

    List<Pago> findByReservacion_IdReservacionOrderByFechaDescIdPagoDesc(Long id);

    Optional<Pago> findByReservacion_IdReservacionAndClaveIntento(Long id, String claveIntento);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Pago p where p.idPago = :id and p.reservacion.idReservacion = :reservacionId")
    Optional<Pago> buscarParaValidar(@Param("reservacionId") Long reservacionId, @Param("id") Long id);
}
