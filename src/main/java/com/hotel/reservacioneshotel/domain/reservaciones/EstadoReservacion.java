package com.hotel.reservacioneshotel.domain.reservaciones;

import java.util.Set;

public enum EstadoReservacion {
    PENDIENTE("Pendiente"),
    CONFIRMADA("Confirmada"),
    CANCELADA("Cancelada"),
    EN_CURSO("En curso"),
    FINALIZADA("Finalizada");

    public static final Set<EstadoReservacion> QUE_REQUIEREN_CUPO = Set.of(PENDIENTE, CONFIRMADA, EN_CURSO);

    private final String etiqueta;

    EstadoReservacion(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public boolean permiteCambios() {
        return this == PENDIENTE || this == CONFIRMADA;
    }
}
