package com.hotel.reservacioneshotel.repository;

import java.util.Optional;

import com.hotel.reservacioneshotel.domain.habitaciones.TipoHabitacion;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TipoHabitacionRepository extends JpaRepository<TipoHabitacion, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from TipoHabitacion t where t.idTipo = :id")
    Optional<TipoHabitacion> buscarParaActualizar(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select t from TipoHabitacion t where t.idTipo = :id")
    Optional<TipoHabitacion> buscarParaReservar(@Param("id") Long id);

    @Query("""
            select count(t) > 0 from TipoHabitacion t
            where lower(trim(t.nombre)) = lower(:nombre) and (:id is null or t.idTipo <> :id)
            """)
    boolean existeOtroNombre(@Param("nombre") String nombre, @Param("id") Long id);
}
