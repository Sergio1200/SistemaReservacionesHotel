package com.hotel.reservacioneshotel.repository;

import com.hotel.reservacioneshotel.domain.habitaciones.TipoHabitacion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TipoHabitacionRepository extends JpaRepository<TipoHabitacion, Long> {
}
