package com.hotel.reservacioneshotel.repository;

import com.hotel.reservacioneshotel.domain.usuarios.Empleado;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmpleadoRepository extends JpaRepository<Empleado, Long> {
}
