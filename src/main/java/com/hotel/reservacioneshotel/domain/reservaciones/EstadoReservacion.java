package com.hotel.reservacioneshotel.domain.reservaciones;

import java.util.Set;

public enum EstadoReservacion {
    PENDIENTE,
    CONFIRMADA,
    CANCELADA,
    EN_CURSO,
    FINALIZADA;

    public static final Set<EstadoReservacion> QUE_REQUIEREN_CUPO = Set.of(PENDIENTE, CONFIRMADA, EN_CURSO);
}
