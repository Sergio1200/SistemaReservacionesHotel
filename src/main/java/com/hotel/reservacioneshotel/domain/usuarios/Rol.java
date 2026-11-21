package com.hotel.reservacioneshotel.domain.usuarios;

public enum Rol {
    HUESPED("Huésped"),
    RECEPCIONISTA("Recepcionista"),
    ADMINISTRADOR("Administrador");

    private final String etiqueta;

    Rol(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
