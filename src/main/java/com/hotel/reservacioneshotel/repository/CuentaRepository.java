package com.hotel.reservacioneshotel.repository;

import java.util.Optional;

import com.hotel.reservacioneshotel.domain.usuarios.Cuenta;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CuentaRepository extends JpaRepository<Cuenta, Long> {

    Optional<Cuenta> findByCorreo(String correo);

    boolean existsByCorreo(String correo);
}
