package com.hotel.reservacioneshotel.controller;

import java.math.BigDecimal;

import com.hotel.reservacioneshotel.domain.habitaciones.EstadoHabitacion;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record HabitacionForm(
        @NotBlank(message = "Escribe el número de habitación.")
        @Size(max = 20, message = "El número admite hasta 20 caracteres.")
        String numero,
        @NotNull(message = "Indica el piso.")
        @PositiveOrZero(message = "El piso debe ser cero o un número entero positivo.")
        Integer piso,
        @NotNull(message = "Selecciona un tipo de habitación.")
        @Positive(message = "Selecciona un tipo de habitación válido.")
        Long idTipo,
        @NotNull(message = "Indica el precio por noche.")
        @Positive(message = "El precio por noche debe ser mayor que cero.")
        @Digits(integer = 10, fraction = 2, message = "Usa hasta 10 cifras enteras y 2 decimales.")
        BigDecimal precio,
        EstadoHabitacion estado) {

    public HabitacionForm {
        numero = numero == null ? "" : numero.strip();
    }
}
