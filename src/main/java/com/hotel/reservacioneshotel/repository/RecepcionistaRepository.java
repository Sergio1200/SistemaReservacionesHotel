package com.hotel.reservacioneshotel.repository;

import com.hotel.reservacioneshotel.domain.usuarios.Recepcionista;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecepcionistaRepository extends JpaRepository<Recepcionista, Long> {
}
