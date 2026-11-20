package com.hotel.reservacioneshotel.repository;

import com.hotel.reservacioneshotel.domain.pagos.Pago;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PagoRepository extends JpaRepository<Pago, Long> {
}
