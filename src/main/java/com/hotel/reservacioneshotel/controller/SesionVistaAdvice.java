package com.hotel.reservacioneshotel.controller;

import java.util.Arrays;

import com.hotel.reservacioneshotel.domain.usuarios.Rol;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class SesionVistaAdvice {

    @ModelAttribute("sesion")
    public SesionVista sesion(HttpServletRequest solicitud) {
        Rol rol = Arrays.stream(Rol.values())
                .filter(valor -> solicitud.isUserInRole(valor.name()))
                .findFirst().orElse(null);
        return new SesionVista(
                solicitud.getUserPrincipal() != null,
                rol == Rol.HUESPED || rol == Rol.RECEPCIONISTA,
                rol == Rol.RECEPCIONISTA,
                rol == Rol.ADMINISTRADOR,
                rol == null ? "" : rol.getEtiqueta());
    }

    public record SesionVista(boolean autenticada, boolean reservas, boolean recepcion, boolean administracion,
                              String rol) {
    }
}
