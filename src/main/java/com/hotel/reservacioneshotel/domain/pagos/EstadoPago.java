package com.hotel.reservacioneshotel.domain.pagos;

import java.util.List;

public enum EstadoPago {
    PENDIENTE("Pendiente"),
    PROCESANDO("Procesando"),
    PAGADO("Pagado"),
    RECHAZADO("Rechazado");

    public static final List<EstadoPago> EN_TRAMITE = List.of(PENDIENTE, PROCESANDO);

    private final String etiqueta;

    EstadoPago(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
