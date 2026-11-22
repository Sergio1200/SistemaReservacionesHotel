package com.hotel.reservacioneshotel.controller;

import java.math.BigDecimal;
import java.util.UUID;

import com.hotel.reservacioneshotel.domain.pagos.MetodoPago;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PagoForm(
        @NotNull(message = "Indica un monto.")
        @Positive(message = "El monto debe ser mayor que cero.")
        @Digits(integer = 10, fraction = 2, message = "Usa hasta diez cifras enteras y dos decimales.")
        BigDecimal monto,
        @NotNull(message = "Selecciona un método simulado.")
        MetodoPago metodo,
        @NotNull(message = "El intento no es válido. Vuelve a abrir el formulario.")
        UUID intento) {
}
