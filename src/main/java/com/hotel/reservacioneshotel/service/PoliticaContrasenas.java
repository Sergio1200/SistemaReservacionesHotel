package com.hotel.reservacioneshotel.service;

import java.nio.charset.StandardCharsets;

public final class PoliticaContrasenas {

    public static final String MENSAJE = "La contraseña debe tener al menos 8 caracteres y ocupar como máximo 72 bytes.";

    private PoliticaContrasenas() {
    }

    public static boolean valida(String contrasena) {
        return contrasena != null && !contrasena.isBlank()
                && contrasena.codePointCount(0, contrasena.length()) >= 8
                && dentroDelLimite(contrasena);
    }

    public static boolean dentroDelLimite(CharSequence contrasena) {
        return contrasena != null && contrasena.length() <= 72
                && contrasena.toString().getBytes(StandardCharsets.UTF_8).length <= 72;
    }
}
