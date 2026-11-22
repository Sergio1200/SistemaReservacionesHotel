package com.hotel.reservacioneshotel.domain.pagos;

public enum MetodoPago {
    EFECTIVO("Efectivo simulado"),
    TARJETA_SIMULADA("Tarjeta simulada"),
    TRANSFERENCIA_SIMULADA("Transferencia simulada");

    private final String etiqueta;

    MetodoPago(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
