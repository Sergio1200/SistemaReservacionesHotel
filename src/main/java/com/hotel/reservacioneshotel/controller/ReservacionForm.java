package com.hotel.reservacioneshotel.controller;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.format.annotation.DateTimeFormat;

public record ReservacionForm(
        @NotNull(message = "Selecciona una habitación.")
        @Positive(message = "Selecciona una habitación válida.")
        Long habitacionId,
        @NotNull(message = "Indica la fecha de entrada.")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate entrada,
        @NotNull(message = "Indica la fecha de salida.")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate salida,
        @NotNull(message = "Indica el número de huéspedes.")
        @Positive(message = "El número de huéspedes debe ser mayor que cero.")
        Integer personas,
        @Positive(message = "Selecciona un huésped válido.")
        Long huespedId) {
}
