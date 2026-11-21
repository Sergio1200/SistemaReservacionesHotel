package com.hotel.reservacioneshotel.repository;

import java.util.Collection;

import com.hotel.reservacioneshotel.domain.reservaciones.EstadoReservacion;
import com.hotel.reservacioneshotel.domain.reservaciones.Reservacion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservacionRepository extends JpaRepository<Reservacion, Long> {

    boolean existsByHuesped_IdHuesped(Long id);

    boolean existsByHabitacion_IdHabitacion(Long id);

    boolean existsByHabitacion_IdHabitacionAndEstado(Long id, EstadoReservacion estado);

    boolean existsByHabitacion_IdHabitacionAndEstadoInAndNumeroHuespedesGreaterThan(
            Long id, Collection<EstadoReservacion> estados, int capacidad);

    boolean existsByHabitacion_TipoHabitacion_IdTipoAndEstadoInAndNumeroHuespedesGreaterThan(
            Long idTipo, Collection<EstadoReservacion> estados, int capacidad);
}
