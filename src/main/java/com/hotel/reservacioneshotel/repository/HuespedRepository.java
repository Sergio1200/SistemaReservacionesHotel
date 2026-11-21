package com.hotel.reservacioneshotel.repository;

import java.util.List;
import java.util.Optional;

import com.hotel.reservacioneshotel.domain.usuarios.Huesped;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HuespedRepository extends JpaRepository<Huesped, Long> {

    @Query("select count(h) > 0 from Huesped h where lower(trim(h.correo)) = :correo")
    boolean existeCorreoNormalizado(@Param("correo") String correo);

    @Query("select count(h) > 0 from Huesped h where lower(trim(h.correo)) = :correo and h.idHuesped <> :id")
    boolean existeCorreoDeOtroHuesped(@Param("correo") String correo, @Param("id") Long id);

    @Query("""
            select h from Huesped h
            where locate(lower(:texto), lower(h.nombre)) > 0
               or locate(lower(:texto), lower(h.correo)) > 0
               or cast(h.idHuesped as String) = :texto
            order by h.nombre, h.idHuesped
            """)
    List<Huesped> buscar(@Param("texto") String texto);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select h from Huesped h where h.idHuesped = :id")
    Optional<Huesped> buscarParaActualizar(@Param("id") Long id);
}
