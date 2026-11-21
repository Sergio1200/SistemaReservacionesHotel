package com.hotel.reservacioneshotel.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record TipoHabitacionForm(
        @NotBlank(message = "Escribe el nombre del tipo de habitación.")
        @Size(max = 80, message = "El nombre admite hasta 80 caracteres.")
        String nombre,
        @NotNull(message = "Indica la capacidad.")
        @Positive(message = "La capacidad debe ser mayor que cero.")
        Integer capacidad,
        @Size(max = 255, message = "La descripción admite hasta 255 caracteres.")
        String descripcion) {

    public TipoHabitacionForm {
        nombre = nombre == null ? "" : nombre.strip();
        descripcion = descripcion == null ? "" : descripcion.strip();
    }
}
