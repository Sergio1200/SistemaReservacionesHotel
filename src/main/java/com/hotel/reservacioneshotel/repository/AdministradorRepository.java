package com.hotel.reservacioneshotel.repository;

import com.hotel.reservacioneshotel.domain.usuarios.Administrador;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdministradorRepository extends JpaRepository<Administrador, Long> {
}
