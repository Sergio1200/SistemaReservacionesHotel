package com.hotel.reservacioneshotel.repository;

import com.hotel.reservacioneshotel.domain.reservaciones.Reservacion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservacionRepository extends JpaRepository<Reservacion, Long> {
}
