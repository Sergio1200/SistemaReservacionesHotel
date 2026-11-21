package com.hotel.reservacioneshotel.repository;

import java.util.Optional;

import com.hotel.reservacioneshotel.domain.usuarios.Cuenta;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CuentaRepository extends JpaRepository<Cuenta, Long> {

    Optional<Cuenta> findByCorreo(String correo);

    boolean existsByCorreo(String correo);

    Optional<Cuenta> findByHuesped_IdHuesped(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Cuenta c where c.huesped.idHuesped = :id")
    Optional<Cuenta> buscarDeHuespedParaActualizar(@Param("id") Long id);
}
