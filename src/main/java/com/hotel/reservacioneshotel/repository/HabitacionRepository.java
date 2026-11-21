package com.hotel.reservacioneshotel.repository;

import java.util.List;
import java.util.Optional;

import com.hotel.reservacioneshotel.domain.habitaciones.Habitacion;
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
