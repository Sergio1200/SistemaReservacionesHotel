package com.hotel.reservacioneshotel.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.hotel.reservacioneshotel.domain.habitaciones.EstadoHabitacion;
import com.hotel.reservacioneshotel.domain.habitaciones.Habitacion;
import com.hotel.reservacioneshotel.domain.reservaciones.EstadoReservacion;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HabitacionRepository extends JpaRepository<Habitacion, Long> {

    @Query("""
            select h from Habitacion h join fetch h.tipoHabitacion t
            where (:numero = '' or locate(lower(:numero), lower(h.numero)) > 0)
              and (:idTipo is null or t.idTipo = :idTipo)
            order by h.numero
            """)
    List<Habitacion> buscar(@Param("numero") String numero, @Param("idTipo") Long idTipo);

    @Query("""
            select h from Habitacion h join fetch h.tipoHabitacion t
            where t.capacidad >= :huespedes
              and (:idHabitacion is null or h.idHabitacion = :idHabitacion)
              and not exists (
                  select 1 from Reservacion r where r.habitacion = h and r.estado in :estadosQueBloquean
                      and (:excluirId is null or r.idReservacion <> :excluirId)
                      and r.fechaEntrada < :salida and r.fechaSalida > :entrada
              )
              and (
                  (h.estadoOperativo = :disponible and not exists (
                      select 1 from Reservacion r where r.habitacion = h and r.estado = :enCurso
                  ))
                  or (
                      exists (
                          select 1 from Reservacion r where r.habitacion = h and r.estado = :enCurso
                              and r.fechaEntrada <= :hoy and r.fechaSalida >= :hoy and r.fechaSalida <= :entrada
                      )
                      and (select count(r) from Reservacion r where r.habitacion = h and r.estado = :enCurso) = 1
                  )
              )
            order by h.numero
            """)
    List<Habitacion> buscarDisponibles(@Param("entrada") LocalDate entrada, @Param("salida") LocalDate salida,
                                     @Param("huespedes") int huespedes, @Param("hoy") LocalDate hoy,
                                     @Param("disponible") EstadoHabitacion disponible,
                                     @Param("enCurso") EstadoReservacion enCurso,
                                     @Param("estadosQueBloquean") Collection<EstadoReservacion> estadosQueBloquean,
                                     @Param("idHabitacion") Long idHabitacion, @Param("excluirId") Long excluirId);

    @EntityGraph(attributePaths = "tipoHabitacion")
    Optional<Habitacion> findOneByIdHabitacion(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select h from Habitacion h where h.idHabitacion = :id")
    Optional<Habitacion> buscarParaActualizar(@Param("id") Long id);

    @Query("""
            select count(h) > 0 from Habitacion h
            where lower(trim(h.numero)) = lower(:numero) and (:id is null or h.idHabitacion <> :id)
            """)
    boolean existeOtroNumero(@Param("numero") String numero, @Param("id") Long id);

    boolean existsByTipoHabitacion_IdTipo(Long id);
}
