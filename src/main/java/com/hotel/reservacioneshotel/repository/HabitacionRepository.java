package com.hotel.reservacioneshotel.repository;

import com.hotel.reservacioneshotel.domain.habitaciones.Habitacion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HabitacionRepository extends JpaRepository<Habitacion, Long> {
}
