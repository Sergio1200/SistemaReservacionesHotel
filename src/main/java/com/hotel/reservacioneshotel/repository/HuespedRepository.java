package com.hotel.reservacioneshotel.repository;

import com.hotel.reservacioneshotel.domain.usuarios.Huesped;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HuespedRepository extends JpaRepository<Huesped, Long> {
}
