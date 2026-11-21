package com.hotel.reservacioneshotel.service;

import java.util.Locale;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;

final class RestriccionesPersistencia {

    private RestriccionesPersistencia() {
    }

    static boolean correoDuplicado(DataIntegrityViolationException exception) {
        return coincide(exception, "uk_huespedes_correo", "uk_cuentas_correo");
    }

    static boolean coincide(DataIntegrityViolationException exception, String... restricciones) {
        for (Throwable causa = exception.getCause(); causa != null; causa = causa.getCause()) {
            if (causa instanceof ConstraintViolationException violacion && violacion.getConstraintName() != null) {
                String nombre = violacion.getConstraintName().toLowerCase(Locale.ROOT);
                for (String restriccion : restricciones) {
                    if (nombre.contains(restriccion)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
