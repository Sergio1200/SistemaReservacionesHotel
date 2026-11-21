package com.hotel.reservacioneshotel.repository;

import com.hotel.reservacioneshotel.domain.usuarios.Huesped;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HuespedRepository extends JpaRepository<Huesped, Long> {

    @Query("select count(h) > 0 from Huesped h where lower(trim(h.correo)) = :correo")
    boolean existeCorreoNormalizado(@Param("correo") String correo);
}
