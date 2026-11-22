package com.hotel.reservacioneshotel.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.hotel.reservacioneshotel.domain.reservaciones.EstadoReservacion;
import com.hotel.reservacioneshotel.domain.reservaciones.Reservacion;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservacionRepository extends JpaRepository<Reservacion, Long> {

    @EntityGraph(attributePaths = {"huesped", "habitacion", "habitacion.tipoHabitacion"})
    Optional<Reservacion> findOneByIdReservacion(Long id);

    @EntityGraph(attributePaths = {"huesped", "habitacion", "habitacion.tipoHabitacion"})
    List<Reservacion> findByHuesped_IdHuespedOrderByFechaEntradaDescIdReservacionDesc(Long idHuesped);

    @Query("""
            select r from Reservacion r
            join fetch r.habitacion h join fetch h.tipoHabitacion join fetch r.huesped u
            where r.estado in :estados and (:q = '' or r.idReservacion = :id
                or locate(lower(:q), lower(u.nombre)) > 0
                or locate(lower(:q), lower(u.correo)) > 0)
            order by r.fechaEntrada desc, r.idReservacion desc
            """)
    List<Reservacion> buscarParaRecepcion(@Param("q") String q, @Param("id") Long id,
                                         @Param("estados") Collection<EstadoReservacion> estados);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Reservacion r where r.idReservacion = :id")
    Optional<Reservacion> buscarParaActualizar(@Param("id") Long id);

    boolean existsByHuesped_IdHuesped(Long id);

    boolean existsByHabitacion_IdHabitacion(Long id);

    boolean existsByHabitacion_IdHabitacionAndEstado(Long id, EstadoReservacion estado);

    boolean existsByHabitacion_IdHabitacionAndEstadoAndIdReservacionNot(Long habitacionId, EstadoReservacion estado,
                                                                       Long reservacionId);

    boolean existsByHabitacion_IdHabitacionAndEstadoInAndNumeroHuespedesGreaterThan(
            Long id, Collection<EstadoReservacion> estados, int capacidad);

    boolean existsByHabitacion_TipoHabitacion_IdTipoAndEstadoInAndNumeroHuespedesGreaterThan(
            Long idTipo, Collection<EstadoReservacion> estados, int capacidad);
}
