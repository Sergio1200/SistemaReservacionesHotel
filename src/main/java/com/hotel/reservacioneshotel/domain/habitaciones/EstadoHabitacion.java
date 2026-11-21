package com.hotel.reservacioneshotel.domain.habitaciones;

public enum EstadoHabitacion {
    DISPONIBLE("Disponible"),
    OCUPADA("Ocupada");

    private final String etiqueta;

    EstadoHabitacion(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
