package com.hotel.reservacioneshotel.controller;

import com.hotel.reservacioneshotel.domain.usuarios.Cuenta;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistroHuespedForm(
        @NotBlank(message = "Escribe tu nombre completo.")
        @Size(max = 120, message = "El nombre debe tener como máximo 120 caracteres.")
        String nombre,

        @NotBlank(message = "Escribe tu correo electrónico.")
        @Email(message = "Escribe un correo electrónico válido.")
        @Size(max = 254, message = "El correo debe tener como máximo 254 caracteres.")
        String correo,

        @NotBlank(message = "Escribe tu teléfono.")
        @Size(max = 32, message = "El teléfono debe tener como máximo 32 caracteres.")
        String telefono) {

    public RegistroHuespedForm {
        nombre = nombre == null ? "" : nombre.strip();
        correo = correo == null ? "" : Cuenta.normalizarCorreo(correo);
        telefono = telefono == null ? "" : telefono.strip();
    }
}
